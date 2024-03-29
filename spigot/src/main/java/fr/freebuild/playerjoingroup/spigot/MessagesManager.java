package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.Action;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.actions.ActionExecutionException;
import fr.freebuild.playerjoingroup.spigot.actions.ConnectAction;
import fr.freebuild.playerjoingroup.spigot.actions.DisconnectAction;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Bukkit.*;

public class MessagesManager {

    private final PlayerJoinGroup plugin;
    private ConnectionToServer server;
    private Queue<byte[]> messages;
    private Socket socket;

    private String ip;
    private int port;

    private Thread connectionThread;
    private Thread consumerThread;
    private AtomicBoolean isRunning;
    private HashMap<Integer, Action> commandIndex;
    private final Object lock = new Object();

    public MessagesManager(String ip, int port, PlayerJoinGroup plugin) {
        this.messages = new LinkedList<>();
        this.ip = ip;
        this.port = port;
        this.plugin = plugin;

        this.connectionThread = null;
        this.consumerThread = null;

        this.isRunning = new AtomicBoolean(false);

        this.commandIndex = new HashMap<>();
    }

    public void initialize() {
        this.isRunning.set(true);
        this.connect();
        this.startConsumerThread();
    }

    private void connect() {
        this.connectionThread = new Thread("playerjoingroup.spigot.messagemanager.connection") {
            @Override
            public void run() {
                int sleepTime = PlayerJoinGroup.plugin.getConfig().getInt("ReconnectDelay");
                int reconnectAttempts = PlayerJoinGroup.plugin.getConfig().getInt("ReconnectAttempts");

                int retryCount = 0;
                while (retryCount < reconnectAttempts && MessagesManager.this.isRunning.get()) {
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
                MessagesManager.this.plugin.disableMessageManager();
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

    public void close() throws IOException, InterruptedException {
        this.isRunning.set(false);

        if (this.server != null)
            this.server.close();

        this.connectionThread.join();

        if (this.consumerThread != null && this.consumerThread.isAlive()) {
            this.consumerThread.interrupt();
            this.consumerThread.join();
        }
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }

    private void startConsumerThread() {
        this.consumerThread = new Thread("playerjoingroup.spigot.messagemanager.consumer") {
            @Override
            public void run() {
                consumeMessage();
            }
        };

        this.consumerThread.setDaemon(true);
        this.consumerThread.start();
    }

    private void consumeMessage() {
        while(!Thread.currentThread().isInterrupted() && this.isRunning.get()) {
            synchronized (messages) {
                while (messages.isEmpty() && !Thread.currentThread().isInterrupted()) {
                    this.waitForMessage();
                }

                if (Thread.currentThread().isInterrupted())
                    break;

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
            Thread.currentThread().interrupt();
        }
    }

    private void processMessage(byte[] message) {
        try {
            Packet packet = Protocol.deconstructPacket(message);
            String subchannel = packet.getSubchannel(); // TODO refactor remove subchannel?

            this.handleMessageBySubchannel(subchannel, packet);
        } catch (DeconstructPacketErrorException | InvalidPacketException |
                 ConstructPacketErrorException | IOException | UnknownSubchannelException err) {
            getLogger().severe(Arrays.toString(err.getStackTrace()));
        }
    }

    private void handleMessageBySubchannel(String subchannel, Packet packet)
            throws ConstructPacketErrorException, IOException, InvalidPacketException, UnknownSubchannelException {
        Subchannel subchannelType = Subchannel.typeof(subchannel);

        switch (subchannelType) {
            case EVENT -> this.handleEventSubchannel(packet);
            case HANDSHAKE -> {
                break; // TODO proper handshake
            }
            default -> getLogger().warning("Received unhandle action: " + subchannel);
        }
    }

    private void handleEventSubchannel(Packet packet) {
        String event = packet.getField(ParamsKey.EVENT);
        EventType eventType = EventType.typeof(event);

        switch (eventType) {
            case GROUP_DECONNECTION -> onPlayerLeave(packet);
            case FIRST_GROUP_CONNECTION -> onFirstConnection(packet);
            case GROUP_CONNECTION -> onHasPlayedBefore(packet);
            case SERVER_CONNECT -> onServerConnect(packet);
            case SERVER_DISCONNECT -> onServerDisconnect(packet);
            default -> getLogger().warning("Unknown event: " + event);
        }
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

    private void onPlayerLeave(Packet packet) {
        if (canDisplayMessage(packet, "essentials.silentquit"))  {
            String message = Utils.getPlayerLeaveMessage(packet.getField("PLAYER_NAME"));
            getServer().broadcastMessage(message);
        }
    }

    private void onServerConnect(Packet packet) {
        String event = packet.getField(ParamsKey.EVENT);
        String serverName = packet.getField("SERVER_NAME");
        String playerName = packet.getField("PLAYER_NAME");
        UUID playerUUID = UUID.fromString(packet.getField(ParamsKey.PLAYER_UUID));

        OfflinePlayer offlinePlayer = this.plugin.getServer().getOfflinePlayer(playerUUID);
        Player player = offlinePlayer.getPlayer();

        if (player == null || !player.hasPermission("essentials.silentjoin")) {
            boolean hasPlayedBefore = offlinePlayer.hasPlayedBefore();
            ConnectAction command = new ConnectAction(this.plugin, serverName, playerName, playerUUID, event, 1000);
            this.executeOrAddCommand(command, hasPlayedBefore);
        }
    }

    private void onServerDisconnect(Packet packet) {
        String event = packet.getField(ParamsKey.EVENT);
        String serverName = packet.getField("SERVER_NAME");
        String playerName = packet.getField("PLAYER_NAME");
        UUID playerUUID = UUID.fromString(packet.getField(ParamsKey.PLAYER_UUID));

        OfflinePlayer offlinePlayer = this.plugin.getServer().getOfflinePlayer(playerUUID);
        Player player = offlinePlayer.getPlayer();

        if (player == null || !player.hasPermission("essentials.silentquit")) {
            DisconnectAction command = new DisconnectAction(this.plugin, serverName, playerName, playerUUID, event, 1000);
            this.executeOrAddCommand(command, null);
        }
    }

    private Boolean canDisplayMessage(Packet packet, String perm) {
        UUID playerUUID = UUID.fromString(packet.getField(ParamsKey.PLAYER_UUID));
        Player player = getOfflinePlayer(playerUUID).getPlayer();

        return player == null || !player.hasPermission(perm);
    }

    public void addCommand(Action action) {
        synchronized (this.lock) {
            this.commandIndex.put(action.hashCode(), action);
        }
    }

    private <T> void executeCommand(int hashCode, T context) throws ActionExecutionException {
        synchronized (this.lock) {
            Action action = this.commandIndex.get(hashCode);

            if (action == null)
                throw new ActionExecutionException("Command with hashcode " + hashCode + " not found.", false);

            if (action.isExpired()) {
                this.commandIndex.remove(hashCode);
                throw new ActionExecutionException("Command with hashcode " + hashCode + " is expired.", true);
            }

            action.execute(context);
            this.commandIndex.remove(hashCode);
        }
    }

    public <T> void executeOrAddCommand(Action action, T context) {
        this.removeExpiredCommand();

        try {
            this.executeCommand(action.hashCode(), context);
        } catch (ActionExecutionException err) {
            if (err.commandIsExpired())
                this.plugin.getLogger().warning("Command " + action.hashCode() + " is expired.");
            else
                this.addCommand(action);
        }
    }

    public void removeExpiredCommand() {
        synchronized (this.lock) {
            Iterator<Map.Entry<Integer, Action>> iterator = this.commandIndex.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Action> entry = iterator.next();
                if (entry.getValue().isExpired())
                    iterator.remove();
            }
        }
    }

    public void send(byte[] msg) throws IOException {
        if (this.server == null)
            throw new RuntimeException("MessageManager not initialize. Cannot send message."); // TODO: better exception
        this.server.write(msg);
    }

    private class ConnectionToServer {
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;
        private Socket socket;

        private AtomicBoolean isConnected;
        private Thread handler;

        public ConnectionToServer(Socket socket) throws IOException {
            this.socket = socket;

            if (socket.isClosed())
                throw new IllegalStateException("Socket for client " + socket + " is closed.");

            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            isConnected = new AtomicBoolean(true);

            handler = new Thread("playerjoingroup.spigot.messagemanager.socket") {
                @Override
                public void run() {
                    while (isConnected.get()) {
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
                            isConnected.set(false);
                            getLogger().warning(error.getMessage());
                            getLogger().warning("Connection to server lost, will attempt to reconnect...");
                        }
                    }

                    // Attempt to reconnect a new instance
                    if (MessagesManager.this.isRunning())
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

        public void close() throws IOException, InterruptedException {
            isConnected.set(false);
            socket.close();
            handler.join();
        }

        public boolean isConnected() {
            return isConnected.get();
        }
    }
}
