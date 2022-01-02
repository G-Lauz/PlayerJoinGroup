package fr.freebuild.playerjoingroup.core.protocol;

public class DeconstructPacketErrorException extends Exception {
    public DeconstructPacketErrorException(String message) {
        super("DeconstructPacketErrorException: " + message);
    }
}
