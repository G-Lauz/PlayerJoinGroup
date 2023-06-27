package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;

import fr.freebuild.playerjoingroup.spigot.utils.FormatParam;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static org.bukkit.Bukkit.getServer;

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
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && PlayerJoinGroup.plugin.getFireworkBuilder().getActivateOnJoin())
            PlayerJoinGroup.plugin.getFireworkBuilder().spawn(player);

        event.setJoinMessage(null);
        if (!this.plugin.isMessageManagerEnabled()) {
            String playerName = event.getPlayer().getDisplayName();
            if (!event.getPlayer().hasPlayedBefore()) {
                this.onFirstConnection(playerName);
            } else {
                this.onHasPlayedBefore(playerName);
            }
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
        if (!this.plugin.isMessageManagerEnabled()) {
            this.onPlayerLeave(event.getPlayer().getDisplayName());
        }
    }

    private void onFirstConnection(String playerName) {
        String message = Utils.getConfigString("FirstJoinMessage");
        final Integer counter = Utils.increaseCounter("PlayerCounter");
        message = Utils.format(message, FormatParam.COUNTER, counter.toString());
        message = Utils.format(message, FormatParam.PLAYER, playerName);
        getServer().broadcastMessage(message);
    }

    private void onHasPlayedBefore(String playerName) {
        String message = Utils.getConfigString("JoinMessage");
        message = Utils.format(message, FormatParam.PLAYER, playerName);
        getServer().broadcastMessage(message);
    }

    private void onPlayerLeave(String playerName) {
        String message = Utils.getConfigString("QuitMessage");
        message = Utils.format(message, FormatParam.PLAYER, playerName);
        getServer().broadcastMessage(message);
    }
}
