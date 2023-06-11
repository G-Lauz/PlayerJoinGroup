package fr.freebuild.playerjoingroup.spigot.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HelpCommand extends Command {
    private final CommandHandler commandHandler;
    private final String pluginName;

    protected HelpCommand(CommandHandler commandHandler, String pluginName) {
        super("help", "Show this help message", "playerjoingroup.use");
        this.commandHandler = commandHandler;
        this.pluginName = pluginName;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        StringBuilder builder = new StringBuilder()
                .append(ChatColor.GOLD)
                .append(this.pluginName)
                .append(ChatColor.RESET)
                .append("\nCommands: \n");

        for (Command command : this.commandHandler.getCommands().values()) {
            if (command.hasPermission(sender)) {
                builder.append("- ").append(ChatColor.BLUE).append(command.getName()).append(ChatColor.RESET);
                if (command.getDescription() != null) {
                    builder.append(": ").append(command.getDescription());
                }
                builder.append("\n");
            }
        }
        sender.sendMessage(builder.deleteCharAt(builder.length() - 1).toString());
    }
}
