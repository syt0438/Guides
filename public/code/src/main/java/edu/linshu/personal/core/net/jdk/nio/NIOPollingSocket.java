package edu.linshu.personal.core.net.jdk.nio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import lombok.extern.java.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 13:35
 */
@Log
public class NIOPollingSocket implements IClientSocket, ISelector {
    private SocketChannel socket;

    private long connectionTimeout;

    public NIOPollingSocket() throws IOException {
        socket = createSocket();
    }

    public NIOPollingSocket(SocketChannel client) throws IOException {
        socket = client;
        client.configureBlocking(false);
    }

    private SocketChannel createSocket() throws IOException {
        SocketChannel socket = SocketChannel.open();
        socket.configureBlocking(false);

        return socket;
    }

    @Override
    public boolean connect(String ip, Integer port) throws IOException {
        if (isClosed()) {
            throw new IllegalStateException("socket is already closed");
        }

        if (socket.isConnected()) {
            return true;
        }

        if (socket.isConnectionPending()) {
            return false;
        }

        long startTime = System.nanoTime();
        long connectionTime = 0L;
        boolean connected = false;

        while (!connected && connectionTime <= connectionTimeout) {
            try {
                connected = socket.connect(new InetSocketAddress(ip, port));

                while (!connected) {
                    Thread.yield();

                    connected = socket.finishConnect();
                }

            } catch (Exception e) {
                socket = createSocket();
                log.info("[" + System.nanoTime() + "]: 连接失败");
            } finally {
                connectionTime = System.nanoTime() - startTime;
            }
        }

        if (connected) {
            return true;
        }

        throw new IOException("连接超时");
    }

    @Override
    public boolean finishConnection() {
        return socket.isConnected();
    }

    @Override
    public void setConnectionTimeout(long time, TimeUnit unit) {
        if (socket.isConnected()) {
            return;
        }

        this.connectionTimeout = unit.toNanos(time);
    }

    @Override
    public SocketAddress getRemoteSocketAddress() throws IOException {
        return socket.getRemoteAddress();
    }

    /**
     * 写入数据的时候，需要在循环中进行，因为非阻塞状态下调用 write，可能还没有写入
     * 任何数据时，write 方法就已经返回
     */
    @Override
    public void sendMsg(String msg, Charset charset) throws IOException {
        if (isClosed()) {
            throw new IllegalStateException("socket is already closed");
        }

        if (!socket.isConnected()) {
            throw new IllegalStateException("socket is not connected ");
        }

        byte[] msgBytes = msg.getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(msgBytes, 0, msgBytes.length);

        while (buffer.hasRemaining()) {
            socket.write(buffer);
        }
    }

    /**
     * read 方法，如果返回 -1，表示已经读到了流的末尾（即：连接已关闭）
     */
    @Override
    public String readMsg(Charset charset) throws IOException {
        if (isClosed()) {
            throw new IllegalStateException("socket is already closed");
        }

        if (!socket.isConnected()) {
            throw new IllegalStateException("socket is not connected ");
        }

        ByteBuffer buffer = ByteBuffer.allocate(4086 << 2);
        int readCount = socket.read(buffer);

        while (readCount <= 0) {
            if (readCount == -1) {
                return EMPTY_STRING;
            }

            readCount = socket.read(buffer);
        }

        buffer.flip();

        return new String(buffer.array(), buffer.arrayOffset(), buffer.limit(), charset);
    }

    @Override
    public void close() throws IOException {
        if (!socket.isConnected() || isClosed()) {
            return;
        }

        socket.close();
    }

    @Override
    public boolean isClosed() {
        return !socket.isOpen();
    }

    @Override
    public void setSoTimeout(long time, TimeUnit unit) throws SocketException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SelectionKey register(Selector selector, int ops, Object attachment) throws ClosedChannelException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        return socket.register(selector, ops, attachment);
    }

    @Override
    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        if (isClosed()) {
            throw new IllegalStateException();
        }

        return socket.register(selector, ops);
    }
}
