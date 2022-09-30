package fr.freebuild.playerjoingroup.bungee.query;

import fr.freebuild.playerjoingroup.bungee.MessagesManager;
import fr.freebuild.playerjoingroup.bungee.PlayerJoinGroup;
import fr.freebuild.playerjoingroup.core.protocol.ConstructPacketErrorException;
import fr.freebuild.playerjoingroup.core.protocol.InvalidPacketException;
import fr.freebuild.playerjoingroup.core.protocol.Packet;
import fr.freebuild.playerjoingroup.core.protocol.Protocol;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

public class QuerySpigotServer<T> implements Callable<T>, Observer<T> {

    private boolean idle;
//    private ServerInfo target;
    private String target;
    private MessagesManager messagesManager;
    private Packet request;
    private T response;

    public QuerySpigotServer(String target, MessagesManager messagesManager) {
        this.idle = true;
        this.target = target;
        this.messagesManager = messagesManager;
        this.request = null;
        this.response = null;
    }

    public QuerySpigotServer(String target, Packet packet, MessagesManager messagesManager) {
        this.idle = true;
        this.target = target;
        this.messagesManager = messagesManager;
        this.request = packet;
        this.response = null;
    }

    @Override
    public T call() throws InterruptedException, InvalidPacketException, ConstructPacketErrorException, IOException {
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
    public void update(T obj) {
        synchronized (this) {
            this.idle = false;
            this.response = obj;
            this.notify();
        }
    }

    public void sendQuery() throws InvalidPacketException, ConstructPacketErrorException, IOException {
//        Collection<ProxiedPlayer> networkPlayers = PlayerJoinGroup.plugin.getProxy().getPlayers();
//        if (networkPlayers == null || networkPlayers.isEmpty())
//            return;

//        Collection<ProxiedPlayer> serverPlayers = this.target.getPlayers();
//        if (serverPlayers == null || serverPlayers.isEmpty())
//            return;
//
//        this.target.sendData(PlayerJoinGroup.plugin.getConfig().getChannel(), Protocol.constructPacket(this.request));
        this.messagesManager.sendToOne(this.target, this.request);
    }

    public void setRequest(Packet request) {
        this.request = request;
    }
}
