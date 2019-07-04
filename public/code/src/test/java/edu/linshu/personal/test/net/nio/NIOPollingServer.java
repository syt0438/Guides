package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import edu.linshu.personal.core.net.jdk.IServerSocket;
import edu.linshu.personal.core.net.jdk.nio.NIOServerPollingSocket;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 7:09
 */
@SuppressWarnings("all")
@Log4j2
public class NIOPollingServer {

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Test
    public void test() throws IOException {
        IServerSocket server = new NIOServerPollingSocket();
        server.bind("0.0.0.0", 55555);
        log.info("服务端启动成功: " + server.getLocalSocketAddress());

        while (!server.isClosed()) {
//            log.info("服务端等待客户端连接");
            IClientSocket client = server.accept();

            executor.execute(() -> {
                try {
                    log.info("客户端连接成功: " + client.getRemoteSocketAddress());

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
        }

        server.close();
    }

}
