package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.Command;
import fr.freebuild.playerjoingroup.core.event.EventType;
import fr.freebuild.playerjoingroup.core.protocol.*;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class DisconnectCommand extends Command<Void> {
    private final PlayerJoinGroup plugin;
    private final String serverName;
    private final String playerName;
    private final UUID playerUUID;
    private final String event;

    public DisconnectCommand(PlayerJoinGroup plugin, String serverName, String playerName, UUID playerUUID, String event, int timeout) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidPacketException e) {
            throw new RuntimeException(e);
        } catch (ConstructPacketErrorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serverName, this.playerName, this.playerUUID, this.event);
    }
}
