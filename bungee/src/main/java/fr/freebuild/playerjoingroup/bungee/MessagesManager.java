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

// TODO split in different files?

public class MessagesManager {

    private final PlayerJoinGroup plugin;
    private ServerSocket serverSocket;
    private Hashtable<String, ConnectionToClient> clients;
    private Queue<Message> messages;

    private HashMap<Integer, QuerySpigotServer<Boolean>> subscribers;

    // TODO make singleton
    public MessagesManager(PlayerJoinGroup plugin, int port) throws IOException {
        this.plugin = plugin;

        this.messages = new LinkedList<>();
        this.clients = new Hashtable<>();
        this.serverSocket = new ServerSocket(port);

        this.subscribers = new HashMap<>();

        Thread accept = new Thread("playerjoingroup.bungee.messagemanager.accept") {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) { // TODO better loop control
                    try {
                        new ConnectionToClient(serverSocket.accept());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        accept.setDaemon(true);
        accept.start();

        Thread consumer = new Thread("playerjoingroup.bungee.messagemanager.consumer") {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) { // TODO better loop control
                    synchronized (messages) {
                        if (messages.isEmpty()) {
                            try {
                                messages.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        Message msg = messages.remove();
                        messages.notifyAll();

                        // TODO chain of responsibility (maybe)?
                        try {
                            Packet packet = Protocol.deconstructPacket(msg.getMessage());

                            String subchannel = packet.getSubchannel(); // TODO using subchannel might be depracated (simplify protocol?)
                            switch (Subchannel.valueOf(subchannel)) {
                                case BROADCAST:
                                    sendToAll(packet);
                                    break;

                                case EVENT:
                                    String eventType = packet.getField(ParamsKey.EVENT);
                                    switch (EventType.typeof(eventType)) {
                                        case FIRST_SPIGOT_CONNECTION -> onFirstConnection(packet.getData()); // TODO we don't receive it anymore
                                        default -> MessagesManager.this.plugin.getLogger().warning("Unknown event: " + eventType);
                                    }
                                    break;

                                case QUERY:
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
                                    break;

                                case HANDSHAKE: // TODO proper handshake (with Query?)
                                    clients.put(packet.getData(), msg.getClient());

                                    Packet ack = new Packet.Builder("HANDSHAKE")
                                            .setData("ACK")
                                            .build();
                                    sendToOne(packet.getData(), ack);
                                    break;

                                default:
                                    MessagesManager.this.plugin.getLogger().warning("Received packet with unknown subchannel: " + subchannel);
                                    throw new UnknownSubchannelException(subchannel);
                            }

                        } catch (DeconstructPacketErrorException e) { // TODO better exception handling
                            throw new RuntimeException(e);
                        }  catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (UnknownSubchannelException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        };

        consumer.setDaemon(true);
        consumer.start();
    }

    public void sendToOne(String server, Packet packet) throws IOException {

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
        } catch (InvalidPacketException e) { // TODO better exception handling
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ConstructPacketErrorException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void sendToAll(Packet packet) throws IOException {
        String group = packet.getField(ParamsKey.SERVER_GROUP);

        this.plugin.getConfig().getGroup().forEach((serverGroup, servers) -> {
            if (serverGroup.equals(group) || group.equals("ALL")) {
                ((ArrayList) servers).forEach(server -> {
                    try {
                        sendToOne((String)server, packet);
                    } catch (IOException e) {
                        throw new RuntimeException(e); // TODO better exception handle
                    }
                });
            }
        });
    }

    public void sendQueryHasPlayedBefore(String serverName, String playerUUID) { // TODO refactor / extract methods / make  Query a core component
        ProxiedPlayer player =  this.plugin.getProxy().getPlayer(UUID.fromString(playerUUID));
        Config config = this.plugin.getConfig();
        String group = Utils.getServerGroupName(serverName, config);

        QuerySpigotServer<Boolean> query = new QuerySpigotServer<>(serverName, MessagesManager.this);
        int queryHashCode = query.hashCode();

        Packet packet = new Packet.Builder(Subchannel.QUERY)
                .setData(playerUUID)
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
                            .setServerGroup(group)
                            .build();
                    sendToAll(hasPlayedBeforePacket);
                } else {
                    Packet greetingPacket = new Packet.Builder(Subchannel.EVENT)
                            .setData(player.getName())
                            .setEventType(EventType.FIRST_GROUP_CONNECTION)
                            .setServerGroup(group) // TODO refactor (simplify packet)
                            .build();
                    sendToAll(greetingPacket);
                }

            } catch (InterruptedException e) { // TODO better exception handling
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
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

        public ConnectionToClient(Socket socket) throws IOException {

            if (socket.isClosed())
                throw new IllegalStateException("Socket for client " + socket + " is closed.");

            this.socket = socket;
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            Thread handler = new Thread("playerjoingroup.bungee.messagemanager.socket") {
                @Override
                public void run() {
                    while(!socket.isClosed()) { // TODO, better loop control
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
