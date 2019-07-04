package edu.linshu.personal.test.net.netty;

import edu.linshu.personal.core.net.netty.NettyClient;
import edu.linshu.personal.core.net.netty.handlers.TimeClientHandler;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 10:30
 */
@Log4j2
public class NettyClientTest {

    @Test
    public void timeClientTest() throws InterruptedException {
        new NettyClient("localhost", 55555)
                .run(new TimeClientHandler());
    }

}
