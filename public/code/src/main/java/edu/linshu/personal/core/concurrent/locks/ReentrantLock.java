package edu.linshu.personal.core.concurrent.locks;

import edu.linshu.personal.core.unsafe.UnsafeUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import static java.lang.Thread.currentThread;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/05/24 14:43
 */
public class ReentrantLock implements Lock {

    private static final Unsafe UNSAFE = UnsafeUtils.getUnsafe();

    private static final long PERMITS_OFFSET;

    private static final long REENTRY_COUNT_OFFSET;

    static {
        try {
            Field reentryCountField = ReentrantLock.class.getDeclaredField("reentryCount");
            Field permitsField = ReentrantLock.class.getDeclaredField("permits");

            PERMITS_OFFSET = UNSAFE.objectFieldOffset(permitsField);
            REENTRY_COUNT_OFFSET = UNSAFE.objectFieldOffset(reentryCountField);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 抽象：资源的数量
     */
    private volatile int permits = 1;

    /**
     * 当前锁的持有者
     */
    private volatile Thread owner;

    /**
     * 锁的重入次数
     */
    private volatile int reentryCount = 0;

    /**
     * 锁池
     */
    private volatile Queue<Thread> waiterPool = new ConcurrentLinkedQueue<>();

    @Override
    public void lock() {
        for (; ; ) {
            if (tryLock()) {
                return;
            }

            waiterPool.add(currentThread());
            LockSupport.park(currentThread());
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        for (; ; ) {
            if (Thread.interrupted()) {
                waiterPool.remove(currentThread());

                throw new InterruptedException();
            }

            if (tryLock()) {
                return;
            }

            waiterPool.add(currentThread());
            LockSupport.park(currentThread());
        }
    }

    @Override
    public boolean tryLock() {
        if (isExclusiveOwnerThread() || tryAcquire()) {
            entryIncrement();
            setExclusiveOwnerThread(currentThread());

            return true;
        }

        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        long deadline = System.nanoTime() + unit.toNanos(time);

        for (; ; ) {
            if (tryLock()) {
                return true;
            }

            long nanoTimeout = deadline - System.nanoTime();

            if (nanoTimeout <= 0) {
                return false;
            }

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    @Override
    public void unlock() {
        if (!isExclusiveOwnerThread()) {
            throw new IllegalMonitorStateException();
        }

        // 当重入次数为 0 时，释放该锁
        if (entryDecrement() == 0) {
            if (!tryRelease()) {
                throw new IllegalMonitorStateException();
            }

            setExclusiveOwnerThread(null);

            notifyWaiters();
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private boolean tryAcquire() {
        return UNSAFE.compareAndSwapInt(this, PERMITS_OFFSET, 1, 0);
    }

    private boolean tryRelease() {
        return UNSAFE.compareAndSwapInt(this, PERMITS_OFFSET, 0, 1);
    }

    private void setExclusiveOwnerThread(Thread thread) {
        this.owner = thread;
    }

    private boolean isExclusiveOwnerThread() {
        return currentThread() == owner;
    }

    private void notifyWaiters() {
        if (waiterPool.isEmpty()) {
            return;
        }

        Thread waiter = waiterPool.remove();

        LockSupport.unpark(waiter);
    }

    private int entryIncrement() {
        int currentValue;

        do {
            currentValue = reentryCount;
        } while (!UNSAFE.compareAndSwapInt(this, REENTRY_COUNT_OFFSET, currentValue, currentValue + 1));

        return reentryCount;
    }

    private int entryDecrement() {
        int currentValue;

        do {
            currentValue = reentryCount;
        } while (!UNSAFE.compareAndSwapInt(this, REENTRY_COUNT_OFFSET, currentValue, currentValue - 1));

        return reentryCount;
    }
}
