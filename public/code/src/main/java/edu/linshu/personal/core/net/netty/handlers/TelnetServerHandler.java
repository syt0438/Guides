package edu.linshu.personal.core.net.netty.handlers;

import io.netty.channel.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.Date;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/11 17:28
 */
@Log4j2
@ChannelHandler.Sharable
public class TelnetServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write("Welcome to " + InetAddress.getLocalHost() + "~\r\n");
        ctx.write("It is " + new Date() + " now.\r\n");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        String response;
        boolean close = false;

        if (StringUtils.isEmpty(msg)) {
            response = "Please type something.\r\n";
        } else if (StringUtils.equalsIgnoreCase("bye", msg)) {
            response = "Have a good day!\r\n";
            close = true;
        } else {
            response = "Did you say '" + msg + "'?\r\n";
        }

        log.info("响应客户端[{}]: [{}]", ctx.channel().remoteAddress(), msg);
        ChannelFuture future = ctx.write(response);

        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error: [{}]", cause.getMessage());
        ctx.close();
    }
}
