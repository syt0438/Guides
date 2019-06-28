package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.net.IClientSocket;
import edu.linshu.personal.core.net.nio.ISelector;
import edu.linshu.personal.core.net.nio.NIOServerSocket;
import lombok.extern.java.Log;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 20:58
 */
@Log
public class NIOSelectorServer {
    @Test
    public void test() throws IOException {
        Selector selector = Selector.open();

        NIOServerSocket serverChannel = new NIOServerSocket();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, serverChannel);

        serverChannel.bind("0.0.0.0", 55555);
        log.info("服务端启动成功: " + serverChannel.getLocalSocketAddress());

        while (!serverChannel.isClosed()) {
            selector.select();

            // 获取所有的事件通知, SelectionKey 相当于事件通知的载体
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isAcceptable()) {
                    NIOServerSocket server = (NIOServerSocket) key.attachment();
                    ISelector client = (ISelector) server.accept();
                    client.register(selector, SelectionKey.OP_READ, client);

                    log.info("客户端连接成功: " + ((IClientSocket) client).getRemoteSocketAddress());
                }

                if (key.isReadable()) {
                    IClientSocket client = (IClientSocket) key.attachment();

                    log.info("准备接收客户端[" + client.getRemoteSocketAddress() + "]信息：");
                    String msg = client.readMsg(StandardCharsets.UTF_8);

                    log.info("接收客户端[" + client.getRemoteSocketAddress() + "]信息：\n" + msg);
                    ((ISelector) client).register(selector, SelectionKey.OP_WRITE, client);
                }

                if (key.isWritable()) {

                    try (IClientSocket client = (IClientSocket) key.attachment()) {
                        StringBuilder responseBody = new StringBuilder();
                        responseBody.append("{");
                        responseBody.append("\"msg\": \"苦难与不幸是智者的晋升之梯，信徒的洗礼之水，弱者的无底深渊\"");
                        responseBody.append("}");
                        String responseMsg = "" +
                                "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json;charset=UTF-8\r\n" +
                                "Content-Length: " + responseBody.toString().getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" +
                                responseBody.toString();

                        client.sendMsg(responseMsg, StandardCharsets.UTF_8);
                        log.info("\n\n反馈客户端[" + client.getRemoteSocketAddress() + "]信息：成功\n\n");
                    } finally {
                        key.cancel();
                    }
                }
            }

            selector.selectNow();
        }
    }

}
