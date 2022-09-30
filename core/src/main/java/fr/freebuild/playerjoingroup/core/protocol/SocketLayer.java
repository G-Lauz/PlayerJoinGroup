package fr.freebuild.playerjoingroup.core.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketLayer extends Layer {

    private ServerSocket serverSocket;
    private byte[] buff = new byte[256];

    public SocketLayer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void handleIncomingRequest(int data) {

        new Thread("protocol.SocketLayer") {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) { // TODO
//                    worker();
                }
            }
        }.start();
    }

    private void worker() throws IOException {
        Socket socket = null;
        try {
            socket = serverSocket.accept();

            DataInputStream din = new DataInputStream(socket.getInputStream());
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            new Thread("handler." + socket.toString()) {
                @Override
                public void run() {
//                    while(socket.isBound())
                }
            }.start();
        } catch (Exception e) {
            // TODO
            socket.close();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleOutgoingRequest(int data) {

    }
}
