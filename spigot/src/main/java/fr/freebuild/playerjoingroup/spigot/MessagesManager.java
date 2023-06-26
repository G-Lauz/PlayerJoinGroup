package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import fr.freebuild.playerjoingroup.spigot.utils.FormatParam;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static org.bukkit.Bukkit.*;

// TODO split in different file?

public class MessagesManager {

    private final PlayerJoinGroup plugin;
    private ConnectionToServer server;
    private Queue<byte[]> messages;
    private Socket socket;

    private String ip;
    private int port;

    public MessagesManager(String ip, int port, PlayerJoinGroup plugin) {
        this.messages = new LinkedList<>();
        this.ip = ip;
        this.port = port;
        this.plugin = plugin;
    }

    public void initialize() {
        this.connect();
        this.startConsumerThread();
    }

    private void connect() {
        Thread connectionThread = new Thread("playerjoingroup.spigot.messagemanager.connection") {
            @Override
            public void run() {
                int sleepTime = PlayerJoinGroup.plugin.getConfig().getInt("ReconnectDelay");
                int reconnectAttempts = PlayerJoinGroup.plugin.getConfig().getInt("ReconnectAttempts");

                int retryCount = 0;
                while (retryCount < reconnectAttempts) {
                    try {
                        Thread.sleep(sleepTime);
                        establishConnection();
                        return;
                    } catch (IOException error) {
                        getLogger().warning("Unable to connect to the proxy. Did you specify the correct IP and port?");
                        getLogger().warning("Retrying in 5 seconds...");
                        retryCount++;
                    } catch (InterruptedException error) {
                        getLogger().warning("Retry connection interrupted: " + error.getMessage());
                    }
                }

                getLogger().warning("Exceeded maximum retry attempts. Connection failed.");
                getLogger().warning("Will use the default join message.");
                MessagesManager.this.plugin.enableMessageManager(false);
            }
        };

        connectionThread.start();
    }

    private void establishConnection() throws IOException {
        this.socket = new Socket(this.ip, this.port);
        this.server = new ConnectionToServer(socket);

        String serverName = PlayerJoinGroup.plugin.getConfig().getString("ServerName");
        getLogger().info("Connected to the proxy as " + serverName);
        Bukkit.getPluginManager().callEvent(new SocketConnectedEvent(serverName));
    }

    private void startConsumerThread() {
        Thread consumer = new Thread("playerjoingroup.spigot.messagemanager.consumer") {
            @Override
            public void run() {
                consumeMessage();
            }
        };

        consumer.setDaemon(true);
        consumer.start();
    }
    private void consumeMessage() {
        while(!Thread.currentThread().isInterrupted()) {
            synchronized (messages) {
                while (messages.isEmpty()) {
                    this.waitForMessage();
                }

                byte[] msg = messages.remove();
                messages.notifyAll();

                this.processMessage(msg);
            }
        }
    }

