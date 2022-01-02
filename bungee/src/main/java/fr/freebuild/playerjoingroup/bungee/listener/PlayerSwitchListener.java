package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.ServerGroupNotFoundException;
import fr.freebuild.playerjoingroup.core.event.EventType;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.event.EventHandler;

public class PlayerSwitchListener extends ConnectionListener {

    public PlayerSwitchListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(ServerSwitchEvent event) throws ServerGroupNotFoundException {
        ProxiedPlayer player = event.getPlayer();
        ServerInfo fromServer = event.getFrom();

        if (fromServer != null) {

            // TODO remove logs?
            System.out.print(player.getName() + " switched from " + fromServer.getName() + " to " + player.getServer().getInfo().getName());

            broadcastEvent(fromServer, player, EventType.LEAVE_SERVER_GROUP);
            broadcastEvent(player.getServer().getInfo(), player, EventType.JOIN_SERVER_GROUP);
        }
    }

    @EventHandler
    public void on(ServerConnectEvent event) throws ServerGroupNotFoundException {
        ProxiedPlayer player = event.getPlayer();
        Server server = player.getServer();

        if (server != null) {
            ServerInfo fromServer = server.getInfo();
            ServerInfo toServer = event.getTarget();

            // TODO remove logs
            System.out.print(player.getName() + " connect event from "+ fromServer.getName() + " to " + toServer.getName());

//            broadcastEvent(fromServer, player, EventType.LEAVE_SERVER_GROUP);
//            broadcastEvent(toServer, player, EventType.JOIN_SERVER_GROUP);
        }
    }
}
