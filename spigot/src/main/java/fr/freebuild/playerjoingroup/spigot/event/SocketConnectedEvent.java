package fr.freebuild.playerjoingroup.spigot.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SocketConnectedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String serverName;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public SocketConnectedEvent(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public String getServerName() {
        return serverName;
    }
}
