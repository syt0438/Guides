package edu.linshu.personal.test.net.netty;

import edu.linshu.personal.core.net.netty.NettyClient;
import edu.linshu.personal.core.net.netty.decoders.TimeDecoder;
import edu.linshu.personal.core.net.netty.encoders.CustomProtocolEncoder;
import edu.linshu.personal.core.net.netty.handlers.TimeClientHandler;
import io.netty.channel.ChannelFuture;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Objects;
import java.util.Scanner;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 10:30
 */
@Log4j2
public class NettyClientTest {
    @Test
    public void customProtocolClient() throws InterruptedException {
        int messageLimit = 200;

        new NettyClient("localhost", 55555)
                .run(
                        (ch) -> {
                            try {
                                ChannelFuture lastWriteFuture;
                                Scanner scanner = new Scanner(System.in);

                                for (; ; ) {
                                    try {
                                        String msg = scanner.nextLine();

                                        if (Objects.isNull(msg)) {
                                            continue;
                                        }

                                        lastWriteFuture = ch.channel().writeAndFlush(msg + System.lineSeparator());

                                        if (StringUtils.equalsIgnoreCase("bye", msg)) {
                                            ch.channel().closeFuture().sync();

                                            break;
                                        }
                                    } catch (InterruptedException e) {
                                        log.error("ERR: {}", e.getMessage());
                                    }
                                }

                                if (Objects.nonNull(lastWriteFuture)) {
                                    lastWriteFuture.sync();
                                }
                            } catch (InterruptedException e) {
                                log.error("ERR: {}", e.getMessage());
                            }
                        },
                        new CustomProtocolEncoder(messageLimit)
                );
    }

    @Test
    public void timeClient() throws InterruptedException {
        new NettyClient("localhost", 55555)
                .run(new TimeDecoder(), new TimeClientHandler());
    }

}
