package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.bungee.listener.PlayerDisconnectListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerJoinListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerSwitchListener;

import net.md_5.bungee.api.plugin.Plugin;

import java.io.IOException;

public class PlayerJoinGroup extends Plugin {

    public static PlayerJoinGroup plugin;

    private Config config;
    private MessagesManager messagesManager;

    public PlayerJoinGroup() {
        PlayerJoinGroup.plugin = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        try {
            config = new Config(this);
        } catch (Exception err) {
            getLogger().severe("Unable to load the configuration. The plugin won't respond:");
            getLogger().severe(err.getMessage());
        }

        getProxy().getPluginManager().registerListener(this, new PlayerJoinListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerSwitchListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener(this));

        try {
            this.messagesManager = new MessagesManager(this, config.getPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO ondisbale close all thread

    public Config getConfig() {
        return config;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
}
