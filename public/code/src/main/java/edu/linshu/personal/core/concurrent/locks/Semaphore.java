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
 * @date 2019/05/28 21:02
 */
public class Semaphore {

    private static final Unsafe UNSAFE = UnsafeUtils.getUnsafe();

    private static final long PERMITS_OFFSET;

    static {
        try {
            Field permitsField = Semaphore.class.getDeclaredField("permits");

            PERMITS_OFFSET = UNSAFE.objectFieldOffset(permitsField);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }

    }

    private final int originalPermits;

    private volatile int permits;

    private volatile Queue<Thread> waiterPool = new ConcurrentLinkedQueue<>();

    public Semaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("permits < 0");
        }

        this.originalPermits = permits;
        this.permits = permits;
    }

    public int getPermits() {
        return permits;
    }

    public void acquire() throws InterruptedException {
        if (currentThread().isInterrupted()) {
            throw new InterruptedException();
        }

        int permits = this.permits;

        if (permits > 0) {
            acquireShared(1);

            return;
        }

        waiterPool.offer(currentThread());
        LockSupport.park(currentThread());
    }

    public void release() {
        releaseShared(1);

        if (!waiterPool.isEmpty()) {
            notifyWaiters();
        }
    }

    private void notifyWaiters() {
        Thread waiter = waiterPool.remove();

        LockSupport.unpark(waiter);
    }

    private void releaseShared(int arg) {
        int permits = this.permits;

        if (permits == originalPermits) {
            return;
        }

        do {
            permits = this.permits;
        } while (!UNSAFE.compareAndSwapInt(this, PERMITS_OFFSET, permits, permits + arg));
    }

    private void acquireShared(int arg) {
        int permits = this.permits;

        if (permits == 0) {
            return;
        }

        do {
            permits = this.permits;
        } while (!UNSAFE.compareAndSwapInt(this, PERMITS_OFFSET, permits, permits - arg));
    }
}
