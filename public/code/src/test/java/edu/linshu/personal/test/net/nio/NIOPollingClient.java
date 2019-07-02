package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import edu.linshu.personal.core.net.jdk.nio.NIOPollingSocket;
import lombok.extern.java.Log;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/21 14:19
 */
@SuppressWarnings("all")
@Log
public class NIOPollingClient {

    @Test
    public void test() throws IOException, InterruptedException {
        IClientSocket client = new NIOPollingSocket();
//        client.setSoTimeout(10, TimeUnit.SECONDS);
        client.setConnectionTimeout(60, TimeUnit.SECONDS);
        client.connect("0.0.0.0", 55555);
        log.info("连接成功：" + client.getRemoteSocketAddress());

        log.info("等待接收用户输入信息：");
        Scanner scanner = new Scanner(System.in);
        String msg = scanner.nextLine();

        log.info("准备发送用户消息：" + msg);
        client.sendMsg(msg, StandardCharsets.UTF_8);
        log.info("发送用户消息成功");

        log.info("等待接收服务器信息: ");
        String readMsg = client.readMsg(StandardCharsets.UTF_8);

        log.info("接收到服务器响应信息，客户端 3 秒后关闭：\r\n\r\n" + readMsg);

        TimeUnit.SECONDS.sleep(3);

        client.close();
        scanner.close();
    }
}
