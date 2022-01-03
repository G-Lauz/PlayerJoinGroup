package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.config.GlobalConfig;
import fr.freebuild.playerjoingroup.core.config.LoadConfigFileException;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.spigot.utils.FormatParam;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.*;

public class PluginMessageReceiver implements PluginMessageListener {

    private final GlobalConfig config;

    public PluginMessageReceiver() throws IOException, LoadConfigFileException {
        this.config = new GlobalConfig();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals(this.config.getChannel()))
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
                        default -> getLogger().warning("Unknown event: " + event);
                    }
                }
                default -> getLogger().warning("Received unhandle action: " + subchannel);
            }

        } catch (DeconstructPacketErrorException deconstructPacketErrorException) {
            getLogger().severe(Arrays.toString(deconstructPacketErrorException.getStackTrace()));
        }
    }

    private void onPlayerLeave(String data) {
        OfflinePlayer player = getOfflinePlayer(UUID.fromString(data));
        String message = Utils.getConfigString("QuitMessage");
        message = Utils.format(message, FormatParam.PLAYER, player.getName());
        getServer().broadcastMessage(message);
    }

    private void onPlayerJoin(String data) {
        String message;
        OfflinePlayer player = getOfflinePlayer(UUID.fromString(data));

        if (!player.hasPlayedBefore()) {
            message = Utils.getConfigString("FirstJoinMessage");

            final Integer counter = Utils.increaseCounter("PlayerCounter");
            message = Utils.format(message, FormatParam.COUNTER, counter.toString());

            if (PlayerJoinGroup.plugin.getFireworkBuilder().getActivateOnJoin()) {
                PlayerJoinGroup.plugin.getFireworkBuilder().spawn(Objects.requireNonNull(player.getPlayer()));
            }
        } else {
            message = Utils.getConfigString("JoinMessage");
        }

        message = Utils.format(message, FormatParam.PLAYER, player.getName());
        getServer().broadcastMessage(message);
    }

    private void onBroadcastReceived(String data) {
    }

    public String getChannel(){
        return this.config.getChannel();
    }
}
