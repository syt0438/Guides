package edu.linshu.personal.core.net;

import edu.linshu.personal.core.net.nio.ISelector;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public interface IClientSocket extends ISocket, ISelector {

    boolean connect(String ip, Integer port) throws IOException;

    /**
     * 如果 finishConnect() 返回 false, 则表示连接尚未完成
     * 如果 finishConnect() 抛出异常, 则表示连接失败
     */
    boolean finishConnection() throws IOException;

    void setConnectionTimeout(long time, TimeUnit unit);

    SocketAddress getRemoteSocketAddress() throws IOException;

    void sendMsg(String msg, Charset charset) throws IOException;

    String readMsg(Charset charset) throws IOException;
}
