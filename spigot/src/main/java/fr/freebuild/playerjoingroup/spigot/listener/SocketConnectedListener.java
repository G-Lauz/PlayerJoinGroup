package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.log.DebugLogger;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SocketConnectedListener implements Listener {

    private DebugLogger logger;

    public SocketConnectedListener(DebugLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onSocketConnected(SocketConnectedEvent event) {
        logger.info("Connected to the proxy as " + event.getServerName());
    }
}
