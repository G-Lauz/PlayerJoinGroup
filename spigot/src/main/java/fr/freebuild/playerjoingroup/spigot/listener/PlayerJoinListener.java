package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static org.bukkit.Bukkit.getPlayer;

public class PlayerJoinListener implements Listener {

    private final PlayerJoinGroup plugin;

    public PlayerJoinListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when player join
     * Overridade default message
     *
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // TODO doesn't seem right (you may use the Bukkit scheduler and regroup all the event from the socket together)
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && PlayerJoinGroup.plugin.getFireworkBuilder().getActivateOnJoin())
            PlayerJoinGroup.plugin.getFireworkBuilder().spawn(player);
        event.setJoinMessage(null);
    }

    /**
     * Called when player quit
     * Override default message
     *
     * @param event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }
}
