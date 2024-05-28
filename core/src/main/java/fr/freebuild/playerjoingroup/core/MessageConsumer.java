package fr.freebuild.playerjoingroup.core;

public interface MessageConsumer {
    void processMessage(Connection connection, byte[] message);
}
