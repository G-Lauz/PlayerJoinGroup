package fr.freebuild.playerjoingroup.core.protocol;

public class UnknownSubchannelException extends Exception{
    public UnknownSubchannelException(String subchannel) {
        super("Unknown subchannel: " + subchannel);
    }
}
