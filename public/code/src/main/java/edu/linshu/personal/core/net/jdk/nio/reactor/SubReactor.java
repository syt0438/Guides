package edu.linshu.personal.core.net.jdk.nio.reactor;

import edu.linshu.personal.core.net.jdk.IClientSocket;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 11:04
 */
@Log4j2
public class SubReactor implements IReactor {

    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Selector selector;
    private final Thread worker;

    private volatile boolean ran;

    private final Queue<IClientSocket> clients = new LinkedBlockingQueue<>();


    public SubReactor() {
        this(null);
    }

    public SubReactor(String name) {
        try {
            this.selector = Selector.open();
            this.worker = new Thread(this, Objects.requireNonNullElse(name, "SubReactor"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void handle(IClientSocket client) {
        clients.offer(client);
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
                selector.close();
                executor.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isClose() {
        return !selector.isOpen() && executor.isShutdown();
    }

    @Override
    @SuppressWarnings("all")
    public void run() {
        while (ran) {
            while (!clients.isEmpty()) {
                IClientSocket client = clients.poll();

                try {
                    client.register(selector, OP_READ, client);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            }

            try {
                selector.select(100);

                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    keyIterator.remove();

                    if (selectionKey.isValid() && selectionKey.isReadable()) {
                        try {
                            IClientSocket client = (IClientSocket) selectionKey.attachment();

                            log.info(worker.getName() + "：准备接收客户端[" + client.getRemoteSocketAddress() + "]信息：");
                            String msg = client.readMsg(StandardCharsets.UTF_8);


                            log.info(worker.getName() + "：接收客户端[" + client.getRemoteSocketAddress() + "]信息：\n" + msg);
                            client.register(selector, SelectionKey.OP_WRITE, client);

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                        }
                    }

                    if (selectionKey.isValid() && selectionKey.isWritable()) {
                        try {
                            IClientSocket client = (IClientSocket) selectionKey.attachment();

                            StringBuilder responseBody = new StringBuilder();
                            responseBody.append("{");
                            responseBody.append("\"msg\": \"苦难与不幸是智者的晋升之梯，信徒的洗礼之水，弱者的无底深渊\"");
                            responseBody.append("}");
                            String responseMsg = "" +
                                    "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json;charset=UTF-8\r\n" +
                                    "Content-Length: " + responseBody.toString().getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" +
                                    responseBody.toString();

                            client.sendMsg(responseMsg, StandardCharsets.UTF_8);
                            log.info(worker.getName() + "：反馈客户端[" + client.getRemoteSocketAddress() + "]信息：成功\n\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                IClientSocket client = (IClientSocket) selectionKey.attachment();
                                client.close();
                                selectionKey.cancel();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }

                selector.selectNow();

            } catch (ClosedSelectorException e) {
                log.info(worker.getName() + "：程序关闭");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
