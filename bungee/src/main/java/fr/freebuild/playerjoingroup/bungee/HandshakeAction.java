package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.action.Action;
import fr.freebuild.playerjoingroup.core.log.DebugLogger;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;

public class HandshakeAction extends Action<String> {
    private final MessagesManager messageManager;
    private final String hostAddress;

    private DebugLogger logger;

    public HandshakeAction(MessagesManager messageManager, String hostAddress, long timeout, DebugLogger logger) {
        super(timeout);
        this.messageManager = messageManager;
        this.hostAddress = hostAddress;
        this.logger = logger;
    }

    @Override
    public void execute(String serverName) {
        this.logger.info(serverName + " has connected to the proxy.");

        this.messageManager.updateClientName(this.hostAddress, serverName);

        Packet ack = new Packet.Builder(Subchannel.HANDSHAKE_ACK)
                .setData(serverName)
                .build();

        this.messageManager.sendToOne(serverName, ack);
    }

    @Override
    public int hashCode() {
        return this.hostAddress.hashCode();
    }
}
