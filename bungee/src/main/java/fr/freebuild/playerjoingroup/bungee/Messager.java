package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.protocol.*;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;

public class Messager {

    private final PlayerJoinGroup plugin;

    public Messager(PlayerJoinGroup plugin) {
        this.plugin = plugin;
    }

    public void broadcast(Packet packet) {
        String group = packet.getParams().get(ParamsKey.SERVER_GROUP.getValue());

        this.plugin.getConfig().getGroup().forEach((serverGroup, servers) -> {
            if (serverGroup.equals(group) || group.equals("ALL")) {
                ((ArrayList) servers).forEach(server -> {
                    send((String)server, packet);
                });
            }
        });
    }

    public void send(String serverName, Packet packet) {
        try {
            Collection<ProxiedPlayer> networkPlayers = this.plugin.getProxy().getPlayers();
            if (networkPlayers == null || networkPlayers.isEmpty())
                return;

            ServerInfo serverInfo = this.plugin.getProxy().getServerInfo(serverName);
            if (serverInfo == null) {
                plugin.getLogger().warning("Unable to find server \"" + serverName + "\", ignoring it.");
                return;
            }

            Collection<ProxiedPlayer> serverPlayers = serverInfo.getPlayers();
            if (serverPlayers == null || serverPlayers.isEmpty())
                return;

            serverInfo.sendData(this.plugin.getConfig().getChannel(), Protocol.constructPacket(packet));
        } catch (InvalidPacketException ipe) {
            ipe.printStackTrace();
        } catch (ConstructPacketErrorException cpee) {
            cpee.printStackTrace();
        }
    }
}
