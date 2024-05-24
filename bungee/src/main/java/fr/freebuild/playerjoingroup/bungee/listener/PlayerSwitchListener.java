package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.*;
import fr.freebuild.playerjoingroup.core.event.EventType;

import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerSwitchListener implements Listener {

    private final PlayerJoinGroup plugin;

    public PlayerSwitchListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(ServerSwitchEvent event) throws ServerGroupNotFoundException {
        ServerInfo serverInfo = event.getFrom();

        if (serverInfo != null) {
            ProxiedPlayer player = event.getPlayer();

            String fromServer = serverInfo.getName();
            String toServer = player.getServer().getInfo().getName();

            String fromGroup = Utils.getServerGroupName(fromServer, this.plugin.getConfig());
            String toGroup = Utils.getServerGroupName(toServer, this.plugin.getConfig());

            if (fromGroup == null) {
                this.plugin.getLogger().warning(fromServer + " his part of any group of server.");
                this.plugin.getLogger().warning("Make sure you have the right configuration.");
                throw new ServerGroupNotFoundException(fromServer + " his part of any group of server.");
            }

            if (toGroup == null) {
                this.plugin.getLogger().warning(toServer + " his part of any group of server.");
                this.plugin.getLogger().warning("Make sure you have the right configuration.");
                throw new ServerGroupNotFoundException(toServer + " his part of any group of server.");
            }

            if (!fromGroup.equalsIgnoreCase(toGroup)) {
                // Send disconnection message to old server
                String disconnectionReason = EventType.SERVER_DISCONNECT.getValue();
                Packet diconnectionPacket = new Packet.Builder(Subchannel.EVENT)
                        .setEventType(EventType.SERVER_DISCONNECT)
                        .setData(disconnectionReason)
                        .setPlayerUuid(player.getUniqueId())
                        .appendParam("SERVER_NAME", fromServer)
                        .appendParam("PLAYER_NAME", player.getName())
                        .setServerGroup(fromGroup)
                        .build();

                this.plugin.getMessagesManager().sendToAll(diconnectionPacket);

                // Send connection message to new server
                String connectionReason = EventType.SERVER_CONNECT.getValue();
                Packet connectionPacket = new Packet.Builder(Subchannel.EVENT)
                        .setEventType(EventType.SERVER_CONNECT)
                        .setData(connectionReason)
                        .setPlayerUuid(player.getUniqueId())
                        .appendParam("SERVER_NAME", toServer)
                        .appendParam("PLAYER_NAME", player.getName())
                        .setServerGroup(toGroup)
                        .build();

                this.plugin.getMessagesManager().sendToAll(connectionPacket);
            }
        }
    }
}
