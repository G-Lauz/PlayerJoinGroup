package fr.freebuild.playerjoingroup.core.protocol;

public class UnknownSubchannelException extends Exception{
    public UnknownSubchannelException(String subchannel) {
        super("Unknown subchannel: " + subchannel);
    }

    public UnknownSubchannelException(String subchannel, String message) {
        super("Unknown subchannel: " + subchannel + " (" + message + ")");
    }
}
