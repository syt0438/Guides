package edu.linshu.personal.core.net.netty.handlers;

import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import lombok.extern.log4j.Log4j2;

import java.io.RandomAccessFile;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/12 8:13
 */
@Log4j2
@ChannelHandler.Sharable
public class FileServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush("Hello: Type the path of the file to retrieve。\r\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        RandomAccessFile raf = null;
        long length = -1;

        try {
            raf = new RandomAccessFile(msg, "r");

            length = raf.length();
        } catch (Exception e) {
            ctx.writeAndFlush("ERR: " + e.getClass().getSimpleName() + ": " + e.getMessage() + '\n');

            return;
        } finally {
            if (length < 0 && raf != null) {
                raf.close();
            }
        }

        ctx.write("File " + msg + "OK: " + raf.length() + '\n');

        if (ctx.pipeline().get(SslHandler.class) == null) {
            // SSL not enabled - can use zero-copy file transfer.
            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, length));
        } else {
            // SSL enabled - cannot use zero-copy file transfer.
            ctx.write(new ChunkedFile(raf));
        }

        ctx.writeAndFlush("\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("ERR: [{}]", cause.getMessage());

        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(
                    "ERR: " + cause.getClass().getSimpleName() + ": " +
                            cause.getMessage() + "\r\n"
            ).addListener(ChannelFutureListener.CLOSE);
        }

    }
}
