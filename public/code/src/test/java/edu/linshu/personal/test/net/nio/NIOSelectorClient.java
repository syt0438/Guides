package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import edu.linshu.personal.core.net.jdk.nio.ISelector;
import edu.linshu.personal.core.net.jdk.nio.NIOSocket;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 22:06
 */
@SuppressWarnings("all")
@Log4j2
public class NIOSelectorClient {
    @Test
    public void test() throws IOException, InterruptedException {
        sendRequest("苦难与不幸是智者的晋升之梯，信徒的洗礼之水，弱者的无底深渊", false);
    }

    static void sendRequest() throws IOException, InterruptedException {
        sendRequest(null, false);
    }

    static void sendRequest(String msg) throws IOException, InterruptedException {
        sendRequest(msg, false);
    }

    static void sendRequest(String msg, boolean withoutResponse) throws IOException, InterruptedException {
        Selector selector = Selector.open();

        IClientSocket client = new NIOSocket();
        ISelector clientSelector = (ISelector) client;

        if (!client.connect("0.0.0.0", 55555)) {
            clientSelector.register(selector, SelectionKey.OP_CONNECT);
        }

        while (!client.isClosed()) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isValid() && key.isConnectable()) {
                    if (!client.finishConnection()) {
                        continue;
                    }

                    log.info("连接成功：" + client.getRemoteSocketAddress());

                    clientSelector.register(selector, SelectionKey.OP_WRITE);
                }

                if (key.isValid() && key.isWritable()) {
                    try {
                        if (Objects.isNull(msg)) {
                            log.info("等待接收用户输入信息：");
                            Scanner scanner = new Scanner(System.in);
                            msg = scanner.nextLine();
                            scanner.close();
                        }

                        log.info("准备发送用户消息：" + msg);
                        client.sendMsg(msg, StandardCharsets.UTF_8);
                        log.info("发送用户消息成功");

                        if (!withoutResponse) {
                            clientSelector.register(selector, SelectionKey.OP_READ);
                        }
                    } finally {
                        if (withoutResponse) {
                            key.cancel();
                            client.close();
                        }
                    }
                }

                if (key.isValid() && key.isReadable()) {
                    try {
                        log.info("等待接收服务器信息: ");
                        String readMsg = client.readMsg(StandardCharsets.UTF_8);

                        log.info("接收到服务器响应信息：\r\n\r\n" + readMsg + "\r\n客户端即将关闭");
                    } finally {
                        key.cancel();
                        client.close();
                    }
                }
            }

            selector.selectNow();
        }
    }

}
