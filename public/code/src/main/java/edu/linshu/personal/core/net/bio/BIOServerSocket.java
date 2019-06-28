package edu.linshu.personal.core.net.bio;

import edu.linshu.personal.core.net.IClientSocket;
import edu.linshu.personal.core.net.IServerSocket;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class BIOServerSocket implements IServerSocket {

    private final ServerSocket serverSocket;

    public BIOServerSocket() {
        try {
            this.serverSocket = new ServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bind(String host, Integer port) throws IOException {
        if (serverSocket.isClosed()) {
            throw new IllegalStateException();
        }

        serverSocket.bind(new InetSocketAddress(host, port));
    }

    @Override
    public IClientSocket accept() throws IOException {
        if (serverSocket.isClosed()) {
            throw new IllegalStateException();
        }

        Socket clientSocket = serverSocket.accept();

        return new BIOSocket(clientSocket);
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return serverSocket.getLocalSocketAddress();
    }

    @Override
    public void close() throws IOException {
        if (serverSocket.isClosed()) {
            throw new IllegalStateException();
        }

        serverSocket.close();
    }

    @Override
    public boolean isClosed() {
        return serverSocket.isClosed();
    }

    @Override
    public void setSoTimeout(long time, TimeUnit unit) throws SocketException {
        serverSocket.setSoTimeout((int) unit.toMillis(time));
    }
}
