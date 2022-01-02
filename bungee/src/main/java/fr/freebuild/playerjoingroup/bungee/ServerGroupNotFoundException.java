package fr.freebuild.playerjoingroup.bungee;

public class ServerGroupNotFoundException extends Exception {
    public ServerGroupNotFoundException(String message) {
        super("ServerGroupNotFoundException: "+ message);
    }
}
