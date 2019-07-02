package edu.linshu.personal.core.net.jdk.bio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import lombok.extern.java.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/21 14:19
 */
@Log
public class BIOSocket implements IClientSocket {
    private Socket socket;
    private long connectionTimeout;

    public BIOSocket() {
        socket = new Socket();
    }

    public BIOSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public boolean connect(String host, Integer port) throws IOException {
        if (isClosed()) {
            throw new IllegalStateException("socket is already closed");
        }

        if (socket.isConnected()) {
            return true;
        }

        long startTime = System.nanoTime();
        long connectionTime = 0L;
        while (!socket.isConnected() && connectionTime <= connectionTimeout) {
            try {
                socket.connect(new InetSocketAddress(host, port));
            } catch (IOException e) {
                socket = new Socket();
                log.info("[" + System.nanoTime() + "]: 连接失败");
            } finally {
                connectionTime = System.nanoTime() - startTime;
            }
        }

        if (!socket.isConnected()) {
            throw new IOException("连接超时");
        }

        return true;
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
    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
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
        return socket.isClosed();
    }

    @Override
    public void sendMsg(String msg, Charset charset) throws IOException {
        if (socket.isOutputShutdown()) {
            throw new IllegalStateException("socket output stream closed");
        }

        OutputStream out = socket.getOutputStream();
        out.write(msg.getBytes(charset));
        out.flush();
    }

    @Override
    public String readMsg(Charset charset) throws IOException {
        if (socket.isInputShutdown()) {
            throw new IllegalStateException("socket input stream closed");
        }

        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

        byte[] buffer = new byte[4086 << 2];
        int readCount = in.read(buffer, 0, buffer.length);

        if (readCount == -1) {
            return EMPTY_STRING;
        }

        return new String(buffer, 0, readCount, charset);
    }

    @Override
    public void setSoTimeout(long time, TimeUnit unit) throws SocketException {
        socket.setSoTimeout((int) unit.toMillis(time));
    }

    public Socket getSocket() {
        return socket;
    }
}
