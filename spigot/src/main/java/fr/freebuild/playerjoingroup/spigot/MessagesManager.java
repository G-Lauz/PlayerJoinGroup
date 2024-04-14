package fr.freebuild.playerjoingroup.spigot;

import java.io.IOException;

public class MessagesManager {
    private final ActionExecutor actionExecutor;
    private final ConnectionToServer server;

    public MessagesManager(ConnectionToServer server, ActionExecutor actionExecutor) {
        this.server = server;
        this.actionExecutor = actionExecutor;
    }

    public void send(byte[] msg) throws IOException {
        this.server.sendMessage(msg);
    }

    public void close() {
        try {
            this.server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEnable() {
        return this.server.isConnected();
    }

    public ActionExecutor getActionExecutor() {
        return this.actionExecutor;
    }
}
