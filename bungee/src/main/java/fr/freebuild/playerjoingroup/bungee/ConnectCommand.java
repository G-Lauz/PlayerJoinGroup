package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.Command;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Subchannel;

import java.util.Objects;
import java.util.UUID;

public class ConnectCommand extends Command<Boolean> {
    private final PlayerJoinGroup plugin;
    private final String serverName;
    private final String playerName;
    private final UUID playerUUID;
    private final String event;

    public ConnectCommand(PlayerJoinGroup plugin, String serverName, String playerName, UUID playerUUID, String event, long timeout) {
        super(timeout);
        this.plugin = plugin;
        this.serverName = serverName;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.event = event;
    }

    @Override
    public void execute(Boolean hasPlayedBefore) {
        EventType eventType = hasPlayedBefore ? EventType.HAS_PLAYED_BEFORE : EventType.FIRST_GROUP_CONNECTION;

        Packet eventPacket = new Packet.Builder(Subchannel.EVENT)
                .setData(this.playerName)
                .setPlayerUuid(this.playerUUID)
                .setEventType(eventType)
                .setServerGroup(this.serverName)
                .build();

        this.plugin.getMessagesManager().sendToAll(eventPacket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serverName, this.playerName, this.playerUUID, this.event);
    }
}
