package edu.linshu.personal.core.net.netty.encoders;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.log4j.Log4j2;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author Linshu 745698872@qq.com
 * @date 2019/7/16 18:35
 */
@Log4j2
@ChannelHandler.Sharable
public class CustomProtocolEncoder extends MessageToByteEncoder<String> {
    private final int messageLimit;

    public CustomProtocolEncoder(int messageLimit) {
        this.messageLimit = messageLimit;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
        msg = msg.replaceAll("[\r\n\t\b]", msg);
        byte[] msgBytes = msg.getBytes(UTF_8);
        int msgBytesLength = msgBytes.length;

        ByteBuf pendingSendBuf = Unpooled.buffer(msgBytesLength);
        pendingSendBuf.writeBytes(msgBytes, 0, msgBytesLength);

        int packageCount = msgBytesLength / messageLimit;
        int unsentBytesCount = msgBytesLength % messageLimit;

        log.info("输入 {} 字节消息, 将被拆分为 {} 个完成数据包发送，以及 1 个填充数据包发送 {} 字节\n", msgBytesLength, packageCount, unsentBytesCount);

        while (pendingSendBuf.readableBytes() % messageLimit != 0) {
            pendingSendBuf.writeByte('-');
        }

        while (pendingSendBuf.readableBytes() >= messageLimit) {
            out.writeBytes(pendingSendBuf, messageLimit);
        }

        log.info("消息发送完毕\n\n");
    }
}
