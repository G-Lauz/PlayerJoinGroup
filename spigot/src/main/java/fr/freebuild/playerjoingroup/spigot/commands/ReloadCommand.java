package fr.freebuild.playerjoingroup.spigot.commands;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends Command {
    private final PlayerJoinGroup plugin;

    public ReloadCommand(PlayerJoinGroup plugin) {
        super("reload", "Reload the plugin", "playerjoingroup.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.plugin.reloadConfig();
        if (!this.plugin.isMessageManagerEnabled()) {
            this.plugin.enableMessageManager();
        }
        this.plugin.getFireworkBuilder().load();
        sender.sendMessage("§aPlayerJoinGroup has been reloaded.");
    }
}
