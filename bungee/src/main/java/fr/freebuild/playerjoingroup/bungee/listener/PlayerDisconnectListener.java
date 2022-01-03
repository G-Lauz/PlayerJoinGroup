package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.ServerGroupNotFoundException;
import fr.freebuild.playerjoingroup.core.event.EventType;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnectListener extends ConnectionListener{
    public PlayerDisconnectListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(PlayerDisconnectEvent event) throws ServerGroupNotFoundException {
        ProxiedPlayer player = event.getPlayer();

        broadcastEvent(player.getServer().getInfo(), player, EventType.LEAVE_SERVER_GROUP);
    }
}
