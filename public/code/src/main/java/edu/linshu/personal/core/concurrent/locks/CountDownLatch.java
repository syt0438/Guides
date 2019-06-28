package edu.linshu.personal.core.concurrent.locks;

import edu.linshu.personal.core.unsafe.UnsafeUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import static java.lang.Thread.currentThread;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/05/28 19:55
 */
public class CountDownLatch {

    private static final Unsafe UNSAFE = UnsafeUtils.getUnsafe();

    private static final long COUNT_OFFSET;

    static {
        try {
            Field countField = CountDownLatch.class.getDeclaredField("count");

            COUNT_OFFSET = UNSAFE.objectFieldOffset(countField);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    private volatile int count;

    private volatile Queue<Thread> waiterPool = new ConcurrentLinkedQueue<>();

    public CountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        }

        this.count = count;
    }

    public void countDown() {
        releaseShared(1);

        if (this.count == 0) {
            notifyWaiters();
        }
    }

    public void await() throws InterruptedException {
        if (currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        if (this.count == 0) {
            return;
        }

        waiterPool.offer(currentThread());
        LockSupport.park(currentThread());

        if (currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

    private void notifyWaiters() {
        for (; ; ) {
            if (waiterPool.isEmpty()) {
                return;
            }

            Thread waiter = waiterPool.remove();

            LockSupport.unpark(waiter);
        }
    }

    private void releaseShared(int arg) {
        int count;

        do {
            count = this.count;
        } while (!UNSAFE.compareAndSwapInt(this, COUNT_OFFSET, count, count - arg));
    }

}
