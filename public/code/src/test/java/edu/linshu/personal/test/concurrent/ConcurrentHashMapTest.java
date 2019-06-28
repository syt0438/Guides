package edu.linshu.personal.test.concurrent;

import edu.linshu.personal.core.concurrent.locks.CountDownLatch;
import edu.linshu.personal.core.concurrent.locks.ReentrantLock;
import edu.linshu.personal.source.jdk.ConcurrentHashMap;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import static java.lang.Thread.currentThread;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/05/24 16:05
 */
@SuppressWarnings("all")
public class ConcurrentHashMapTest {

    private final static Map<String, String> data = new ConcurrentHashMap<>();

    @Test
    public void test() throws InterruptedException {
        final Lock lock = new ReentrantLock();
        ThreadFactory threadFactory = getThreadFactory();
        int taskCount = 100;

        final long[] count = {0};

        final CountDownLatch startLock = new CountDownLatch(taskCount);
        final CountDownLatch endLock = new CountDownLatch(taskCount);
        List<Thread> threads = new LinkedList<>();

        for (int i = 0; i < taskCount; i++) {
            Thread thread = threadFactory.newThread(new Runnable() {
                @Override
                public void run() {
                    startLock.countDown();

                    try {
                        startLock.await();

                        for (int j = 0; j < 100; j++) {
                            doHandle(count);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        endLock.countDown();
                    }
                }
            });

            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        endLock.await();

        System.out.println(count[0]);
    }

    private void doHandle(long[] count) throws InterruptedException {
        String key = "[Thread-" + currentThread().getName() + "]: " + count[0]++;

        System.out.println(key);

        data.put(key, key);

        TimeUnit.MILLISECONDS.sleep(20);
    }

    private ThreadFactory getThreadFactory() {
        return new ThreadFactory() {
            private AtomicInteger adder = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Test[" + adder.incrementAndGet() + "]");
            }
        };
    }

}
