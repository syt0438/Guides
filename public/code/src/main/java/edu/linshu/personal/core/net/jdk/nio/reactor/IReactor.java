package edu.linshu.personal.core.net.jdk.nio.reactor;

import edu.linshu.personal.core.net.jdk.IClientSocket;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 9:48
 */
public interface IReactor extends Runnable {

    void handle(IClientSocket client);

    void start();

    void close();

    boolean isClose();

    default void setSubReactorGroup(ReactorGroup subReactorGroup) {
        throw new UnsupportedOperationException();
    }
}
