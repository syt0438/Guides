package com.study.test;

import lombok.extern.java.Log;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.Closeable;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 22:06
 */
@SuppressWarnings("all")
@Log
public class NIOClientAccessBaiduTest {
    @Test
    public void test() throws IOException, InterruptedException {
        sendRequest();
    }

    private static void sendRequest() throws IOException, InterruptedException {
        Selector selector = Selector.open();

        IClientSocket client = new NIOSocket();
        ISelector clientSelector = (ISelector) client;

        if (!client.connect("www.baidu.com", 80)) {
            clientSelector.register(selector, SelectionKey.OP_CONNECT);
        }

        while (!client.isClosed()) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isConnectable()) {
                    if (!client.finishConnection()) {
                        continue;
                    }

                    log.info("连接成功：" + client.getRemoteSocketAddress());

                    clientSelector.register(selector, SelectionKey.OP_WRITE);
                }

                if (key.isWritable()) {
                    client.sendMsg(
                            "GET / HTTP/1.1\r\n" +
                                    "Host: www.baidu.com\r\n" +
                                    "Connection: close\r\n" +
                                    "User-Agent: LinShu-Custom-Client\r\n" +
                                    "Accept: */*\r\n" +
                                    "Accept-Encoding: gzip, deflate\r\n" +
                                    "\r\n",
                            StandardCharsets.UTF_8);

                    client.register(selector, SelectionKey.OP_READ);
                }

                if (key.isReadable()) {
                    try {
                        String readMsg = client.readMsg(StandardCharsets.UTF_8);

                        handleTask(readMsg);

                    } catch (Exception e) {
                        log.severe("处理信息出错：" + client.getRemoteSocketAddress());
                    } finally {
                        key.cancel();
                        client.close();
                    }
                }
            }

            selector.selectNow();
        }
    }

    private static void handleTask(String readMsg) {
        if (StringUtils.isEmpty(readMsg)) {
            throw new IllegalStateException("未接收到数据信息");
        }

        int headerEndIndex = StringUtils.indexOf(readMsg, "\r\n\r\n");
        String header = StringUtils.substring(readMsg, 0, headerEndIndex);

        Optional<String> optional = Arrays.stream(header.split("\r\n"))
                .filter(headerLine -> StringUtils.startsWith(headerLine, "Server:"))
                .map(headerLine -> StringUtils.split(headerLine, ":"))
                .filter(ArrayUtils::isNotEmpty)
                .filter(headerLine -> headerLine.length == 2)
                .map(headerLine -> StringUtils.join(headerLine, ":"))
                .findFirst();

        optional.ifPresent(headerLine -> {
            log.info("我的QQ号：745698872，我的解析到百度服务器server类型是：" + headerLine);
        });
    }

    private static interface IClientSocket extends ISocket, ISelector {

        boolean connect(String ip, Integer port) throws IOException;

        /**
         * 如果 finishConnect() 返回 false, 则表示连接尚未完成
         * 如果 finishConnect() 抛出异常, 则表示连接失败
         */
        boolean finishConnection() throws IOException;

        void setConnectionTimeout(long time, TimeUnit unit);

        SocketAddress getRemoteSocketAddress() throws IOException;

        void sendMsg(String msg, Charset charset) throws IOException;

        String readMsg(Charset charset) throws IOException;
    }

    private static interface ISocket extends Closeable {
        String EMPTY_STRING = "";

        @Override
        void close() throws IOException;

        boolean isClosed();

        void setSoTimeout(long time, TimeUnit unit) throws SocketException;
    }

    private static interface ISelector {

        default SelectionKey register(Selector selector, int ops, Object attachment) throws ClosedChannelException {
            throw new UnsupportedOperationException();
        }

        default SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
            throw new UnsupportedOperationException();
        }
    }

    private static class NIOSocket implements IClientSocket, ISelector {
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

            ByteBuffer buffer = ByteBuffer.allocate(2048);
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
}
