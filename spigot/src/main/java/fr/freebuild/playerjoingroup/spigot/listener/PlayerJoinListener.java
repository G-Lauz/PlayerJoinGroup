package fr.freebuild.playerjoingroup.spigot.listener;

import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.spigot.ConnectCommand;
import fr.freebuild.playerjoingroup.spigot.DisconnectCommand;
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
        }

        String serverName = this.plugin.getConfig().getString("ServerName");
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getDisplayName();
        String eventName = EventType.SERVER_CONNECT.getValue();

        ConnectCommand command = new ConnectCommand(this.plugin, serverName, playerName, playerUUID, eventName, 1000);
        this.plugin.getMessageManager().executeOrAddCommand(command, player.hasPlayedBefore());
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
        else
            event.setQuitMessage(null);

        String serverName = this.plugin.getConfig().getString("ServerName");
        UUID playerUUID = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getDisplayName();
        String eventName = EventType.SERVER_DISCONNECT.getValue();

        DisconnectCommand command = new DisconnectCommand(this.plugin, serverName, playerName, playerUUID, eventName, 1000);
        this.plugin.getMessageManager().executeOrAddCommand(command, null);
    }

}
