package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.spigot.actions.ConnectAction;
import fr.freebuild.playerjoingroup.spigot.actions.DisconnectAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.spigot.utils.Utils;

import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final PlayerJoinGroup plugin;

    public PlayerJoinListener(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when player join
     * Override default message
     *
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && PlayerJoinGroup.plugin.getFireworkBuilder().getActivateOnJoin())
            PlayerJoinGroup.plugin.getFireworkBuilder().spawn(player);

        if (!this.plugin.isMessageManagerEnabled()) {
            String message;
            if (!player.hasPlayedBefore())
                message = Utils.getFirstConnectionMessage(player.getDisplayName());
            else
                message = Utils.getHasPlayedBeforeMessage(player.getDisplayName());
            event.setJoinMessage(message);
        } else {
            event.setJoinMessage(null);

            String serverName = this.plugin.getConfig().getString("ServerName");
            UUID playerUUID = player.getUniqueId();
            String playerName = player.getDisplayName();
            String eventName = EventType.SERVER_CONNECT.getValue();

            if (player == null || !player.hasPermission("essentials.silentjoin")) {
                // Use a timeout of 10 seconds to allow the player to join before sending the connect action
                // This is intended for players with a higher ping.
                // 10 seconds as been chosen to match a ping of 500ms to which, behond this value, Minecraft raises a timeout error.
                ConnectAction action = new ConnectAction(this.plugin, serverName, playerName, playerUUID, eventName, 10000);
                this.plugin.getMessageManager().getActionExecutor().resolve(action, player.hasPlayedBefore());
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
        if (!this.plugin.isMessageManagerEnabled())
            event.setQuitMessage(Utils.getPlayerLeaveMessage(event.getPlayer().getDisplayName()));
        else {
            event.setQuitMessage(null);

            String serverName = this.plugin.getConfig().getString("ServerName");
            UUID playerUUID = event.getPlayer().getUniqueId();
            String playerName = event.getPlayer().getDisplayName();
            String eventName = EventType.SERVER_DISCONNECT.getValue();

            Player player = event.getPlayer();

            if (player == null || !player.hasPermission("essentials.silentquit")) {
                DisconnectAction action = new DisconnectAction(this.plugin, serverName, playerName, playerUUID, eventName, 1000);
                this.plugin.getMessageManager().getActionExecutor().resolve(action, null);
            }
        }
    }

}
