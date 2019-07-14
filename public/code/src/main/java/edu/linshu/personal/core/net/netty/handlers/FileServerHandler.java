package edu.linshu.personal.core.net.netty.handlers;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.log4j.Log4j2;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/12 8:13
 */
@Log4j2
@ChannelHandler.Sharable
public class FileServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush("Hello: Type the path of the file to retrieve。\r\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ERR: [{}]", cause.getMessage());

        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(
                    "ERR: " + cause.getClass().getSimpleName() + ": " +
                            cause.getMessage() + "\r\n"
            ).addListener(ChannelFutureListener.CLOSE);
        }

    }
}
