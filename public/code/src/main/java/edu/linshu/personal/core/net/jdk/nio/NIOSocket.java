package edu.linshu.personal.core.net.jdk.nio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ConnectException;
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
@Log4j2
public class NIOSocket implements IClientSocket, ISelector {
    private SocketChannel socket;

    private long connectionTimeout;

    public NIOSocket() throws IOException {
        socket = createSocket();
    }

    public NIOSocket(SocketChannel client) throws IOException {
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

        return socket.connect(new InetSocketAddress(ip, port));
    }

    @Override
    public boolean finishConnection() throws IOException {
        try {
            return socket.finishConnect();
        } catch (ConnectException e) {
            log.info("连接异常： " + e.getMessage());

            return false;
        }
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
        return socket.register(selector, ops, attachment);
    }

    @Override
    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        return socket.register(selector, ops);
    }
}
