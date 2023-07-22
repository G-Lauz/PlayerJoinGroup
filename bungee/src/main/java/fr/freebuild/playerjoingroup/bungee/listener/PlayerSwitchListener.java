package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.bungee.Utils;
import fr.freebuild.playerjoingroup.core.event.EventType;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerSwitchListener extends ConnectionListener {

    public PlayerSwitchListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ServerInfo fromServer = event.getFrom();

        if (fromServer != null) {
            String fromGroup = Utils.getServerGroupName(fromServer.getName(), this.plugin.getConfig());
            String toGroup = Utils.getServerGroupName(player.getServer().getInfo().getName(), this.plugin.getConfig());

            if (!fromGroup.equalsIgnoreCase(toGroup)) {
                scheduledBroadcastEvent(fromServer, player, EventType.LEAVE_SERVER_GROUP, 1);

                ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                service.schedule(() -> {
                    this.plugin.getMessagesManager().sendQueryHasPlayedBefore(player.getServer().getInfo().getName(), player);
                }, 1, TimeUnit.SECONDS);
            }
        }
    }
}
