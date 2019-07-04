package edu.linshu.personal.core.net.netty.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/04 10:29
 */
@Log4j2
public class TimeClientHandler extends ChannelInboundHandlerAdapter {
    private ByteBuf buf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        buf = ctx.alloc().buffer(4);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        buf.release();
        buf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        buf.writeBytes(in);

        try {
            if (buf.readableBytes() >= 4) {
                String time = DateFormatUtils.format((buf.readUnsignedInt() - 2208988800L) * 1000L, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.getPattern());

                log.info("客户端接收 [{}] 消息：[{}]\r\n", ctx.channel().remoteAddress(), time);
            }
        } finally {
            in.release();
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端发生异常 [{}] 异常消息：[{}]\r\n", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }
}
