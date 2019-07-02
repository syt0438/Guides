package edu.linshu.personal.core.net.jdk;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/21 14:20
 */
public interface ISocket extends Closeable {
    String EMPTY_STRING = "";

    @Override
    void close() throws IOException;

    boolean isClosed();

    void setSoTimeout(long time, TimeUnit unit) throws SocketException;
}
