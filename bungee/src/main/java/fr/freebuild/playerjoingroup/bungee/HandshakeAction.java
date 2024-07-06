package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.action.Action;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;

public class HandshakeAction extends Action<String> {
    private final MessagesManager messageManager;
    private final String hostAddress;

    public HandshakeAction(MessagesManager messageManager, String hostAddress, long timeout) {
        super(timeout);
        this.messageManager = messageManager;
        this.hostAddress = hostAddress;
    }

    @Override
    public void execute(String serverName) {
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
