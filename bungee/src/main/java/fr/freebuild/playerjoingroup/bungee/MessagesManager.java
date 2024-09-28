package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.Connection;
import fr.freebuild.playerjoingroup.core.MessageConsumer;
import fr.freebuild.playerjoingroup.core.log.DebugLogger;
import fr.freebuild.playerjoingroup.core.protocol.*;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class MessagesManager {

    private final PlayerJoinGroup plugin;
    private ServerSocket serverSocket;
    private Hashtable<String, Connection> clientsRegistry;
    private ScheduledTask acceptor;
    private MessageConsumer messageConsumer;
    private DebugLogger logger;

    public MessagesManager(PlayerJoinGroup plugin, int port, DebugLogger logger) throws IOException {
        this.plugin = plugin;

        this.messageConsumer = new ServerMessageConsumer(plugin, this, logger);

        this.clientsRegistry = new Hashtable<>();
        this.serverSocket = new ServerSocket(port);

        this.logger = logger;

        this.acceptor = this.plugin.getProxy().getScheduler().runAsync(this.plugin, this::acceptClient);
    }

    private void acceptClient() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                ConnectionToClient client = new ConnectionToClient(this.plugin, socket, this.messageConsumer, this.logger);

                synchronized (this.clientsRegistry) {
                    this.clientsRegistry.put(socket.getInetAddress().getHostAddress(), client); // TODO proper handshake
                }

            } catch (IOException exception) {
                this.logger.severe("Unable to accept client: " + exception.getMessage());
                throw new RuntimeException(exception);
            }
        }
    }

    public void updateClientName(String oldName, String newName) {
        synchronized (this.clientsRegistry) {
            this.logger.debug("Updating client name from \"" + oldName + "\" to \"" + newName + "\".");
            Connection connection = this.clientsRegistry.remove(oldName);
            connection.setName(newName);
            this.clientsRegistry.put(newName, connection);
        }
    }

    public void sendToOne(String server, Packet packet) {

        ServerInfo serverInfo = this.plugin.getProxy().getServerInfo(server);
        if (serverInfo == null) {
            this.logger.warning("Unable to find server \"" + server + "\", ignoring it.");
            return;
        }

        boolean clientIsRegistered = false;
        synchronized (this.clientsRegistry) {
            clientIsRegistered = clientsRegistry.containsKey(server);
        }

        if (!clientIsRegistered) {
            this.logger.warning(server + "\" did not handshake, ignoring it.");
            return;
        }

        Connection client = null;
        synchronized (this.clientsRegistry) {
            client = clientsRegistry.get(server);
        }

        if (client == null) {
            this.logger.warning("Client \"" + server + "\" is null, ignoring it.");
            return;
        }

        try {
            client.sendMessage(Protocol.constructPacket(packet));
        } catch (IOException e) {
            if (e.getMessage().contains("Broken pipe")) {
                this.logger.warning("Broken pipe: Unable to send packet to \"" + server + "\", removing it from the list.");

                Connection connection = null;
                synchronized (this.clientsRegistry) {
                    connection = clientsRegistry.remove(server);
                }

                try {
                    connection.close();
                } catch (IOException | InterruptedException err) {
                    this.logger.severe(Arrays.toString(err.getStackTrace()));
                    throw new RuntimeException(err);
                }
            }
        } catch (InvalidPacketException | ConstructPacketErrorException err) { // TODO better exception handling
            this.logger.severe(Arrays.toString(err.getStackTrace()));
            throw new RuntimeException(err);
        }
    }

    public void sendToAll(Packet packet) {
        String group = packet.getField(ParamsKey.SERVER_GROUP);

        this.plugin.getConfig().getGroup().forEach((serverGroup, servers) -> {
            if (serverGroup.equals(group) || group.equals("ALL")) {
                ((ArrayList) servers).forEach(server -> {
                    sendToOne((String)server, packet);
                });
            }
        });
    }

    public void close() throws InterruptedException{
        this.acceptor.cancel();

        // Close all connections and remove them from the registry
        synchronized (this.clientsRegistry) {
            clientsRegistry.forEach((name, connection) -> {
                try {
                    connection.close();
                } catch (IOException | InterruptedException exception) {
                    this.logger.severe(Arrays.toString(exception.getStackTrace()));
                    throw new RuntimeException(exception);
                }
            });

            clientsRegistry.clear();
        }
    }
}
