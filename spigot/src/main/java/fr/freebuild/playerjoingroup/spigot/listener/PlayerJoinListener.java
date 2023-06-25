package fr.freebuild.playerjoingroup.spigot.listener;

import static org.bukkit.Bukkit.getServer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;

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
        if (!this.plugin.isMessageManagerEnabled()) {
            String message;
            if (!player.hasPlayedBefore())
                message = Utils.getFirstConnectionMessage(player.getDisplayName());
            else
                message = Utils.getHasPlayedBeforeMessage(player.getDisplayName());
            getServer().broadcastMessage(message);
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
        if (!this.plugin.isMessageManagerEnabled())
            getServer().broadcastMessage(Utils.getPlayerLeaveMessage(event.getPlayer().getDisplayName()));
    }

}
