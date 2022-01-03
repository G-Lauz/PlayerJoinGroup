package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.core.event.EventType;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.event.EventHandler;

public class PlayerJoinListener extends ConnectionListener {

    public PlayerJoinListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY)
            scheduledBroadcastEvent(event.getTarget(), player, EventType.JOIN_SERVER_GROUP, 1);
    }
}
