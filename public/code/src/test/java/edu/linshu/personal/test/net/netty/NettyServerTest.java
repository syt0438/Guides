package edu.linshu.personal.test.net.netty;

import edu.linshu.personal.core.net.netty.NettyServer;
import edu.linshu.personal.core.net.netty.handlers.DiscardServerHandler;
import edu.linshu.personal.core.net.netty.handlers.EchoServerHandler;
import edu.linshu.personal.core.net.netty.handlers.TimeServerHandler;
import org.junit.Test;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/02 13:44
 */
public class NettyServerTest {

    @Test
    public void timeServerTest() throws InterruptedException {
        new NettyServer(55555).run(new TimeServerHandler());
    }

    @Test
    public void echoServerTest() throws InterruptedException {
        new NettyServer(55555).run(new EchoServerHandler());
    }

    @Test
    public void discardServerTest() throws InterruptedException {
        new NettyServer(55555).run(new DiscardServerHandler());
    }

}
