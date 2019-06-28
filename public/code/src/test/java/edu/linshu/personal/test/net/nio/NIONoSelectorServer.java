package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.net.IClientSocket;
import edu.linshu.personal.core.net.IServerSocket;
import edu.linshu.personal.core.net.nio.NIOServerPollingSocket;
import lombok.extern.java.Log;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 7:09
 */
@SuppressWarnings("all")
@Log
public class NIONoSelectorServer {

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    private static final LinkedList<IClientSocket> clients = new LinkedList<>();

    @Test
    public void test() throws IOException {
        IServerSocket server = new NIOServerPollingSocket();
        server.bind("0.0.0.0", 55555);
        log.info("服务端启动成功: " + server.getLocalSocketAddress());

        while (!server.isClosed()) {
            IClientSocket acceptClient = server.accept();

            if (Objects.nonNull(acceptClient)) {
                log.info("客户端连接成功: " + acceptClient.getRemoteSocketAddress());
                clients.addLast(acceptClient);
            }

            Iterator<IClientSocket> clientIterator = clients.iterator();

            while (clientIterator.hasNext()) {
                IClientSocket client = clientIterator.next();

                executor.execute(() -> {
                    try {
                        log.info("准备接收客户端[" + client.getRemoteSocketAddress() + "]信息：");
                        String msg = client.readMsg(StandardCharsets.UTF_8);

                        log.info("接收客户端[" + client.getRemoteSocketAddress() + "]信息：\n" + msg);

                        String responseBody = "{ 'uid': ['123', '456', '789'] }";
                        String responseMsg = "" +
                                "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json;charset=UTF-8\r\n" +
                                "Content-Length: " + responseBody.length() + "\r\n\r\n" +
                                responseBody;

                        client.sendMsg(responseMsg, StandardCharsets.UTF_8);
                        log.info("\n\n反馈客户端[" + client.getRemoteSocketAddress() + "]信息：成功\n\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            client.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                clientIterator.remove();
            }

        }

        server.close();
    }

}
