package fr.freebuild.playerjoingroup.bungee.actions;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.core.Action;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;

import java.util.Objects;
import java.util.UUID;

public class DisconnectAction extends Action<Void> {
    private final PlayerJoinGroup plugin;
    private final String serverName;
    private final String playerName;
    private final UUID playerUUID;
    private final String event;

    public DisconnectAction(PlayerJoinGroup plugin, String serverName, String playerName, UUID playerUUID, String event, long timeout) {
        super(timeout);
        this.plugin = plugin;
        this.serverName = serverName;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.event = event;
    }

    @Override
    public void execute(Void context) {
        Packet eventPacket = new Packet.Builder(Subchannel.EVENT)
                .setEventType(EventType.GROUP_DECONNECTION)
                .setData(this.playerUUID.toString())
                .appendParam("PLAYER_NAME", this.playerName)
                .setPlayerUuid(this.playerUUID)
                .setServerGroup(this.serverName)
                .build();

        this.plugin.getMessagesManager().sendToAll(eventPacket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serverName, this.playerName, this.playerUUID, this.event);
    }
}