    private void waitForMessage() {
        try {
            messages.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processMessage(byte[] message) {
        try {
            Packet packet = Protocol.deconstructPacket(message);
            String subchannel = packet.getSubchannel(); // TODO refactor remove subchannel?

            this.handleMessageBySubchannel(subchannel, packet);
        } catch (DeconstructPacketErrorException | InvalidPacketException |
                 ConstructPacketErrorException | IOException err) {
            getLogger().severe(Arrays.toString(err.getStackTrace()));
        }
    }

    private void handleMessageBySubchannel(String subchannel, Packet packet) throws ConstructPacketErrorException, IOException, InvalidPacketException {
        Subchannel subchannelType = Subchannel.typeof(subchannel);

        switch (subchannelType) {
            case EVENT -> this.handleEventSubchannel(packet);
            case QUERY -> this.handleQuerySubchannel(packet);
            case HANDSHAKE -> {
                break; // TODO proper handshake (with Query?)
            }
            default -> getLogger().warning("Received unhandle action: " + subchannel);
        }
    }

    private void handleEventSubchannel(Packet packet) throws ConstructPacketErrorException, IOException, InvalidPacketException {
        String event = packet.getParams().get(ParamsKey.EVENT.getValue());
        EventType eventType = EventType.typeof(event);

        switch (eventType) {
            case JOIN_SERVER_GROUP -> onPlayerJoin(packet);
            case LEAVE_SERVER_GROUP -> onPlayerLeave(packet);
            case FIRST_GROUP_CONNECTION -> onFirstConnection(packet);
            case HAS_PLAYED_BEFORE -> onHasPlayedBefore(packet);
            default -> getLogger().warning("Unknown event: " + event);
        }
    }

    private void handleQuerySubchannel(Packet packet) throws ConstructPacketErrorException, IOException, InvalidPacketException {
        String query = packet.getParams().get(ParamsKey.QUERY.getValue());
        QueryType queryType = QueryType.typeof(query);

        switch (queryType) {
            case HAS_PLAYED_BEFORE -> onQueryHasPlayedBefore(packet.getParams().get(ParamsKey.HASH_CODE.getValue()), packet.getData());
            default -> getLogger().warning("Unknown query: " + query);
        }
    }

    public void send(byte[] msg) throws IOException {
        if (this.server == null)
            throw new RuntimeException("MessageManager not initialize. Cannot send message."); // TODO: better exception
        this.server.write(msg);
    }

    private void onPlayerLeave(Packet packet) {
        if (canDisplayMessage(packet, "essentials.silentquit"))  {
            String message = Utils.getPlayerLeaveMessage(packet.getParams().get("PLAYER_NAME"));
            getServer().broadcastMessage(message);
        }
    }

    private void onPlayerJoin(Packet packet) throws InvalidPacketException, ConstructPacketErrorException, IOException {
        OfflinePlayer player = getOfflinePlayer(UUID.fromString(packet.getData()));
        String hashCode = packet.getParams().get(ParamsKey.HASH_CODE.getValue());

        Packet answer = new Packet.Builder(Subchannel.QUERY)
                .setData(Boolean.toString(player.hasPlayedBefore()))
                .setHashCode(Integer.parseInt(hashCode))
                .setQuery(QueryType.HAS_PLAYED_BEFORE_RESPONSE)
                .build();
        this.send(Protocol.constructPacket(answer));
    }

    private void onQueryHasPlayedBefore(String hashCode, String playerUUID) throws InvalidPacketException, ConstructPacketErrorException, IOException {
        OfflinePlayer player = getOfflinePlayer(UUID.fromString(playerUUID));

        Packet packet = new Packet.Builder(Subchannel.QUERY)
                .setData(Boolean.toString(player.hasPlayedBefore()))
                .setHashCode(Integer.parseInt(hashCode))
                .setQuery(QueryType.HAS_PLAYED_BEFORE_RESPONSE)
                .build();
        this.send(Protocol.constructPacket(packet));
    }

    private void onFirstConnection(Packet packet) {
        if (canDisplayMessage(packet, "essentials.silentjoin"))  {
            String message = Utils.getFirstConnectionMessage(packet.getData());
            getServer().broadcastMessage(message);
        }
    }

    private void onHasPlayedBefore(Packet packet) {
        if (canDisplayMessage(packet, "essentials.silentjoin"))  {
            String message = Utils.getHasPlayedBeforeMessage(packet.getData());
            getServer().broadcastMessage(message);
        }
    }

    private Boolean canDisplayMessage(Packet packet, String perm) {
        UUID playerUUID = UUID.fromString(packet.getParams().get(ParamsKey.PLAYER_UUID.getValue()));
        Player player = getOfflinePlayer(playerUUID).getPlayer();

        return player == null || !player.hasPermission(perm);
    }

    private class ConnectionToServer {
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;
        Socket socket;

        public ConnectionToServer(Socket socket) throws IOException {
            this.socket = socket;

            if (socket.isClosed())
                throw new IllegalStateException("Socket for client " + socket + " is closed.");

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            Thread handler = new Thread("playerjoingroup.spigot.messagemanager.socket") {
                @Override
                public void run() {
                    boolean isConnected = true;

                    while (isConnected) {
                        try {
                            int length = dataInputStream.readInt();
                            if (length > 0) {
                                byte[] msg = new byte[length];
                                dataInputStream.read(msg, 0, msg.length);

                                synchronized (messages) {
                                    messages.add(msg);
                                    messages.notifyAll();
                                }
                            }
                        } catch (IOException error) {
                            isConnected = false;
                            getLogger().warning("Connection to server lost, will attempt to reconnect...");
                        }
                    }

                    // Attempt to reconnect a new instance
                    MessagesManager.this.connect();
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
}
