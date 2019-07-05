package edu.linshu.personal.core.net.netty.decoders;

import edu.linshu.personal.core.net.netty.bean.UnixTime;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/05 14:03
 */
public class TimeDecoder extends ByteToMessageDecoder {

    private static final int TIME_PROTOCOL_BYTES_COUNT = 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < TIME_PROTOCOL_BYTES_COUNT) {
            return;
        }

        out.add(new UnixTime(in.readUnsignedInt()));
    }
}
