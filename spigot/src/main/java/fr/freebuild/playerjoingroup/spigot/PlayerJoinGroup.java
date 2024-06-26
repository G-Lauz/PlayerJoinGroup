package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.spigot.commands.CommandHandler;
import fr.freebuild.playerjoingroup.spigot.commands.ReloadCommand;
import fr.freebuild.playerjoingroup.spigot.commands.StatusCommand;
import fr.freebuild.playerjoingroup.spigot.firework.FireworkBuilder;
import fr.freebuild.playerjoingroup.spigot.listener.PlayerJoinListener;

import fr.freebuild.playerjoingroup.spigot.listener.SocketConnectedListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.Socket;

public class PlayerJoinGroup extends JavaPlugin {

    public static PlayerJoinGroup plugin;
    private FireworkBuilder fireworkBuilder;
    private MessagesManager messagesManager;

    public PlayerJoinGroup() {
        PlayerJoinGroup.plugin = this;
        this.fireworkBuilder = new FireworkBuilder();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        this.messagesManager = null;

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        CommandHandler commandHandler = new CommandHandler(this, "playerjoingroup");
        commandHandler.register(new ReloadCommand(this));
        commandHandler.register(new StatusCommand(this));

        this.fireworkBuilder = new FireworkBuilder();
        this.saveResource("config.yml", false);
        this.fireworkBuilder.load();

        if (this.checkIfBungee()) {
            this.enableMessageManager();
        } else {
            getLogger().warning("This server is not hook to BungeeCord. The group feature will not work. And each new connection will be handle locally.");
            getLogger().warning("If the server is already hooked to BungeeCord, enable it into your spigot.yml as well.");
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.disableMessageManager();
    }

    private boolean checkIfBungee() {
        return getServer().spigot().getConfig().getBoolean("settings.bungeecord");
    }

    public FireworkBuilder getFireworkBuilder() {
        return fireworkBuilder;
    }

    public MessagesManager getMessageManager() {
        return this.messagesManager;
    }

    public void enableMessageManager() {
        try {
            FileConfiguration config = this.getConfig();

            String serverName = config.getString("ServerName");

            String proxyIP = config.getString("ProxyIP");
            int proxyPort = config.getInt("ProxyPort");

            Socket socket = new Socket(proxyIP, proxyPort);

            int maxAttempts = config.getInt("ReconnectAttempts");
            int delay = config.getInt("ReconnectDelay");
            RetryPolicy retryPolicy = new ConstantRetryPolicy(maxAttempts, delay);

            getServer().getPluginManager().registerEvents(new SocketConnectedListener(this),this);

            ActionExecutor actionExecutor = new ActionExecutor(this);
            PlayerMessageConsumer playerMessageConsumer = new PlayerMessageConsumer(this, actionExecutor);
            ConnectionToServer server = new ConnectionToServer(serverName, socket, playerMessageConsumer, retryPolicy);

            this.messagesManager = new MessagesManager(server, actionExecutor);
        } catch (IOException exception) {
            getLogger().warning("Unable to connect to the proxy server. The group feature will not work. And each new connection will be handle locally.");
            throw new RuntimeException(exception);
        }
    }

    public void disableMessageManager() {
        if (this.messagesManager != null) {
            this.messagesManager.close();
        }
    }

    public boolean isMessageManagerEnabled() {
        return this.messagesManager != null && this.messagesManager.isEnable();
    }
}
