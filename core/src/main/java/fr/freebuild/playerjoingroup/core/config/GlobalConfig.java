package fr.freebuild.playerjoingroup.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GlobalConfig {

    private String channel;

    public GlobalConfig() throws IOException, LoadConfigFileException {
        this.loadConfig();
    }


    public GlobalConfig loadConfig() throws IOException, LoadConfigFileException {
        InputStream configFile = null;
        Properties properties = null;

        try {
            configFile = GlobalConfig.class.getResourceAsStream("/config.properties");
            properties = new Properties();

            properties.load(configFile);

            this.channel = properties.getProperty("channel");

        } catch (FileNotFoundException fnfe) {
            throw new LoadConfigFileException(fnfe.getMessage());
        } catch (IOException ioe) {
            throw new LoadConfigFileException(ioe.getMessage());
        } finally {
            if (configFile != null)
                configFile.close();
            return this;
        }
    }

    public String getChannel() {
        return channel;
    }
}
