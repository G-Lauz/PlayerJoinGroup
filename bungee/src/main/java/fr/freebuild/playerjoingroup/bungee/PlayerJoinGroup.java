package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.bungee.listener.PlayerDisconnectListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerJoinListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerSwitchListener;
import fr.freebuild.playerjoingroup.bungee.listener.PluginMessageReceiver;

import net.md_5.bungee.api.plugin.Plugin;

public class PlayerJoinGroup extends Plugin {

    public static PlayerJoinGroup plugin;

    private Config config;
    private Messager messager;

    public PlayerJoinGroup() {
        PlayerJoinGroup.plugin = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        messager = new Messager(this);

        try {
            config = new Config(this);
        } catch (Exception err) {
            getLogger().severe("Unable to load the configuration. The plugin won't respond:");
            getLogger().severe(err.getMessage());
        }

        getProxy().getPluginManager().registerListener(this, new PluginMessageReceiver(this));
        getProxy().getPluginManager().registerListener(this, new PlayerJoinListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerSwitchListener(this));
        getProxy().getPluginManager().registerListener(this, new PlayerDisconnectListener(this));
        getProxy().registerChannel(this.getConfig().getChannel());
    }

    public Config getConfig() {
        return config;
    }

    public Messager getMessager() {
        return messager;
    }
}
