package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.Connection;
import fr.freebuild.playerjoingroup.core.MessageConsumer;
import fr.freebuild.playerjoingroup.core.action.ActionExecutor;
import fr.freebuild.playerjoingroup.core.log.DebugLogger;
import fr.freebuild.playerjoingroup.core.protocol.*;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionToClient implements Connection {

    private final PlayerJoinGroup plugin;
    private Socket socket;
    private MessageConsumer messageConsumer;

    private AtomicBoolean isConnected;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Queue<byte[]> messages;

    private ScheduledTask producer;
    private ScheduledTask consumer;

    private String name;

    private final CountDownLatch threadsStartedLatch;
    private final ActionExecutor actionExecutor;

    private final DebugLogger logger;

    public ConnectionToClient(PlayerJoinGroup plugin, Socket socket, MessageConsumer messageConsumer, DebugLogger logger) {
        if (socket.isClosed())
            throw new IllegalStateException("Socket for client " + socket + " is closed.");

        this.plugin = plugin;

        this.socket = socket;
        this.messageConsumer = messageConsumer;
        this.logger = logger;

        this.isConnected = new AtomicBoolean(false);

        this.messages = new LinkedList<>();

        this.name = this.socket.getInetAddress().getHostAddress(); // Temporary name

        this.threadsStartedLatch = new CountDownLatch(2);
        this.actionExecutor = new ActionExecutor(logger);

        this.consumer = this.plugin.getProxy().getScheduler().runAsync(this.plugin, () -> {
            threadsStartedLatch.countDown();
            consumeMessage();
            logger.debug("Closing consumer thread for " + name);
        });

        this.producer = this.plugin.getProxy().getScheduler().runAsync(this.plugin, () -> {
            establishConnection();
            threadsStartedLatch.countDown();
            if (isConnected.get())
                queueIncomingMessage();

            logger.debug("Closing producer thread for " + name);
        });

        this.plugin.getProxy().getScheduler().runAsync(this.plugin, () -> {
            try {
                logger.debug("Waiting for threads to start for " + name);
                threadsStartedLatch.await();
                logger.debug("Threads started for " + name + ". Initiating handshake.");
                initiateHandshake(ConnectionToClient.this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void initiateHandshake(Connection connection) {
        Packet packet = new Packet.Builder(Subchannel.HANDSHAKE).build();

        try {
            connection.sendMessage(Protocol.constructPacket(packet));

            HandshakeAction handshakeAction = new HandshakeAction(this.plugin.getMessagesManager(), connection.getName(), 1000, this.logger);
            this.actionExecutor.resolve(handshakeAction, null);
        } catch (IOException | ConstructPacketErrorException | InvalidPacketException err) {
            this.logger.severe(Arrays.toString(err.getStackTrace()));
            throw new RuntimeException(err);
        }
    }

    private void establishConnection() {
        try {
            this.dataInputStream = new DataInputStream(this.socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
            this.isConnected.set(true);

            this.logger.debug("Connected to " + this.name + ". Waiting for handshake.");
        } catch (IOException exception) {
            this.logger.severe("Unable to establish connection with " + this.name);
            this.logger.severe(exception.getMessage());
        }
    }

    private void queueIncomingMessage() {
        while (!Thread.currentThread().isInterrupted() && this.isConnected.get()) {
            try {
                int length = this.dataInputStream.readInt();
                if (length > 0) {
                    byte[] msg = new byte[length];
                    dataInputStream.read(msg, 0, length);

                    this.logger.debug("Message received from " + this.name);

                    synchronized (this.messages) {
                        this.logger.debug("Message queued from " + this.name);
                        this.messages.add(msg);
                        this.logger.debug("Notifying consumer for message from " + this.name);
                        this.messages.notifyAll();
                        this.logger.debug("Notified consumer for message from " + this.name);
                    }
                }
            } catch (EOFException | SocketException endOfFileException) {
                try {
                    this.logger.debug("Connection closed by " + this.name);
                    this.close();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            } catch (IOException exception) {
                this.logger.severe("An error occured while reading the message from " + this.name);
                this.logger.severe(exception.getMessage());

                try {
                    this.close();
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
        }
    }

    private void consumeMessage() {
        try {
            this.threadsStartedLatch.await();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        this.logger.debug("Consumer started for " + this.name);
        while (!Thread.currentThread().isInterrupted() && this.isConnected.get()) {
            byte[] msg = null;
            synchronized (this.messages) {
                try {
                    msg = messages.remove();
                    this.logger.debug("(Consumer) Message received from " + this.name);
                } catch (NoSuchElementException emptyQueue) {
                    try {
                        this.logger.debug("Waiting for message from " + this.name);
                        this.messages.wait();
                        this.logger.debug("Notified for message from " + this.name + ", stopping waiting.");
                    } catch (InterruptedException e) {
                        this.logger.debug("Interrupted while waiting for message from " + this.name + ".");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (msg != null) {
                this.logger.debug("Processing message from " + this.name);
                this.messageConsumer.processMessage(this, msg);
            }
        }
    }

    @Override
    public void sendMessage(byte[] message) throws IOException {
        if (this.isConnected.get()) {
            synchronized (this.dataOutputStream) {
                this.dataOutputStream.writeInt(message.length);
                this.dataOutputStream.write(message);
                this.dataOutputStream.flush();
            }
        } else {
            throw new IOException("Connection to " + this.name + " is not established.");
        }
    }

    @Override
    public void close() throws IOException {
        this.isConnected.set(false);

        // Close the producer thread
        this.dataInputStream.close();
        this.dataOutputStream.close();
        this.socket.close();
        this.producer.cancel();
        this.consumer.cancel();
    }

    @Override
    public boolean isConnected() {
        return this.isConnected.get();
    }

    @Override
    public synchronized void setName(String name) {
        this.name = name;
    }

    @Override
    public synchronized String getName() {
        return this.name;
    }

    public ActionExecutor getActionExecutor() {
        return this.actionExecutor;
    }
}
