package fr.freebuild.playerjoingroup.spigot;

public class ServerIsNotBungeecordException extends Exception {
    public ServerIsNotBungeecordException() {
        super("ServerIsNotBungeecordException: The server doesn't use a Bungeecord proxy.");
    }
}
