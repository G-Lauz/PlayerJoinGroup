package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;

import static org.bukkit.Bukkit.getLogger;

public class SocketConnectedListener implements Listener {

    private final PlayerJoinGroup plugin;

    public SocketConnectedListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSocketConnected(SocketConnectedEvent event) {
        getLogger().info("Connected to the proxy as " + event.getServerName());
    }
}
