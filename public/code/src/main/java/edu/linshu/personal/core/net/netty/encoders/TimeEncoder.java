package edu.linshu.personal.core.net.netty.encoders;

import edu.linshu.personal.core.net.netty.bean.UnixTime;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.log4j.Log4j2;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/05 16:38
 */
@Log4j2
@ChannelHandler.Sharable
public class TimeEncoder extends MessageToByteEncoder<UnixTime> {

    @Override
    protected void encode(ChannelHandlerContext ctx, UnixTime msg, ByteBuf out) throws Exception {
        out.writeInt((int) msg.value());

        log.info("反馈客户端 [{}] 消息：[{}]\r\n", ctx.channel().remoteAddress(), msg);
    }
}
