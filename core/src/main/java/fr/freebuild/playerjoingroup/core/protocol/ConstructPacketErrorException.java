package fr.freebuild.playerjoingroup.core.protocol;

public class ConstructPacketErrorException extends Exception{
    public ConstructPacketErrorException(String message) {
        super("ConstructPacketErrorException: " + message);
    }
}
