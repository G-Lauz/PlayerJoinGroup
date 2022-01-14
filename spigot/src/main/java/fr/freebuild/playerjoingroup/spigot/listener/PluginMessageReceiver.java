package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.spigot.utils.FormatParam;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.UUID;

import static org.bukkit.Bukkit.*;

public class PluginMessageReceiver implements PluginMessageListener {

    private final PlayerJoinGroup plugin;

    public PluginMessageReceiver(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals(this.plugin.getChannel()))
            return;

        try {
            Packet packet = Protocol.deconstructPacket(bytes);

            String subchannel = packet.getSubchannel();
            switch (Subchannel.typeof(subchannel)) {
                case BROADCAST -> onBroadcastReceived(packet.getData());
                case EVENT -> {
                    String event = packet.getParams().get(ParamsKey.EVENT.getValue());
                    switch (EventType.typeof(event)) {
                        case JOIN_SERVER_GROUP -> onPlayerJoin(packet.getData());
                        case LEAVE_SERVER_GROUP -> onPlayerLeave(packet.getData());
                        case FIRST_GROUP_CONNECTION -> onFirstConnection(packet.getData());
                        default -> getLogger().warning("Unknown event: " + event);
                    }
                }
                case QUERY -> {
                    String query = packet.getParams().get(ParamsKey.QUERY.getValue());
                    switch (QueryType.typeof(query)) {
                        case HAS_PLAYED_BEFORE -> onQueryHasPlayedBefore(packet.getParams().get(ParamsKey.HASH_CODE.getValue()), packet.getData());
                        default -> getLogger().warning("Unknown query: " + query);
                    }
                }
                default -> getLogger().warning("Received unhandle action: " + subchannel);
            }

        } catch (DeconstructPacketErrorException | InvalidPacketException | ConstructPacketErrorException e) {
            getLogger().severe(Arrays.toString(e.getStackTrace()));
        }
    }

    private void onPlayerLeave(String data) {
        OfflinePlayer player = getOfflinePlayer(UUID.fromString(data));
        String message = Utils.getConfigString("QuitMessage");
        message = Utils.format(message, FormatParam.PLAYER, player.getName());
        getServer().broadcastMessage(message);
    }

    private void onPlayerJoin(String data) throws InvalidPacketException, ConstructPacketErrorException {
        OfflinePlayer player = getOfflinePlayer(UUID.fromString(data));

        if (!player.hasPlayedBefore()) {
            Packet packet = new Packet.Builder(Subchannel.EVENT)
                    .setData(player.getUniqueId().toString())
                    .setEventType(EventType.FIRST_SPIGOT_CONNECTION)
                    .build();
            byte[] message = Protocol.constructPacket(packet);

            // TODO refactor player
            Player onlinePlayer = getPlayer(UUID.fromString(data));
            if (onlinePlayer == null)
                return;
            onlinePlayer.sendPluginMessage(this.plugin, this.plugin.getChannel(), message);
        } else {
            String message = Utils.getConfigString("JoinMessage");
            message = Utils.format(message, FormatParam.PLAYER, player.getName());
            getServer().broadcastMessage(message);
        }
    }

    private void onQueryHasPlayedBefore(String hashCode, String playerUUID) throws InvalidPacketException, ConstructPacketErrorException {
        Player player = getPlayer(UUID.fromString(playerUUID));
        if (player == null)
            player = (Player) getOnlinePlayers().toArray()[0];

        Packet packet = new Packet.Builder(Subchannel.QUERY)
                .setData(Boolean.toString(player.hasPlayedBefore()))
                .setHashCode(Integer.parseInt(hashCode))
                .setQuery(QueryType.HAS_PLAYED_BEFORE_RESPONSE)
                .build();
        player.sendPluginMessage(this.plugin, this.plugin.getChannel(), Protocol.constructPacket(packet));
    }

    private void onFirstConnection(String playerName) {
        Player player = getPlayer(playerName);

        String message = Utils.getConfigString("FirstJoinMessage");

        final Integer counter = Utils.increaseCounter("PlayerCounter");
        message = Utils.format(message, FormatParam.COUNTER, counter.toString());

        if (PlayerJoinGroup.plugin.getFireworkBuilder().getActivateOnJoin() && player != null) {
            PlayerJoinGroup.plugin.getFireworkBuilder().spawn(player);
        }

        message = Utils.format(message, FormatParam.PLAYER, playerName);
        getServer().broadcastMessage(message);
    }

    private void onBroadcastReceived(String data) {
    }
}
