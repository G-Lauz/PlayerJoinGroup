package fr.freebuild.playerjoingroup.bungee;

import fr.freebuild.playerjoingroup.core.Connection;
import fr.freebuild.playerjoingroup.core.MessageConsumer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ConnectionToClient implements Connection {

    private Socket socket;
    private MessageConsumer messageConsumer;

    private AtomicBoolean isConnected;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Queue<byte[]> messages;

    private Thread producer;
    private Thread consumer;

    private String name;

    private final Logger logger;

    public ConnectionToClient(Socket socket, MessageConsumer messageConsumer, Logger logger) {
        if (socket.isClosed())
            throw new IllegalStateException("Socket for client " + socket + " is closed.");

        this.socket = socket;
        this.messageConsumer = messageConsumer;
        this.logger = logger;

        this.isConnected = new AtomicBoolean(false);

        this.messages = new LinkedList<>();

        this.name = this.socket.getInetAddress().getHostAddress(); // Temporary name

        this.consumer = new Thread("playerjoingroup.bungee.connectiontoclient.consumer." + this.name) {
            @Override
            public void run() {
                consumeMessage();
            }
        };
        this.consumer.setDaemon(true);
        this.consumer.start();

        this.producer = new Thread("playerjoingroup.bungee.connectiontoclient.socket." + this.name) {
            @Override
            public void run() {
                establishConnection();

                if (isConnected.get())
                    queueIncomingMessage();
            }
        };
        this.producer.setDaemon(true);
        this.producer.start();
    }

    private void establishConnection() {
        try {
            this.dataInputStream = new DataInputStream(this.socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
            this.isConnected.set(true);

            this.logger.info("Connected to " + this.name + ". Waiting for handshake.");
        } catch (IOException exception) {
            this.logger.severe("Unable to establish connection with " + this.socket.getInetAddress().getHostAddress());
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

                    this.logger.info("[" + Thread.currentThread().getName() + "] Received message " + new String(msg) + " from " + this.socket.getInetAddress().getHostAddress());

                    synchronized (this.messages) {
                        this.logger.info("[" + Thread.currentThread().getName() + "] Queuing message " + new String(msg) + " from " + this.socket.getInetAddress().getHostAddress());
                        this.messages.add(msg);
                        this.logger.info("[" + Thread.currentThread().getName() + "] Notifying consumer for message " + new String(msg) + " from " + this.socket.getInetAddress().getHostAddress());
                        this.messages.notifyAll();
                        this.logger.info("[" + Thread.currentThread().getName() + "] Notified consumer for message " + new String(msg) + " from " + this.socket.getInetAddress().getHostAddress());
                    }
                }
            } catch (EOFException endOfFileException) {
                try {
                    this.close();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            } catch (IOException exception) {
                this.logger.severe("An error occured while reading the message from " + this.socket.getInetAddress().getHostAddress());
                this.logger.severe(exception.getMessage());
            }
        }
    }

    private void consumeMessage() {
        this.logger.info("[" + Thread.currentThread().getName() + "] Consumer started for " + this.socket.getInetAddress().getHostAddress());
        while (!Thread.currentThread().isInterrupted() && this.isConnected.get()) {
//            this.logger.info("[" + Thread.currentThread().getName() + "] Waiting (FIRST) for message from " + this.socket.getInetAddress().getHostAddress());
            byte[] msg = null;
            synchronized (this.messages) {
                try {
                    msg = messages.remove();
                    this.logger.info("[" + Thread.currentThread().getName() + "] Received message " + new String(msg) + " from " + this.socket.getInetAddress().getHostAddress());
                } catch (NoSuchElementException emptyQueue) {
                    try {
                        this.logger.info("[" + Thread.currentThread().getName() + "] Waiting for message from " + this.socket.getInetAddress().getHostAddress());
                        this.messages.wait();
                        this.logger.info("[" + Thread.currentThread().getName() + "] Stop waiting for message from " + this.socket.getInetAddress().getHostAddress());
                    } catch (InterruptedException e) {
                        this.logger.info("[" + Thread.currentThread().getName() + "] Interrupted while waiting for message from " + this.socket.getInetAddress().getHostAddress());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (msg != null) {
                this.logger.info("[" + Thread.currentThread().getName() + "] Processing message " + new String(msg) + " from " + this.socket.getInetAddress().getHostAddress());
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
            throw new IOException("Connection to " + this.socket.getInetAddress().getHostAddress() + " is not established.");
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
    public synchronized void setName(String name) {
        this.name = name;

        this.producer.setName("playerjoingroup.bungee.connectiontoclient." + this.name + ".socket");
        this.consumer.setName("playerjoingroup.bungee.connectiontoclient." + this.name + ".consumer");
    }

    @Override
    public synchronized String getName() {
        return this.name;
    }
}
