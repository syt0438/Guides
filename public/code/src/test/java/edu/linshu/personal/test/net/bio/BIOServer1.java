package edu.linshu.personal.test.net.bio;

import edu.linshu.personal.core.net.IClientSocket;
import edu.linshu.personal.core.net.IServerSocket;
import edu.linshu.personal.core.net.bio.BIOServerSocket;
import lombok.extern.java.Log;
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
@Log
public class BIOServer1 {

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    @Test
    public void test() throws IOException {
        IServerSocket server = new BIOServerSocket();
        server.bind("0.0.0.0", 55555);
        log.info("服务端启动成功: " + server.getLocalSocketAddress());

        while (!server.isClosed()) {
            log.info("服务端等待客户端连接");
            IClientSocket client = server.accept();

            executor.execute(() -> {
                try {
                    log.info("客户端连接成功: " + client.getRemoteSocketAddress());

                    log.info("准备接收客户端[" + client.getRemoteSocketAddress() + "]信息：");
                    String msg = client.readMsg(StandardCharsets.UTF_8);
                    log.info("接收客户端[" + client.getRemoteSocketAddress() + "]信息：\n" + msg);

                    client.sendMsg(msg, StandardCharsets.UTF_8);
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
