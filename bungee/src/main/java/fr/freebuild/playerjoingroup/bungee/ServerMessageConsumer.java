package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.Connection;
import fr.freebuild.playerjoingroup.core.MessageConsumer;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

public class ServerMessageConsumer implements MessageConsumer {

    private final PlayerJoinGroup plugin;
    private final MessagesManager messageManager;
    private final Logger logger;

    public ServerMessageConsumer(PlayerJoinGroup plugin, MessagesManager messageManager, Logger logger) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.logger = logger;
    }

    @Override
    public void processMessage(Connection connection, byte[] message) {
        try {
            Packet packet = Protocol.deconstructPacket(message);
            String subchannel = packet.getSubchannel();

            this.handleMessageBySubchannel(subchannel, packet, connection);
        } catch (DeconstructPacketErrorException | IOException | UnknownSubchannelException err) {
            this.logger.severe(Arrays.toString(err.getStackTrace()));
        }
    }

    private void handleMessageBySubchannel(String subchannel, Packet packet, Connection connection)
            throws IOException, UnknownSubchannelException {
        Subchannel subchannelType = Subchannel.typeof(subchannel);

        switch (subchannelType) {
            case BROADCAST -> this.messageManager.sendToAll(packet);
            case EVENT -> this.handleEventSubchannel(packet);
            case HANDSHAKE -> this.handleHandshakeSubchannel(packet, connection);
            default -> throw new UnknownSubchannelException(subchannel, "Will ignore this message");
        }
    }

    private void handleEventSubchannel(Packet packet) {
        String eventType = packet.getField(ParamsKey.EVENT);
        switch (EventType.typeof(eventType)) {
            case SERVER_CONNECT -> onServerConnect(packet);
            case SERVER_DISCONNECT -> onServerDisconnect(packet);
            default -> this.logger.warning("Unknown event: " + eventType);
        }
    }

    private void handleHandshakeSubchannel(Packet packet, Connection connection) {
        String serverName = packet.getData();

        this.messageManager.updateClientName(connection, serverName);

        Packet ack = new Packet.Builder(Subchannel.HANDSHAKE_ACK)
                .setData(serverName)
                .build();
        this.messageManager.sendToOne(serverName, ack);
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
        this.messageManager.sendToAll(eventPacket);
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

        this.messageManager.sendToAll(disconnectionPacket);
    }
}
