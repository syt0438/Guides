package edu.linshu.personal.core.net.jdk.nio.reactor;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import edu.linshu.personal.core.net.jdk.IServerSocket;
import lombok.extern.java.Log;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 9:53
 */
@Log
public class MainReactor implements IReactor {
    private final Selector selector;
    private final IServerSocket socket;
    private final Thread worker;

    private ReactorGroup subReactorGroup;

    private volatile boolean ran;

    public MainReactor(IServerSocket socket) {
        this(socket, null);
    }

    public MainReactor(IServerSocket socket, String name) {
        try {
            this.socket = socket;
            this.selector = Selector.open();
            this.worker = new Thread(this, Objects.requireNonNullElse(name, "MainReactor"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSubReactorGroup(ReactorGroup subReactorGroup) {
        this.subReactorGroup = subReactorGroup;
    }

    @Override
    public void handle(IClientSocket client) {
        try {
            log.info(worker.getName() + ": 客户端连接成功: " + client.getRemoteSocketAddress());

            IReactor reactor = subReactorGroup.election();

            reactor.handle(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        worker.start();
        ran = true;
    }

    @Override
    public void close() {
        try {
            ran = false;

            if (!isClose()) {
                socket.close();
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isClose() {
        return socket.isClosed() && !selector.isOpen();
    }

    @Override
    public void run() {
        try {
            socket.register(selector, SelectionKey.OP_ACCEPT, socket);

        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        }

        while (ran) {
            try {
                selector.select();

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    keyIterator.remove();

                    if (selectionKey.isValid() && selectionKey.isAcceptable()) {
                        IServerSocket server = (IServerSocket) selectionKey.attachment();

                        IClientSocket client = server.accept();

                        if (Objects.isNull(client)) {
                            continue;
                        }

                        handle(client);
                    }
                }

                selector.selectNow();
            } catch (ClosedSelectorException e) {
                log.info(worker.getName() + "：程序关闭");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
