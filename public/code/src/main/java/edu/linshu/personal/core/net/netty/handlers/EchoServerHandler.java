package edu.linshu.personal.core.net.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 9:05
 */
@Log4j2
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;

        log.info("反馈客户端 [{}] 消息：[{}]\r\n", ctx.channel().remoteAddress(), in.toString(StandardCharsets.UTF_8));

        ctx.writeAndFlush(msg);
    }
}
