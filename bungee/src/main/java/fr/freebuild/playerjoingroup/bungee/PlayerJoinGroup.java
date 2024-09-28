package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.bungee.listener.PlayerDisconnectListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerJoinListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerSwitchListener;

import fr.freebuild.playerjoingroup.core.log.DebugLogger;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;

public class PlayerJoinGroup extends Plugin {

    public static PlayerJoinGroup plugin;

    private Config config;
    private MessagesManager messagesManager;

    private DebugLogger logger;

    public PlayerJoinGroup() {
        PlayerJoinGroup.plugin = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        try {
            config = new Config(this);

            this.logger = new DebugLogger(this.getLogger(), config.isDebugMode());

            if (config.isDebugMode()) {
                this.logger.info("Debug mode enabled.");
            }

        } catch (Exception err) {
            this.logger.severe("Unable to load the configuration. The plugin won't respond:");
            this.logger.severe(err.getMessage());
        }

        getProxy().getPluginManager().registerListener(this, new PlayerJoinListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerSwitchListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener(this));

        enableMessageManager();

    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.disableMessageManager();
    }

    public void enableMessageManager() {
        try {
            this.messagesManager = new MessagesManager(this, this.config.getPort(), this.logger);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void disableMessageManager() {
        if (this.messagesManager != null) {
            try {
                this.messagesManager.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Config getConfig() {
        return config;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
