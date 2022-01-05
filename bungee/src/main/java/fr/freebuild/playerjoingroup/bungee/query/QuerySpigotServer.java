package fr.freebuild.playerjoingroup.bungee.query;

import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.core.protocol.ConstructPacketErrorException;
import fr.freebuild.playerjoingroup.core.protocol.InvalidPacketException;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Protocol;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.concurrent.Callable;

public class QuerySpigotServer<T> implements Callable<T>, Observer<T> {

    private boolean idle;
    private ServerInfo target;
    private Packet request;
    private T response;

    public QuerySpigotServer(ServerInfo target, Packet packet) {
        this.idle = true;
        this.target = target;
        this.request = packet;
        this.response = null;
    }

    @Override
    public T call() throws InterruptedException, InvalidPacketException, ConstructPacketErrorException {
        this.idle = true;

        this.sendQuery();

        synchronized (this) {
            while(this.idle) {
                this.wait();
            }
        }
        return response;
    }

    @Override
    public void notify(T obj) {
        synchronized (this) {
            this.idle = false;
            this.response = obj;
            this.notify();
        }
    }

    public void sendQuery() throws InvalidPacketException, ConstructPacketErrorException {
        Collection<ProxiedPlayer> networkPlayers = PlayerJoinGroup.plugin.getProxy().getPlayers();
        if (networkPlayers == null || networkPlayers.isEmpty())
            return;

        Collection<ProxiedPlayer> serverPlayers = this.target.getPlayers();
        if (serverPlayers == null || serverPlayers.isEmpty())
            return;

        this.target.sendData(PlayerJoinGroup.plugin.getConfig().getChannel(), Protocol.constructPacket(this.request));
    }
}
