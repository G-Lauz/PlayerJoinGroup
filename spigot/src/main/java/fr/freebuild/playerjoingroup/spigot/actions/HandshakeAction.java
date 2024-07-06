package fr.freebuild.playerjoingroup.spigot.actions;

import fr.freebuild.playerjoingroup.core.action.Action;
import fr.freebuild.playerjoingroup.spigot.MessagesManager;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import org.bukkit.Bukkit;

public class HandshakeAction extends Action<String> {
    private final MessagesManager messageManager;
    private final String hostAddress;

    public HandshakeAction(MessagesManager messageManager, String hostAddress, long timeout) {
        super(timeout);
        this.messageManager = messageManager;
        this.hostAddress = hostAddress;
    }

    @Override
    public void execute(String serverName) {
        Bukkit.getPluginManager().callEvent(new SocketConnectedEvent(serverName));
    }

    @Override
    public int hashCode() {
        return this.hostAddress.hashCode();
    }
}
