package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.Connection;
import fr.freebuild.playerjoingroup.core.MessageConsumer;
import fr.freebuild.playerjoingroup.spigot.event.SocketConnectedEvent;
import org.bukkit.Bukkit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Bukkit.getLogger;

public class ConnectionToServer implements Connection {
    private RetryPolicy retryPolicy;
    private Thread producer;
    private Thread consumer;
    private AtomicBoolean isConnected;
    private String serverName;
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Queue<byte[]> messages;
    private MessageConsumer messageConsumer;

    private String name;

    public ConnectionToServer(String serverName, Socket socket, MessageConsumer messageConsumer, RetryPolicy retryPolicy) {
        this.serverName = serverName;
        this.socket = socket;
        this.retryPolicy = retryPolicy;

        this.isConnected = new AtomicBoolean(false);

        this.messages = new LinkedList<>();
        this.messageConsumer = messageConsumer;

        this.producer = new Thread("playerjoingroup.spigot.messagemanager." + this.serverName + ".socket") {
            @Override
            public void run() {
                establishConnection();
            }
        };
        this.producer.setDaemon(true);
        this.producer.start();

        this.consumer = new Thread("playerjoingroup.spigot.connectiontoserver." + this.serverName + ".consumer") {
            @Override
            public void run() {
                consumeMessage();
            }
        };
        this.consumer.setDaemon(true);
        this.consumer.start();
    }

    private void establishConnection() {
        while(!Thread.currentThread().isInterrupted()) {
            int attempts = 0;
            while(!isConnected.get() && retryPolicy.shouldRetry(attempts)) {
                try {
                    if (!socket.isConnected() || socket.isClosed())
                        socket = new Socket(socket.getInetAddress(), socket.getPort());

                    if (socket.isConnected() && !socket.isClosed()) {
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        isConnected.set(true);

                        Bukkit.getPluginManager().callEvent(new SocketConnectedEvent(serverName));

                        break;
                    }
                } catch (IOException exception) {
                    long delay = retryPolicy.getDelay(attempts);

                    getLogger().warning(exception.getMessage());
                    getLogger().warning("Connection to the proxy server failed. Did you specified the correct IP and port?");
                    getLogger().warning("Will attempt to reconnect in " + delay + " ms...");

                    attempts++;
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (!isConnected.get()) {
                getLogger().warning("Unable to connect to the proxy server. The group feature will not work. And each new connection will be handle locally.");
                break;
            }

            queueIncomingMessage();

            if (!isConnected.get()) {
                getLogger().warning("Connection to the proxy server is closed. Will attempt to reconnect...");
            }
        }
    }

    private void queueIncomingMessage() {
        while (!Thread.currentThread().isInterrupted() && isConnected.get()) {
            try {
                int length = dataInputStream.readInt();
                if (length > 0) {
                    byte[] msg = new byte[length];
                    dataInputStream.read(msg, 0, msg.length);

                    synchronized (messages) {
                        messages.add(msg);
                        messages.notifyAll();
                    }
                }
            } catch (EOFException endOfFileException) {
                // Close the connection
                try {
                    this.dataInputStream.close();
                    this.dataOutputStream.close();
                    this.socket.close();

                    isConnected.set(false);
                } catch (IOException error) {
                    throw new RuntimeException(error);
                }

            } catch (IOException error) {
                getLogger().warning("An error occurred while reading the message from the proxy server: " + error.getMessage());
            }
        }
    }

    private void consumeMessage() {
        while(!Thread.currentThread().isInterrupted() && this.isConnected.get()) {
            byte[] msg = null;
            synchronized (messages) {
                try {
                    msg = messages.remove();
                } catch (NoSuchElementException emptyQueue) { // Queue is empty
                    try {
                        messages.wait();
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (msg != null) {
                this.messageConsumer.processMessage(this, msg);
            }
        }
    }

    @Override
    public void sendMessage(byte[] msg) throws IOException {
        if (this.isConnected.get()) {
            synchronized (this.dataOutputStream) {
                this.dataOutputStream.writeInt(msg.length);
                this.dataOutputStream.write(msg);
                this.dataOutputStream.flush();
            }
        } else {
            throw new IOException("Connection to the proxy server is not established.");
        }
    }

    @Override
    public void close() throws IOException, InterruptedException {
        this.isConnected.set(false);

        // Close the producer thread
        this.dataInputStream.close();
        this.dataOutputStream.close();
        this.socket.close();
        this.producer.join();

        // Close the consumer thread
        if (this.consumer != null && this.consumer.isAlive()) {
            this.consumer.interrupt();
            this.consumer.join();
        }
    }

    @Override
    public boolean isConnected() {
        return this.isConnected.get();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
