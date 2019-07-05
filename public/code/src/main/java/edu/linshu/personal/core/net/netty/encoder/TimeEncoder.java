package edu.linshu.personal.core.net.netty.encoder;

import edu.linshu.personal.core.net.netty.bean.UnixTime;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/05 16:38
 */
@ChannelHandler.Sharable
public class TimeEncoder extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        UnixTime time = (UnixTime) msg;

        ByteBuf outBuffer = ctx.alloc().buffer(4);
        outBuffer.writeInt((int) time.value());

        ctx.write(outBuffer, promise);
    }
}
