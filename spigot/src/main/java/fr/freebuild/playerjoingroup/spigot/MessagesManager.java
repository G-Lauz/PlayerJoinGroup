package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import fr.freebuild.playerjoingroup.spigot.listener.SocketConnectedListener;
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
import static org.bukkit.Bukkit.getServer;

public class MessagesManager {

    private ConnectionToServer server;
    private Queue<byte[]> messages;
    private Socket socket;

    private String ip;
    private int port;

    public MessagesManager(String ip, int port) {
        this.messages = new LinkedList<>();
        this.ip = ip;
        this.port = port;
    }

    public void initialize() throws IOException {
        this.socket = new Socket(this.ip, this.port);
        this.server = new ConnectionToServer(socket);

        if (this.server == null)
            throw new RuntimeException("MessageManager wasn't initialize.");

        String serverName = PlayerJoinGroup.plugin.getConfig().getString("ServerName");
        Bukkit.getPluginManager().callEvent(new SocketConnectedEvent(serverName));

        Thread consumer = new Thread("playerjoingroup.spigot.messagemanager.consumer") {
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

                        byte[] msg = messages.remove();
                        messages.notifyAll();

                        try {
                            Packet packet = Protocol.deconstructPacket(msg);

//                            System.out.println("Received: " + packet.toString());

                            String subchannel = packet.getSubchannel();
                            switch (Subchannel.typeof(subchannel)) {
                                case BROADCAST -> onBroadcastReceived(packet.getData());
                                case EVENT -> {
                                    String event = packet.getParams().get(ParamsKey.EVENT.getValue());
                                    switch (EventType.typeof(event)) {
                                        case JOIN_SERVER_GROUP -> /*onPlayerJoin(packet);*/{
                                            System.out.println("Received: " + packet.getParams().get("PLAYER_NAME") + " " + event);
                                            onPlayerJoin(packet);}
                                        case LEAVE_SERVER_GROUP -> /*onPlayerLeave(packet);*/{
                                            System.out.println("Received: " + packet.getParams().get("PLAYER_NAME") + " " + event);
                                            onPlayerLeave(packet);
                                        }
                                        case FIRST_GROUP_CONNECTION -> /*onFirstConnection(packet.getData());*/ {
                                            System.out.println("Received: " + packet.getData() + " " + event);
                                            onFirstConnection(packet.getData());
                                        }
                                        case HAS_PLAYED_BEFORE -> onHasPlayedBefore(packet);
                                        default -> getLogger().warning("Unknown event: " + event);
                                    }
                                }
                                case QUERY -> {
                                    String query = packet.getParams().get(ParamsKey.QUERY.getValue());
                                    switch (QueryType.typeof(query)) {
                                        case HAS_PLAYED_BEFORE -> /*onQueryHasPlayedBefore(packet.getParams().get(ParamsKey.HASH_CODE.getValue()), packet.getData());*/ {
                                            System.out.println("Received: " + packet.getData() + " " + query);
                                            onQueryHasPlayedBefore(packet.getParams().get(ParamsKey.HASH_CODE.getValue()), packet.getData());
                                        }
                                        default -> getLogger().warning("Unknown query: " + query);
                                    }
                                }
                                default -> getLogger().warning("Received unhandle action: " + subchannel);
                            }

                        } catch (DeconstructPacketErrorException | InvalidPacketException |
                                 ConstructPacketErrorException | IOException e) {
                            getLogger().severe(Arrays.toString(e.getStackTrace()));
                        }
                    }
                }
            }
        };

        consumer.setDaemon(true);
        consumer.start();
    }

    public void send(byte[] msg) throws IOException {
        if (this.server == null)
            throw new RuntimeException("MessageManager not initialize. Cannot send message."); // TODO: better exception
        this.server.write(msg);
    }

    /*
     COPY .....
     */
    private void onPlayerLeave(Packet packet) {
        String playerName = packet.getParams().get("PLAYER_NAME");
        String message = Utils.getConfigString("QuitMessage");
        message = Utils.format(message, FormatParam.PLAYER, playerName);
        getServer().broadcastMessage(message);
    }

    private void onPlayerJoin(Packet packet) throws InvalidPacketException, ConstructPacketErrorException, IOException {
        String playerName = packet.getParams().get("PLAYER_NAME");

        OfflinePlayer player = getOfflinePlayer(UUID.fromString(packet.getData()));

        // TODO WIP

        if (!player.hasPlayedBefore()) {

            // TODO doesn't seems right
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null)
                return;

            Packet firstConnection = new Packet.Builder(Subchannel.EVENT)
                    .setData(player.getUniqueId().toString())
                    .setEventType(EventType.FIRST_SPIGOT_CONNECTION)
                    .build();

            System.out.println("Sending: " + playerName + " FirstSpigotConnection");

            byte[] message = Protocol.constructPacket(firstConnection);
            this.send(message);
        } else {
            String message = Utils.getConfigString("JoinMessage");
            message = Utils.format(message, FormatParam.PLAYER, playerName);
            getServer().broadcastMessage(message);
        }

