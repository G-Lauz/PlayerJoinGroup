package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.config.GlobalConfig;
import fr.freebuild.playerjoingroup.core.config.LoadConfigFileException;

import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.*;

public class Config {

    private final Plugin plugin;

    private String channel;
    private Hashtable group;

    public Config(Plugin plugin) throws IOException, LoadConfigFileException {
        this.plugin = plugin;
        this.channel = null;
        this.group = null;
        this.loadConfig();
    }

    public Config loadConfig() throws IOException, LoadConfigFileException {
        // Load globals configuration
        GlobalConfig globalConfig = new GlobalConfig();
        this.channel = globalConfig.getChannel();

        // Load plugin's configuration
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdir();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile = generateDefaultConfigFile(configFile);
        }

        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            List<?> serverGroup =  config.getList("group");
            group = new Hashtable();
            serverGroup.stream().forEach(item ->
                    ((LinkedHashMap)item).entrySet().forEach(entry ->
                            group.put(
                                    ((Map.Entry<String, ArrayList>)entry).getKey(),
                                    ((Map.Entry<String, ArrayList>)entry).getValue())));

        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration file");
        }
        return this;
    }

    private File generateDefaultConfigFile(File file) {
        try {
            file.createNewFile();

            try (InputStream is = plugin.getResourceAsStream("config.yml");
                 OutputStream os = new FileOutputStream(file)) {
                ByteStreams.copy(is, os);
            }
            return file;

        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file");
        }
    }

    public String getChannel() {
        return channel;
    }

    public Hashtable getGroup() {
        return group;
    }
}
