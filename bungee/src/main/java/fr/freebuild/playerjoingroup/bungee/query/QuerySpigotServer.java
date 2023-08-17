package fr.freebuild.playerjoingroup.bungee.query;

import fr.freebuild.playerjoingroup.bungee.MessagesManager;
import fr.freebuild.playerjoingroup.core.protocol.Packet;

import java.util.concurrent.Callable;

public class QuerySpigotServer<T> implements Callable<T>, Observer<T> {

    private boolean idle;
    private String target;
    private MessagesManager messagesManager; // TODO refactor, doesn't seems right
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
    public T call() throws InterruptedException {
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

    public void sendQuery() {
        this.messagesManager.sendToOne(this.target, this.request);
    }

    public void setRequest(Packet request) {
        this.request = request;
    }
}
