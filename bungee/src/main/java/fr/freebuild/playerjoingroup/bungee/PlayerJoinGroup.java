package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.bungee.listener.PlayerDisconnectListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerJoinListener;
import fr.freebuild.playerjoingroup.bungee.listener.PlayerSwitchListener;
import fr.freebuild.playerjoingroup.bungee.listener.PluginMessageReceiver;

import net.md_5.bungee.api.plugin.Plugin;

public class PlayerJoinGroup extends Plugin {

    private Config config;
    private Messager messager;

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
    }

    public Config getConfig() {
        return config;
    }

    public Messager getMessager() {
        return messager;
    }
}
