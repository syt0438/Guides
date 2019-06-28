package edu.linshu.personal.test.concurrent;

import edu.linshu.personal.core.concurrent.CustomThreadPoolExecutor;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/18 19:30
 */
@SuppressWarnings("all")
public class ForkJoinCustomFutureTaskDemo {

    private static final ThreadPoolExecutor executor;

    static {
        executor = new CustomThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() >> 1,
                Runtime.getRuntime().availableProcessors(),
                6,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(
                        Runtime.getRuntime().availableProcessors() << 3
                ),
                (ThreadFactory) Thread::new
        );
    }

    @Test
    public void test(String[] args) throws ExecutionException, InterruptedException {
        ForkJoinMockDataSupplier.UserInfo userInfo = null;
        ForkJoinMockDataSupplier.OrderInfo orderInfo = null;
        ForkJoinMockDataSupplier.PointInfo pointInfo = null;
        ForkJoinMockDataSupplier.GoodInfo goodInfo = null;

        long startTime = System.currentTimeMillis();

        Future<ForkJoinMockDataSupplier.UserInfo> userInfoFuture = executor.submit(() -> {
            return ForkJoinMockDataSupplier.getUserInfo();
        });
        Future<ForkJoinMockDataSupplier.OrderInfo> orderInfoFuture = executor.submit(() -> {
            return ForkJoinMockDataSupplier.getOrderInfo();
        });
        Future<ForkJoinMockDataSupplier.PointInfo> pointInfoFuture = executor.submit(() -> {
            return ForkJoinMockDataSupplier.getPoints();
        });
        Future<ForkJoinMockDataSupplier.GoodInfo> goodInfoFuture = executor.submit(() -> {
            return ForkJoinMockDataSupplier.getGoodInfo();
        });

        userInfo = userInfoFuture.get();
        orderInfo = orderInfoFuture.get();
        pointInfo = pointInfoFuture.get();
        goodInfo = goodInfoFuture.get();


        long endTime = System.currentTimeMillis();
        long millis = (endTime - startTime);

        System.out.println();
        System.out.println(userInfo);
        System.out.println(orderInfo);
        System.out.println(pointInfo);
        System.out.println(goodInfo);

        System.out.println(millis);

        executor.shutdown();
    }
}
