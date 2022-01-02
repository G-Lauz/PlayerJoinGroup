package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.spigot.utils.FormatParam;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    /**
     * Called when player join
     * Overridade default message
     *
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        if (!event.getPlayer().hasPlayedBefore()) {
            String message = Utils.getConfigString("FirstJoinMessage");

            final Integer counter = Utils.increaseCounter("PlayerCounter");
            message = Utils.format(message, FormatParam.COUNTER, counter.toString());

            message = Utils.format(message, FormatParam.PLAYER, event.getPlayer().getName());
            Bukkit.getServer().broadcastMessage(message);
        }
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
