package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.config.GlobalConfig;
import fr.freebuild.playerjoingroup.core.config.LoadConfigFileException;
import fr.freebuild.playerjoingroup.spigot.firework.FireworkBuilder;
import fr.freebuild.playerjoingroup.spigot.listener.PlayerJoinListener;
import fr.freebuild.playerjoingroup.spigot.listener.PluginMessageReceiver;

import fr.freebuild.playerjoingroup.spigot.listener.SocketConnectedListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Arrays;

public class PlayerJoinGroup extends JavaPlugin {

    public static PlayerJoinGroup plugin;
    private final GlobalConfig config;
    private FireworkBuilder fireworkBuilder;

    private MessagesManager messagesManager;

    public PlayerJoinGroup() throws IOException, LoadConfigFileException {
        PlayerJoinGroup.plugin = this;
        this.config = new GlobalConfig();
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

        this.messagesManager = new MessagesManager("bungeecord", 26005);

//        PluginMessageReceiver pluginMessageListener = new PluginMessageReceiver(this);
//        this.registerIOChannel(pluginMessageListener);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new SocketConnectedListener(this),this);

        try {
            this.messagesManager.initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        getServer().getMessenger().registerIncomingPluginChannel(this.plugin, this.getChannel(), listener);
        getServer().getMessenger().registerOutgoingPluginChannel(this.plugin, this.getChannel());
    }

    public String getChannel() {
        return this.config.getChannel();
    }

    public FireworkBuilder getFireworkBuilder() {
        return fireworkBuilder;
    }

    public MessagesManager getMessageManager() {
        return this.messagesManager;
    }
}
