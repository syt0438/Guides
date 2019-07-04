package edu.linshu.personal.core.net.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.log4j.Log4j2;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 9:32
 */
@Log4j2
@ChannelHandler.Sharable
public class TimeServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf time = ctx.alloc().buffer(4);
        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));

        ChannelFuture channelFuture = ctx.writeAndFlush(time);
        channelFuture.addListener((ChannelFutureListener) future -> {
            future.channel().close();
            log.info("响应客户端[{}]成功", future.channel().remoteAddress());
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端发生异常 [{}] 异常消息：[{}]\r\n", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
