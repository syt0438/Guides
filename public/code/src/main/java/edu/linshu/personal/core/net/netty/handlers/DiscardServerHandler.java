package edu.linshu.personal.core.net.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.java.Log;

import java.nio.charset.StandardCharsets;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/02 11:23
 */
@ChannelHandler.Sharable
@Log
public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        log.info("客户端 [" + ctx.channel().remoteAddress() + "] 入站信息：\r\n" + byteBuf.toString(StandardCharsets.UTF_8));

        byteBuf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();

        ctx.close();
    }
}
