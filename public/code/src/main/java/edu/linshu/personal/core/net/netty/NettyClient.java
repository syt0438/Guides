package edu.linshu.personal.core.net.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 10:13
 */
public class NettyClient {

    private final String host;
    private final Integer port;

    public NettyClient(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void run(ChannelHandler... handlers) throws InterruptedException {
        run(null, handlers);
    }

    public void run(Consumer<ChannelFuture> handler, ChannelHandler... handlers) throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(handlers);
                }
            });

            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();

            if (Objects.nonNull(handler)) {
                handler.accept(channelFuture);
            } else {
                channelFuture.channel().closeFuture().sync();
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

}
