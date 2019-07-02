package edu.linshu.personal.core.net.jdk.nio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import edu.linshu.personal.core.net.jdk.IServerSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 19:15
 */
public class NIOServerSocket implements IServerSocket, ISelector {

    private final ServerSocketChannel serverSocket;

    public NIOServerSocket() {
        try {
            this.serverSocket = ServerSocketChannel.open();
            this.serverSocket.configureBlocking(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bind(String host, Integer port) throws IOException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        serverSocket.bind(new InetSocketAddress(host, port));
    }

    @Override
    public IClientSocket accept() throws IOException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        SocketChannel client = serverSocket.accept();

        if (Objects.isNull(client)) {
            return null;
        }

        return new NIOSocket(client);
    }

    @Override
    public SelectionKey register(Selector selector, int ops, Object attachment) throws ClosedChannelException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        return serverSocket.register(selector, ops, attachment);
    }

    @Override
    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        return serverSocket.register(selector, ops);
    }

    @Override
    public SocketAddress getLocalSocketAddress() throws IOException {
        return serverSocket.getLocalAddress();
    }

    @Override
    public void close() throws IOException {
        if (isClosed()) {
            return;
        }

        serverSocket.close();
    }

    @Override
    public boolean isClosed() {
        return !serverSocket.isOpen();
    }

    @Override
    public void setSoTimeout(long time, TimeUnit unit) throws SocketException {
        throw new UnsupportedOperationException();
    }
}
