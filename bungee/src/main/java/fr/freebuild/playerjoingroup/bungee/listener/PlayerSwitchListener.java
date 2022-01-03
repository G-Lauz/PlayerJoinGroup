package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.Utils;
import fr.freebuild.playerjoingroup.core.event.EventType;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.event.EventHandler;

public class PlayerSwitchListener extends ConnectionListener {

    public PlayerSwitchListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ServerInfo fromServer = event.getFrom();

        if (fromServer != null) {
            String fromGroup = Utils.getServerGroupName(fromServer.getName(), this.plugin.getConfig());
            String toGroup = Utils.getServerGroupName(player.getServer().getInfo().getName(), this.plugin.getConfig());

            if (!fromGroup.equalsIgnoreCase(toGroup)) {
                scheduledBroadcastEvent(fromServer, player, EventType.LEAVE_SERVER_GROUP, 1);
                scheduledBroadcastEvent(player.getServer().getInfo(), player, EventType.JOIN_SERVER_GROUP, 1);
            }
        }
    }
}
