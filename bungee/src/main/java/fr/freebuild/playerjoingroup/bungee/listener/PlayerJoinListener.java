package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.ServerGroupNotFoundException;
import fr.freebuild.playerjoingroup.core.event.EventType;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.event.EventHandler;

public class PlayerJoinListener extends ConnectionListener {

    public PlayerJoinListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(ServerConnectEvent event) throws ServerGroupNotFoundException {
        ProxiedPlayer player = event.getPlayer();

        // TODO remove logs?
        this.plugin.getLogger().info(player.getName() + " connect to " + event.getTarget());

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY)
            broadcastEvent(event.getTarget(), player, EventType.JOIN_SERVER_GROUP);
    }
}
