package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.net.jdk.IServerSocket;
import edu.linshu.personal.core.net.jdk.nio.reactor.MainReactorGroup;
import edu.linshu.personal.core.net.jdk.nio.reactor.SubReactorGroup;
import edu.linshu.personal.core.net.jdk.nio.NIOServerSocket;
import lombok.extern.java.Log;
import org.junit.Test;

import java.io.IOException;


/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 9:45
 */
@Log
public class NIOMultiReactorServer {

    @Test
    public void test() throws IOException {
        IServerSocket server = new NIOServerSocket();
        server.bind("0.0.0.0", 55555);
        log.info("服务端启动成功: " + server.getLocalSocketAddress());

        /*
         * TODO:
         *  问题分析：第一次请求时，accept 为 null
         *  问题原因：多个 MainReactor 线程同时处理 Accept 请求时，
         *          请求可能已经被其中一种 MainReactor 线程处理，
         *          所以其它线程 accept 的时候，当前 accept 的 client 已经被处理
         */
        MainReactorGroup mainReactorGroup = new MainReactorGroup(server, 2);
        SubReactorGroup subReactorGroup = new SubReactorGroup(4);
        mainReactorGroup.setSubReactorGroup(subReactorGroup);
        mainReactorGroup.start();
    }

}
