package fr.freebuild.playerjoingroup.core.config;

public class LoadConfigFileException extends Exception {
    public LoadConfigFileException(String message) {
        super("LoadConfigFileException: " + message);
    }
}
