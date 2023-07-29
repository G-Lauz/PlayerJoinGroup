package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.bungee.query.QuerySpigotServer;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessagesManager {

    private final PlayerJoinGroup plugin;
    private ServerSocket serverSocket;
    private Hashtable<String, ConnectionToClient> clients;
    private Queue<Message> messages;
    private AtomicBoolean isRunning;
    private Thread acceptThread;
    private Thread consumerThread;

    private HashMap<Integer, QuerySpigotServer<Boolean>> subscribers;

    public MessagesManager(PlayerJoinGroup plugin, int port) throws IOException {
        this.plugin = plugin;

        this.messages = new LinkedList<>();
        this.clients = new Hashtable<>();
        this.serverSocket = new ServerSocket(port);
        this.subscribers = new HashMap<>();

        this.acceptThread = null;
        this.consumerThread = null;

        this.isRunning = new AtomicBoolean(false);
    }

    public void initialize() {
        this.isRunning.set(true);
        this.accept();
        this.startConsumerThread();
    }

    private void accept() {
        this.acceptThread = new Thread("playerjoingroup.bungee.messagemanager.accept") {
            @Override
            public void run() {
                while (MessagesManager.this.isRunning.get()) {
                    try {
                        Socket socket = serverSocket.accept();
                        new ConnectionToClient(socket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    private void startConsumerThread() {
        this.consumerThread = new Thread("playerjoingroup.bungee.messagemanager.consumer") {
            @Override
            public void run() {
                MessagesManager.this.consumeMessage();
            }
        };

        this.consumerThread.setDaemon(true);
        this.consumerThread.start();
    }

    private void consumeMessage() {
        while (!Thread.currentThread().isInterrupted() && this.isRunning.get()) {
            synchronized (this.messages) {
                if (this.messages.isEmpty())
                    this.waitForMessage();

                Message msg = this.messages.remove();
                this.messages.notifyAll();

                this.processMessage(msg);
            }
        }
    }

    private void waitForMessage() {
        try {
            this.messages.wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processMessage(Message message) {
        try {
            Packet packet = Protocol.deconstructPacket(message.getMessage());
            ConnectionToClient client = message.getClient();
            String subchannel = packet.getSubchannel();

            this.handleMessageBySubchannel(subchannel, packet, client);



        } catch (DeconstructPacketErrorException | IOException | UnknownSubchannelException err) {
            this.plugin.getLogger().severe(Arrays.toString(err.getStackTrace()));
        }
    }

    private void handleMessageBySubchannel(String subchannel, Packet packet, ConnectionToClient client)
            throws IOException, UnknownSubchannelException {
        Subchannel subchannelType = Subchannel.typeof(subchannel);

        switch (subchannelType) {
            case BROADCAST -> this.sendToAll(packet);
            case EVENT -> this.handleEventSubchannel(packet);
            case QUERY -> this.handleQuerySubchannel(packet);
            case HANDSHAKE -> this.handleHandshakeSubchannel(packet, client);
            default -> throw new UnknownSubchannelException(subchannel);
        }
    }

    private void handleEventSubchannel(Packet packet) {
        String eventType = packet.getField(ParamsKey.EVENT);
        switch (EventType.typeof(eventType)) {
            case FIRST_SPIGOT_CONNECTION -> onFirstConnection(packet.getData()); // TODO we don't receive it anymore
            default -> MessagesManager.this.plugin.getLogger().warning("Unknown event: " + eventType);
        }
    }

    private void handleQuerySubchannel(Packet packet) {
        String queryType = packet.getField(ParamsKey.QUERY);
        switch (QueryType.typeof(queryType)) {
            case HAS_PLAYED_BEFORE_RESPONSE -> {
                int hashCode = Integer.parseInt(packet.getField(ParamsKey.HASH_CODE));
                boolean data = Boolean.parseBoolean(packet.getData());
                notifySubscriber(hashCode, data);
                unsubscribe(hashCode);
            }
            default -> MessagesManager.this.plugin.getLogger().warning("Unknown query: "+ queryType);
        }
    }

    private void handleHandshakeSubchannel(Packet packet, ConnectionToClient client) {
        // TODO proper handshake (with Query?)
        clients.put(packet.getData(), client);

        Packet ack = new Packet.Builder("HANDSHAKE")
                .setData("ACK")
                .build();
        sendToOne(packet.getData(), ack);
    }

    public void sendToOne(String server, Packet packet) {

        ServerInfo serverInfo = this.plugin.getProxy().getServerInfo(server);
        if (serverInfo == null) {
            plugin.getLogger().warning("Unable to find server \"" + server + "\", ignoring it.");
            return;
        }

        if (!clients.containsKey(server)) {
            plugin.getLogger().warning(server + "\" did not handshake, ignoring it.");
            return;
        }

        try {
            clients.get(server).write(Protocol.constructPacket(packet));
        } catch (IOException e) {
            if (e.getMessage().contains("Broken pipe")) {
                this.plugin.getLogger().warning("Broken pipe: Unable to send packet to \"" + server + "\", removing it from the list.");
                ConnectionToClient connection = clients.remove(server);
                try {
                    connection.close();
                } catch (IOException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } catch (InvalidPacketException | ConstructPacketErrorException e) { // TODO better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void sendToAll(Packet packet) {
        String group = packet.getField(ParamsKey.SERVER_GROUP);

        this.plugin.getConfig().getGroup().forEach((serverGroup, servers) -> {
            if (serverGroup.equals(group) || group.equals("ALL")) {
                ((ArrayList) servers).forEach(server -> {
                    sendToOne((String)server, packet);
                });
            }
        });
    }

    public void sendQueryHasPlayedBefore(String serverName, ProxiedPlayer player) { // TODO refactor / extract methods / make  Query a core component
        UUID playerUUID =  player.getUniqueId();
        Config config = this.plugin.getConfig();
        String group = Utils.getServerGroupName(serverName, config);

        QuerySpigotServer<Boolean> query = new QuerySpigotServer<>(serverName, MessagesManager.this);
        int queryHashCode = query.hashCode();

        Packet packet = new Packet.Builder(Subchannel.QUERY)
                .setData(playerUUID.toString())
                .setQuery(QueryType.HAS_PLAYED_BEFORE)
                .setServerGroup(group) // TODO refactor (simplify packet)
                .setHashCode(queryHashCode)
                .build();
        query.setRequest(packet);

        this.subscribe(queryHashCode, query);

        ExecutorService services = Executors.newFixedThreadPool(2);
        Future<Boolean> response = services.submit(query);
        services.submit(() -> {
            try {
                boolean hasPlayedBefore = response.get();

                if (hasPlayedBefore) {
                    Packet hasPlayedBeforePacket = new Packet.Builder(Subchannel.EVENT)
                            .setData(player.getName())
                            .setEventType(EventType.HAS_PLAYED_BEFORE)
                            .setPlayerUuid(playerUUID)
                            .setServerGroup(group)
                            .build();
                    sendToAll(hasPlayedBeforePacket);
                } else {
                    Packet greetingPacket = new Packet.Builder(Subchannel.EVENT)
                            .setData(player.getName())
                            .setPlayerUuid(playerUUID)
                            .setEventType(EventType.FIRST_GROUP_CONNECTION)
                            .setServerGroup(group) // TODO refactor (simplify packet)
                            .build();
                    sendToAll(greetingPacket);
                }

            } catch (InterruptedException e) { // TODO better exception handling
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void onFirstConnection(String playerUUID) {
        this.plugin.getLogger().warning("OnFirstConnection not implemented yet.");
    }

    private void subscribe(int hash, QuerySpigotServer<Boolean> subscriber) {
        this.subscribers.put(hash, subscriber);
    }

    private void unsubscribe(int hash) {
        this.subscribers.remove(hash);
    }

    private void notifySubscriber(int hash, boolean hasPlayedBefore) {
        this.subscribers.get(hash).update(hasPlayedBefore);
    }

    private class ConnectionToClient {
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;
        private Socket socket;

        private Thread handler;
        private AtomicBoolean isConnected;

        public ConnectionToClient(Socket socket) throws IOException {

            if (socket.isClosed())
                throw new IllegalStateException("Socket for client " + socket + " is closed.");

            this.socket = socket;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());

            this.isConnected = new AtomicBoolean(true);

            this.handler = new Thread("playerjoingroup.bungee.messagemanager.socket") {
                @Override
                public void run() {
                    while(isConnected.get()) {
                        try {
                            int length = dataInputStream.readInt();
                            if (length > 0) {
                                byte[] msg = new byte[length];
                                dataInputStream.read(msg, 0, msg.length);

                                synchronized (messages) {
                                    messages.add(new Message(ConnectionToClient.this, msg));
                                    messages.notifyAll();
                                }
                            }
                        } catch (IOException e) {
                            if (!(e instanceof EOFException))
                                throw new RuntimeException(e);
                        }
                    }
                }
            };

            handler.setDaemon(true);
            handler.start();
        }

        public void write(byte[] msg) throws IOException {
            dataOutputStream.writeInt(msg.length);
            dataOutputStream.write(msg);
        }

        public void close() throws IOException, InterruptedException {
            this.isConnected.set(false);
            this.socket.close();
            this.handler.join();
        }

        public boolean isConnected() {
            return this.isConnected.get();
        }
    }

    private class Message {
        private ConnectionToClient client;
        private byte[] message;

        public Message(ConnectionToClient client, byte[] message) {
            this.client = client;
            this.message = message;
        }

        public ConnectionToClient getClient() {
            return client;
        }

        public byte[] getMessage() {
            return message;
        }
    }
}
