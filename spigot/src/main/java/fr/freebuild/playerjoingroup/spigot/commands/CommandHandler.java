package fr.freebuild.playerjoingroup.spigot.commands;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final Map<String, Command> commands;
    private final String commandName;

    public CommandHandler(PlayerJoinGroup plugin, String commandName) {
        this.commands = new HashMap<>();
        this.commandName = commandName.toLowerCase();

        plugin.getCommand(commandName).setExecutor(this);
        plugin.getCommand(commandName).setTabCompleter(this);

        this.register(new HelpCommand(this, plugin.getName()));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String s, String[] args) {
        if (!command.getName().equals(this.commandName) || args.length < 1)
            return false;

        Command cmd = commands.get(args[0].toLowerCase());
        if (cmd == null)
            return false;

        if (!cmd.hasPermission(sender)) {
            sender.sendMessage("§cYou don't have the permission to execute this command.");
            return true;
        }

        List<String> argList = Arrays.asList(args).subList(1, args.length);
        cmd.execute(sender, argList.toArray(new String[0]));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String s, String[] args) {
        if (!command.getName().equals(this.commandName) || args.length < 1)
            return null;

        List<String> results = new LinkedList<>();
        boolean ignoreArg = args.length == 0;
        for (Command cmd: commands.values())
            if (cmd.hasPermission(sender) && (ignoreArg || cmd.getName().startsWith(args[0])))
                results.add(cmd.getName());
        return results;
    }

    public void register(Command command) {
        this.commands.put(command.getName().toLowerCase(), command);
    }

    public Map<String, Command> getCommands() {
        return commands;
    }
}
