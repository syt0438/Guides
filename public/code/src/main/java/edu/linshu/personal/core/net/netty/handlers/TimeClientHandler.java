package edu.linshu.personal.core.net.netty.handlers;

import edu.linshu.personal.core.net.netty.bean.UnixTime;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 10:29
 */
@Log4j2
public class TimeClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            UnixTime time = (UnixTime) msg;

            log.info("客户端接收 [{}] 消息：[{}]\r\n", ctx.channel().remoteAddress(), time);
        } finally {
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端发生异常 [{}] 异常消息：[{}]\r\n", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
