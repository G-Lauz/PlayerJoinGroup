package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.actions.ConnectAction;
import fr.freebuild.playerjoingroup.spigot.actions.DisconnectAction;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.*;

public class PlayerMessageConsumer implements MessageConsumer {
    private final PlayerJoinGroup plugin;
    private final ActionExecutor actionExecutor;

    public PlayerMessageConsumer(PlayerJoinGroup plugin, ActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void processMessage(byte[] message) {
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
