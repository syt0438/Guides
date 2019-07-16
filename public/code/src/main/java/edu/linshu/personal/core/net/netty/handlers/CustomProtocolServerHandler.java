package edu.linshu.personal.core.net.netty.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.log4j.Log4j2;

/**
 * @author Linshu 745698872@qq.com
 * @date 2019/07/04 10:29
 */
@Log4j2
@ChannelHandler.Sharable
public class CustomProtocolServerHandler extends SimpleChannelInboundHandler<CharSequence> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CharSequence msg) throws Exception {
        log.info("\n读取到客户端 {} 消息: \n{}\n", ctx.channel().remoteAddress(), msg);
    }
}
