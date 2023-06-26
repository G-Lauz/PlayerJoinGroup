package fr.freebuild.playerjoingroup.bungee.listener;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerJoinListener extends ConnectionListener {

    public PlayerJoinListener(PlayerJoinGroup plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(ServerConnectEvent event) { // TODO refactor (see ConnectionListener)
        ProxiedPlayer player = event.getPlayer();

        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

            service.schedule(() -> {
                this.plugin.getMessagesManager().sendQueryHasPlayedBefore(event.getTarget().getName(), player);
            }, 1, TimeUnit.SECONDS);
        }
    }
}
