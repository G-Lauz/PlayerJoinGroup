package fr.freebuild.playerjoingroup.core.protocol;

public class InvalidPacketException extends Exception{
    public InvalidPacketException(String message) {
        super("InvalidPacketException: " + message);
    }
}
