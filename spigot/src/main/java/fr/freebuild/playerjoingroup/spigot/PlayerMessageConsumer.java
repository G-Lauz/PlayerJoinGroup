package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.Connection;
import fr.freebuild.playerjoingroup.core.action.ActionExecutor;
import fr.freebuild.playerjoingroup.core.MessageConsumer;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.actions.ConnectAction;
import fr.freebuild.playerjoingroup.spigot.actions.DisconnectAction;
import fr.freebuild.playerjoingroup.spigot.actions.HandshakeAction;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static org.bukkit.Bukkit.*;

public class PlayerMessageConsumer implements MessageConsumer {
    private final PlayerJoinGroup plugin;
    private final ActionExecutor actionExecutor;

    private final Logger logger;

    public PlayerMessageConsumer(PlayerJoinGroup plugin, ActionExecutor actionExecutor, Logger logger) {
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
        this.logger = logger;
    }

    @Override
    public void processMessage(Connection connection, byte[] message) {
        try {
            Packet packet = Protocol.deconstructPacket(message);
            String subchannel = packet.getSubchannel(); // TODO refactor remove subchannel?

            this.handleMessageBySubchannel(subchannel, packet);
        } catch (DeconstructPacketErrorException | InvalidPacketException |
                 ConstructPacketErrorException | IOException | UnknownSubchannelException err) {
            this.logger.severe(Arrays.toString(err.getStackTrace()));
        }
    }

    private void handleMessageBySubchannel(String subchannel, Packet packet)
            throws ConstructPacketErrorException, IOException, InvalidPacketException, UnknownSubchannelException {
        Subchannel subchannelType = Subchannel.typeof(subchannel);

        switch (subchannelType) {
            case EVENT -> this.handleEventSubchannel(packet);
            case HANDSHAKE -> this.handleHandshake(packet);
            case HANDSHAKE_ACK -> this.onHandshakeAck(packet);
            default -> this.logger.warning("Received unhandle action: " + subchannel);
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
            default -> this.logger.warning("Unknown event: " + event);
        }
    }

    private void handleHandshake(Packet packet) {
        String serverName = this.plugin.getConfig().getString("ServerName");

        Packet handshake = new Packet.Builder(Subchannel.HANDSHAKE)
                .setData(serverName)
                .build();

        try {
            this.plugin.getMessageManager().send(Protocol.constructPacket(handshake));

            HandshakeAction action = new HandshakeAction(this.plugin.getMessageManager(), serverName, 1000);
            this.plugin.getMessageManager().getActionExecutor().resolve(action, serverName);
        } catch (IOException | ConstructPacketErrorException | InvalidPacketException e) {
            this.logger.severe("An error occurred while sending packet to bungeecord");
            throw new RuntimeException(e);
        }
    }

    private void onHandshakeAck(Packet packet) {
        String serverName = packet.getData();

        HandshakeAction action = new HandshakeAction(this.plugin.getMessageManager(), serverName, 1000);
        this.plugin.getMessageManager().getActionExecutor().resolve(action, serverName);
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
            this.actionExecutor.resolve(command, hasPlayedBefore);
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
            this.actionExecutor.resolve(command, null);
        }
    }

    private Boolean canDisplayMessage(Packet packet, String perm) {
        UUID playerUUID = UUID.fromString(packet.getField(ParamsKey.PLAYER_UUID));
        Player player = getOfflinePlayer(playerUUID).getPlayer();

        return player == null || !player.hasPermission(perm);
    }
}
