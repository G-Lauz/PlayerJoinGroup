package fr.freebuild.playerjoingroup.core;

import java.io.IOException;

public interface Connection {
    public void sendMessage(byte[] message) throws IOException;
    public void close() throws IOException, InterruptedException;
    public boolean isConnected();
    public void setName(String name);
    public String getName();
}
