package edu.linshu.personal.test.net.nio;

import edu.linshu.personal.core.concurrent.locks.CountDownLatch;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 22:19
 */
@SuppressWarnings("all")
public class NIOMultiClientConcurrentPressureTest {

    @Test
    public void test() throws InterruptedException {
        int workerCount = 100;
        int taskCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch openLock = new CountDownLatch(workerCount);
        CountDownLatch endLock = new CountDownLatch(workerCount * taskCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < workerCount; i++) {
            executor.submit(() -> {
                try {
                    openLock.countDown();
                    openLock.await();

                    for (int j = 0; j < taskCount; j++) {
                        try {
                            NIOSelectorClient.sendRequest("苦难与不幸是智者的晋升之梯，信徒的洗礼之水，弱者的无底深渊");
                        } finally {
                            endLock.countDown();
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            });
        }

        endLock.await();

        long endTime = System.currentTimeMillis();

        long time = endTime - startTime;
        long timeSeconds = TimeUnit.MILLISECONDS.toSeconds(time);

        System.out.println("\r\n\r\n\r\n\r\n\r\n任务执行完毕, 用时：" + timeSeconds + "秒!!!");
        executor.shutdown();
    }

}
