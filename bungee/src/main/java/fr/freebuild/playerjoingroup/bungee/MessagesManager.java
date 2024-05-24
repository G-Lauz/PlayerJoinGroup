package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessagesManager {

    private final PlayerJoinGroup plugin;
    private ServerSocket serverSocket;
    private Hashtable<String, ConnectionToClient> clients;
    private Queue<Message> messages;
    private AtomicBoolean isRunning;
    private Thread acceptThread;
    private Thread consumerThread;

    public MessagesManager(PlayerJoinGroup plugin, int port) throws IOException {
        this.plugin = plugin;

        this.messages = new LinkedList<>();
        this.clients = new Hashtable<>();
        this.serverSocket = new ServerSocket(port);

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
                    } catch (IOException err) {
                        MessagesManager.this.plugin.getLogger().severe(err.getMessage());
                        throw new RuntimeException(err);
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
            case HANDSHAKE -> this.handleHandshakeSubchannel(packet, client);
            default -> throw new UnknownSubchannelException(subchannel, "Will ignore this message");
        }
    }

    private void handleEventSubchannel(Packet packet) {
        String eventType = packet.getField(ParamsKey.EVENT);
        switch (EventType.typeof(eventType)) {
            case SERVER_CONNECT -> onServerConnect(packet);
            case SERVER_DISCONNECT -> onServerDisconnect(packet);
            default -> MessagesManager.this.plugin.getLogger().warning("Unknown event: " + eventType);
        }
    }

    private void handleHandshakeSubchannel(Packet packet, ConnectionToClient client) {
        // TODO proper handshake
        clients.put(packet.getData(), client);

        Packet ack = new Packet.Builder("HANDSHAKE")
                .setData("ACK")
                .build();
        sendToOne(packet.getData(), ack);
    }

    private void onServerConnect(Packet packet) {
        String playerUUID = packet.getField(ParamsKey.PLAYER_UUID);
        String playerName = packet.getField("PLAYER_NAME");
        String serverName = packet.getField("SERVER_NAME");
        String serverGroup = Utils.getServerGroupName(serverName, this.plugin.getConfig());

        boolean hasPlayedBefore = Boolean.parseBoolean(packet.getData());
        EventType eventType = hasPlayedBefore ? EventType.GROUP_CONNECTION : EventType.FIRST_GROUP_CONNECTION;

        Packet eventPacket = new Packet.Builder(Subchannel.EVENT)
                .setEventType(eventType)
                .setData(playerName)
                .setPlayerUuid(UUID.fromString(playerUUID))
                .setServerGroup(serverGroup)
                .build();
        sendToAll(eventPacket);
    }

    private void onServerDisconnect(Packet packet) {
        String playerUUID = packet.getField(ParamsKey.PLAYER_UUID);
        String playerName = packet.getField("PLAYER_NAME");
        String serverName = packet.getField("SERVER_NAME");
        String serverGroup = Utils.getServerGroupName(serverName, this.plugin.getConfig());

        Packet disconnectionPacket = new Packet.Builder(Subchannel.EVENT)
                .setEventType(EventType.GROUP_DECONNECTION)
                .setData(playerUUID)
                .appendParam("PLAYER_NAME", playerName)
                .setPlayerUuid(UUID.fromString(playerUUID))
                .setServerGroup(serverGroup)
                .build();

        this.plugin.getMessagesManager().sendToAll(disconnectionPacket);
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
                } catch (IOException | InterruptedException err) {
                    this.plugin.getLogger().severe(Arrays.toString(err.getStackTrace()));
                    throw new RuntimeException(err);
                }
            }
        } catch (InvalidPacketException | ConstructPacketErrorException err) { // TODO better exception handling
            this.plugin.getLogger().severe(Arrays.toString(err.getStackTrace()));
            throw new RuntimeException(err);
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
            dataOutputStream.flush();
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
