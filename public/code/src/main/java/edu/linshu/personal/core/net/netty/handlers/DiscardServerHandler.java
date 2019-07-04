package edu.linshu.personal.core.net.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/02 11:23
 */
@ChannelHandler.Sharable
@Log4j2
public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        try {
            while (byteBuf.isReadable()) {
                System.out.println((char) byteBuf.readByte());
                System.out.flush();
            }

            log.info("客户端 [{}] 入站消息：[{}]\r\n", ctx.channel().remoteAddress(), byteBuf.toString(StandardCharsets.UTF_8));
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();

        ctx.close();
    }
}
