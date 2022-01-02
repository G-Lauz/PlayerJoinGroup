package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.spigot.firework.FireworkBuilder;
import fr.freebuild.playerjoingroup.spigot.listener.PlayerJoinListener;
import fr.freebuild.playerjoingroup.spigot.listener.PluginMessageReceiver;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class PlayerJoinGroup extends JavaPlugin {

    public static PlayerJoinGroup plugin;
    private FireworkBuilder fireworkBuilder;

    public PlayerJoinGroup() {
        PlayerJoinGroup.plugin = this;
        this.fireworkBuilder = new FireworkBuilder();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (!this.checkIfBungee()) {
            this.disablePlugin(
                    "This server is not hook to BungeeCord.",
                    "If the server is already hooked to BungeeCord, enable it into your spigot.yml as well."
            );
            return;
        }

        try {
            PluginMessageReceiver pluginMessageListener = new PluginMessageReceiver();
            this.registerIOChannel(pluginMessageListener);
        } catch (Exception err) {
            getLogger().severe(err.getMessage());
            err.printStackTrace();
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        this.fireworkBuilder = new FireworkBuilder();
        this.saveResource("config.yml", false);
        this.fireworkBuilder.load();
    }

    private boolean checkIfBungee() {
        return getServer().spigot().getConfig().getBoolean("settings.bungeecord");
    }

    private void disablePlugin(String... messages) {
        Arrays.stream(messages).forEach(msg -> getLogger().severe(msg));
        getLogger().severe("Plugin disabled!");
        getServer().getPluginManager().disablePlugin(this.plugin);
    }

    private void registerIOChannel(PluginMessageReceiver listener) {
        getServer().getMessenger().registerIncomingPluginChannel(this.plugin, listener.getChannel(), listener);
        getServer().getMessenger().registerOutgoingPluginChannel(this.plugin, listener.getChannel());
    }

    public FireworkBuilder getFireworkBuilder() {
        return fireworkBuilder;
    }
}
