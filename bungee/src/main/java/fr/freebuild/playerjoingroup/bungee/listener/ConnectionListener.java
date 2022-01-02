package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.ServerGroupNotFoundException;
import fr.freebuild.playerjoingroup.bungee.Utils;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

public abstract class ConnectionListener implements Listener {

    protected final PlayerJoinGroup plugin;

    public ConnectionListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    protected void broadcastEvent(ServerInfo serverInfo, ProxiedPlayer player, EventType event)
            throws ServerGroupNotFoundException {

        String serverName = serverInfo.getName();
        String group = Utils.getServerGroupName(serverName, this.plugin.getConfig());

        if (group == null) {
            this.plugin.getLogger().warning(serverName + " his part of any group of server.");
            this.plugin.getLogger().warning("Make sure you have the right configuration.");
            throw new ServerGroupNotFoundException(serverName + " his part of any group of server.");
        }

        Packet packet = new Packet.Builder(Subchannel.EVENT)
                .setData(player.getUniqueId().toString())
                .setEventType(event)
                .setServerGroup(group)
                .build();

        this.plugin.getMessager().broadcast(packet);
    }
}
