package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.actions.ConnectAction;
import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.ServerGroupNotFoundException;
import fr.freebuild.playerjoingroup.bungee.Utils;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final PlayerJoinGroup plugin;

    public PlayerJoinListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(ServerConnectEvent event) throws ServerGroupNotFoundException {
        ProxiedPlayer player = event.getPlayer();

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            UUID playerUUID = player.getUniqueId();
            String serverName = event.getTarget().getName();
            String group  = Utils.getServerGroupName(serverName, this.plugin.getConfig());

            if (group == null) {
                this.plugin.getLogger().warning(serverName + " his part of any group of server.");
                this.plugin.getLogger().warning("Make sure you have the right configuration.");
                throw new ServerGroupNotFoundException(serverName + " his part of any group of server.");
            }

            String reason = EventType.SERVER_CONNECT.getValue();
            Packet packet = new Packet.Builder(Subchannel.EVENT)
                    .setEventType(EventType.SERVER_CONNECT)
                    .setData(reason)
                    .setPlayerUuid(player.getUniqueId())
                    .appendParam("SERVER_NAME", serverName)
                    .appendParam("PLAYER_NAME", player.getName())
                    .setServerGroup(group)
                    .build();

            this.plugin.getMessagesManager().sendToAll(packet);
            this.plugin.getMessagesManager().addCommand(new ConnectAction(
                    this.plugin, serverName, player.getName(), playerUUID, reason, 1000
            ));
        }
    }
}
