package edu.linshu.personal.core.net.netty.handlers;

import edu.linshu.personal.core.net.netty.bean.UnixTime;
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
        ChannelFuture channelFuture = ctx.writeAndFlush(new UnixTime());

        channelFuture
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端发生异常 [{}] 异常消息：[{}]\r\n", ctx.channel().remoteAddress(), cause.getMessage());

        ctx.close();
    }
}
