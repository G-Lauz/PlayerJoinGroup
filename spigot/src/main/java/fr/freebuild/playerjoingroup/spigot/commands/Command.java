package fr.freebuild.playerjoingroup.spigot.commands;

import org.bukkit.command.CommandSender;

public abstract class Command {
    private final String name;
    private final String permission;
    private final String description;

    protected Command(String name) {
        this(name, null, null);
    }

    protected Command(String name, String permission) {
        this(name,null, permission);
    }

    protected Command(String name, String description, String permission) {
        this.name = name;
        this.permission = permission;
        this.description = description;
    }

    public abstract void execute(CommandSender sender, String[] args);

    public boolean hasPermission(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }
}
