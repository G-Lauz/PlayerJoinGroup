package fr.freebuild.playerjoingroup.spigot.actions;

import fr.freebuild.playerjoingroup.core.Action;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;
import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class DisconnectAction extends Action<Void> {
    private final PlayerJoinGroup plugin;
    private final String serverName;
    private final String playerName;
    private final UUID playerUUID;
    private final String event;

    public DisconnectAction(PlayerJoinGroup plugin, String serverName, String playerName, UUID playerUUID, String event, int timeout) {
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
                .setEventType(EventType.SERVER_DISCONNECT)
                .setData(this.playerUUID.toString())
                .setPlayerUuid(this.playerUUID)
                .appendParam("SERVER_NAME", this.serverName)
                .appendParam("PLAYER_NAME", this.playerName)
                .build();

        try {
            this.plugin.getMessageManager().send(Protocol.constructPacket(eventPacket));
        } catch (IOException | InvalidPacketException | ConstructPacketErrorException e) {
            this.plugin.getLogger().severe("An error occurred while sending packet to bungeecord");
            for (StackTraceElement element : e.getStackTrace()) {
                this.plugin.getLogger().severe(element.toString());
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serverName, this.playerName, this.playerUUID, this.event);
    }
}
