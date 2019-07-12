package edu.linshu.personal.test.net.netty;

import edu.linshu.personal.core.net.netty.NettyServer;
import edu.linshu.personal.core.net.netty.encoders.TimeEncoder;
import edu.linshu.personal.core.net.netty.handlers.DiscardServerHandler;
import edu.linshu.personal.core.net.netty.handlers.EchoServerHandler;
import edu.linshu.personal.core.net.netty.handlers.TelnetServerHandler;
import edu.linshu.personal.core.net.netty.handlers.TimeServerHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/02 13:44
 */
public class NettyServerTest {

    @Test
    public void telnetServer() throws Exception {
        new NettyServer(55555).run(
                new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()),
                new StringDecoder(UTF_8),
                new StringEncoder(UTF_8),
                new TelnetServerHandler()
        );

    }

    @Test
    public void timeServerTest() throws InterruptedException {
        new NettyServer(55555).run(new TimeEncoder(), new TimeServerHandler());
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
