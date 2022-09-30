package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.IOException;

public class SocketConnectedListener implements Listener {

    private final PlayerJoinGroup plugin;

    public SocketConnectedListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSocketConnected(SocketConnectedEvent event) {
        try {
            Packet packet = new Packet.Builder("HANDSHAKE")
                    .setData(event.getServerName())
                    .build();
            this.plugin.getMessageManager().send(Protocol.constructPacket(packet));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ConstructPacketErrorException e) {
            throw new RuntimeException(e);
        } catch (InvalidPacketException e) {
            throw new RuntimeException(e);
        }
    }
}
