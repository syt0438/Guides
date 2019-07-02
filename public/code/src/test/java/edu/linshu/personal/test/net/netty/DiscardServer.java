package edu.linshu.personal.test.net.netty;

import edu.linshu.personal.core.net.netty.NettyServer;
import edu.linshu.personal.core.net.netty.handlers.DiscardServerHandler;
import org.junit.Test;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/02 13:44
 */
public class DiscardServer {

    @Test
    public void test() throws InterruptedException {
        new NettyServer(55555).run(new DiscardServerHandler());
    }

}
