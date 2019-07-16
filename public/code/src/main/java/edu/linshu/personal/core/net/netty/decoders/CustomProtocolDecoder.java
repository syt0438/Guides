package edu.linshu.personal.core.net.netty.decoders;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.log4j.Log4j2;

import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/16 14:03
 */
@Log4j2
public class CustomProtocolDecoder extends ByteToMessageDecoder {
    private static final int BUFFER_SIZE = 1024;

    private final int messageLimit;
    private ByteBuf unresolvedBuf;

    public CustomProtocolDecoder(int messageLimit) {
        this.messageLimit = messageLimit;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.unresolvedBuf = Unpooled.buffer();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        if (Objects.nonNull(unresolvedBuf)) {
            ReferenceCountUtil.release(unresolvedBuf);

            this.unresolvedBuf = null;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf decodeBuf = Unpooled.buffer(BUFFER_SIZE);

        try {
            SocketAddress remoteAddress = ctx.channel().remoteAddress();

            int oldUnresolvedBytes = unresolvedBuf.readableBytes();

            if (oldUnresolvedBytes > 0) {
                log.info("客户端 {} 聚合未解析 {} 字节\n", remoteAddress, oldUnresolvedBytes);

                decodeBuf.writeBytes(unresolvedBuf, oldUnresolvedBytes);
            }

            int readableBytes = in.readableBytes();

            if (readableBytes == 0) {
                log.info("未从 {} 客户端读取到数据\n", remoteAddress);
            } else {
                decodeBuf.writeBytes(in, readableBytes);
            }


            int packageCount = readableBytes / messageLimit;

            while (decodeBuf.readableBytes() >= messageLimit) {
                CharSequence packageMessage = decodeBuf.readCharSequence(messageLimit, UTF_8);

                out.add(packageMessage);
            }

            int unresolvedBytes = decodeBuf.readableBytes();
            log.info("从 {} 客户端读取到 {} 字节，解析 {} 条消息，剩余 {} 字节未解析\n", remoteAddress, readableBytes, packageCount, unresolvedBytes);

            unresolvedBuf.writeBytes(decodeBuf, unresolvedBytes);

            log.info("客户端 {} 写入未解析 {} 字节到未解析缓冲区\n", remoteAddress, unresolvedBytes);
        } finally {
            ReferenceCountUtil.release(decodeBuf);
        }
    }
}
