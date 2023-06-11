package fr.freebuild.playerjoingroup.spigot.commands;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import org.bukkit.command.CommandSender;

public class StatusCommand extends Command {
    private final PlayerJoinGroup plugin;

    public StatusCommand(PlayerJoinGroup plugin) {
        super("status", "Check if the group feature is enable.", "playerjoingroup.status");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean isMessageManagerEnabled = this.plugin.isMessageManagerEnabled();
        sender.sendMessage("§fPlayerJoinGroup group feature is " + (isMessageManagerEnabled ? "§aenabled" : "§cdisabled") + ".");
    }
}
