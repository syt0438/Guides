package edu.linshu.personal.test.concurrent;

import edu.linshu.personal.core.concurrent.locks.CountDownLatch;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/18 19:30
 */
public class ForkJoinCountDownLatchDemo {

    private static final ThreadPoolExecutor executor;

    static {
        executor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                6,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(
                        Runtime.getRuntime().availableProcessors() << 2
                ),
                (ThreadFactory) Thread::new
        );
    }

    @Test
    public void test() throws InterruptedException {
        final ForkJoinMockDataSupplier.UserInfo[] userInfos = new ForkJoinMockDataSupplier.UserInfo[1];
        final ForkJoinMockDataSupplier.OrderInfo[] orderInfos = new ForkJoinMockDataSupplier.OrderInfo[1];
        final ForkJoinMockDataSupplier.PointInfo[] pointInfos = new ForkJoinMockDataSupplier.PointInfo[1];
        final ForkJoinMockDataSupplier.GoodInfo[] goodInfos = new ForkJoinMockDataSupplier.GoodInfo[1];

        long startTime = System.currentTimeMillis();

        CountDownLatch endLock = new CountDownLatch(4);

        executor.submit(() -> {
            userInfos[0] = ForkJoinMockDataSupplier.getUserInfo();

            endLock.countDown();
        });
        executor.submit(() -> {
            orderInfos[0] = ForkJoinMockDataSupplier.getOrderInfo();

            endLock.countDown();
        });
        executor.submit(() -> {
            pointInfos[0] = ForkJoinMockDataSupplier.getPoints();

            endLock.countDown();
        });
        executor.submit(() -> {
            goodInfos[0] = ForkJoinMockDataSupplier.getGoodInfo();

            endLock.countDown();
        });


        endLock.await();
        long endTime = System.currentTimeMillis();
        long millis = (endTime - startTime);

        System.out.println();
        System.out.println(Arrays.toString(userInfos));
        System.out.println(Arrays.toString(orderInfos));
        System.out.println(Arrays.toString(pointInfos));
        System.out.println(Arrays.toString(goodInfos));

        System.out.println(millis);

        executor.shutdown();
    }
}