//        if (!player.hasPlayedBefore()) {
//            Packet packet = new Packet.Builder(Subchannel.EVENT)
//                    .setData(player.getUniqueId().toString())
//                    .setEventType(EventType.FIRST_SPIGOT_CONNECTION)
//                    .build();
//            byte[] message = Protocol.constructPacket(packet);
//
//            // TODO refactor player
//            Player onlinePlayer = getPlayer(UUID.fromString(data));
//            if (onlinePlayer == null)
//                return;
////            onlinePlayer.sendPluginMessage(this.plugin, this.plugin.getChannel(), message);
//            this.send(message);
//        } else {
//            String message = Utils.getConfigString("JoinMessage");
//            message = Utils.format(message, FormatParam.PLAYER, player.getName());
//            getServer().broadcastMessage(message);
//        }
    }

    private void onQueryHasPlayedBefore(String hashCode, String playerUUID) throws InvalidPacketException, ConstructPacketErrorException, IOException {
        Player player = getPlayer(UUID.fromString(playerUUID));
        if (player == null)
            player = (Player) getOnlinePlayers().toArray()[0];

        Packet packet = new Packet.Builder(Subchannel.QUERY)
                .setData(Boolean.toString(player.hasPlayedBefore()))
                .setHashCode(Integer.parseInt(hashCode))
                .setQuery(QueryType.HAS_PLAYED_BEFORE_RESPONSE)
                .build();
//        player.sendPluginMessage(this.plugin, this.plugin.getChannel(), Protocol.constructPacket(packet));

        System.out.println("Sending: Answer query HasPlayedBefore");

        this.send(Protocol.constructPacket(packet));
        System.out.println("SENT");
    }

    private void onFirstConnection(String playerName) {
        Player player = getPlayer(playerName);

        String message = Utils.getConfigString("FirstJoinMessage");

        final Integer counter = Utils.increaseCounter("PlayerCounter");
        message = Utils.format(message, FormatParam.COUNTER, counter.toString());

//        if (PlayerJoinGroup.plugin.getFireworkBuilder().getActivateOnJoin() && player != null) {
//            PlayerJoinGroup.plugin.getFireworkBuilder().spawn(player);
//        }

        message = Utils.format(message, FormatParam.PLAYER, playerName);
        getServer().broadcastMessage(message);
    }

    private void onHasPlayedBefore(Packet packet) {
        String message = Utils.getConfigString("JoinMessage");
        message = Utils.format(message, FormatParam.PLAYER, packet.getData());
        getServer().broadcastMessage(message);
    }

    private void onBroadcastReceived(String data) {
    }

    /*
     COPY ^^^^
     */

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
                    while (!socket.isClosed()) { // TODO better loop control
                        try {
                            int length = dataInputStream.readInt();
                            if (length > 0) {
                                byte[] msg = new byte[length];
                                dataInputStream.read(msg, 0, msg.length);
//                                System.out.println("Received: " + msg);

                                synchronized (messages) {
                                    messages.add(msg);
                                    messages.notifyAll();
                                }
                            }
                        } catch (IOException e) {
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
}
