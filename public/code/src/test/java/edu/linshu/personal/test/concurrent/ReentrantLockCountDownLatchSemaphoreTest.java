package edu.linshu.personal.test.concurrent;

import edu.linshu.personal.core.concurrent.locks.CountDownLatch;
import edu.linshu.personal.core.concurrent.locks.ReentrantLock;
import edu.linshu.personal.core.concurrent.locks.Semaphore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
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
public class ReentrantLockCountDownLatchSemaphoreTest {

    @Test
    public void test() throws InterruptedException {
        final Lock lock = new ReentrantLock();
        ThreadFactory threadFactory = getThreadFactory();
        final int[] adderCount = {100};
        int taskCount = 10;

        final long[] count = {0};

        final Semaphore semaphore = new Semaphore(2);
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

                        semaphore.acquire();

                        for (int j = 0; j < adderCount[0]; j++) {

                            lock.lock();

                            try {
                                lock.tryLock();

                                try {
                                    lock.tryLock();

                                    try {
                                        lock.tryLock();

                                        try {
                                            doHandle(count);
                                        } finally {
                                            lock.unlock();
                                        }
                                    } finally {
                                        lock.unlock();
                                    }
                                } finally {
                                    lock.unlock();
                                }
                            } finally {
                                lock.unlock();
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        semaphore.release();

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

    private static void doHandle(long[] count) throws InterruptedException {
        System.out.println("[Thread-" + currentThread().getName() + "]: " + count[0]++);

        TimeUnit.MILLISECONDS.sleep(20);
    }

    private static ThreadFactory getThreadFactory() {
        return new ThreadFactory() {
            private AtomicInteger adder = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Test[" + adder.incrementAndGet() + "]");
            }
        };
    }

}
