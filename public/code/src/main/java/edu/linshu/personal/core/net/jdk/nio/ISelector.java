package edu.linshu.personal.core.net.jdk.nio;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/26 21:01
 */
public interface ISelector {

    default SelectionKey register(Selector selector, int ops, Object attachment) throws ClosedChannelException {
        throw new UnsupportedOperationException();
    }

    default SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        throw new UnsupportedOperationException();
    }
}
