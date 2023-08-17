package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.actions.DisconnectAction;
import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.ServerGroupNotFoundException;
import fr.freebuild.playerjoingroup.bungee.Utils;
import fr.freebuild.playerjoingroup.core.event.EventType;

import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerDisconnectListener implements Listener {

    private final PlayerJoinGroup plugin;

    public PlayerDisconnectListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(PlayerDisconnectEvent event) throws ServerGroupNotFoundException {
        ProxiedPlayer player = event.getPlayer();
        String serverName = player.getServer().getInfo().getName();
        String group = Utils.getServerGroupName(serverName, this.plugin.getConfig());

        if (group == null) {
            this.plugin.getLogger().warning(serverName + " his part of any group of server.");
            this.plugin.getLogger().warning("Make sure you have the right configuration.");
            throw new ServerGroupNotFoundException(serverName + " his part of any group of server.");
        }

        String reason = EventType.SERVER_DISCONNECT.getValue();
        Packet packet = new Packet.Builder(Subchannel.EVENT)
                .setEventType(EventType.SERVER_DISCONNECT)
                .setData(reason)
                .setPlayerUuid(player.getUniqueId())
                .appendParam("SERVER_NAME", serverName)
                .appendParam("PLAYER_NAME", player.getName())
                .setServerGroup(group)
                .build();

        this.plugin.getMessagesManager().sendToAll(packet);
        this.plugin.getMessagesManager().addCommand(new DisconnectAction(
                this.plugin, serverName, player.getName(), player.getUniqueId(), reason, 1000
        ));
    }
}
