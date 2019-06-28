package edu.linshu.personal.test.concurrent;

import edu.linshu.personal.core.concurrent.locks.ReentrantLock;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/05/24 16:05
 */
@SuppressWarnings("all")
public class ReentrantLockTimeUnitTest {

    @Test
    public void test() throws InterruptedException {
        final Lock lock = new ReentrantLock();

        final CountDownLatch endLock = new CountDownLatch(2);

        final Thread timeThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    TimeUnit.SECONDS.sleep(1);

//                boolean successed  = lock.tryLock();
//                boolean successed = lock.tryLock(6, TimeUnit.SECONDS);
                    lock.lockInterruptibly();

                    try {
                        System.out.println("计时获取锁：尝试获取锁成功");
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    System.out.println("计时获取锁：尝试获取锁失败，线程中断");
                } finally {
                    endLock.countDown();
                }
            }
        });

        timeThread.start();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    lock.lock();

                    try {
                        System.out.println("普通获取锁：尝试获取锁成功");

                        TimeUnit.SECONDS.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();

                        System.out.println("普通获取锁：释放锁成功");
                    }
                } finally {
                    endLock.countDown();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
//            if (System.currentTimeMillis() % 10 > 5) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("中断器：准备中断计时获取锁");

                timeThread.interrupt();
//            }
            }
        }).start();

        endLock.await();
    }

}
