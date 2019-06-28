package edu.linshu.personal.test.concurrent;

import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/18 19:30
 */
@SuppressWarnings("all")
public class ForkJoinTest {

    private static final ForkJoinPool executor;

    static {
        executor = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    @Test
    public void test() throws ExecutionException, InterruptedException, NoSuchMethodException {
        ForkJoinMockDataSupplier.UserInfo userInfo = null;
        ForkJoinMockDataSupplier.OrderInfo orderInfo = null;
        ForkJoinMockDataSupplier.PointInfo pointInfo = null;
        ForkJoinMockDataSupplier.GoodInfo goodInfo = null;

        long startTime = System.currentTimeMillis();

        LinkedList<Method> methods = new LinkedList<>();
        methods.add(ForkJoinMockDataSupplier.class.getDeclaredMethod("getUserInfo"));
        methods.add(ForkJoinMockDataSupplier.class.getDeclaredMethod("getOrderInfo"));
        methods.add(ForkJoinMockDataSupplier.class.getDeclaredMethod("getPoints"));
        methods.add(ForkJoinMockDataSupplier.class.getDeclaredMethod("getGoodInfo"));

        ForkJoinTask<List<Serializable>> task = executor.submit(
                new HttpGetTask<>(
                        methods,
                        0, 4
                )
        );

        List<Serializable> results = task.get();

        long endTime = System.currentTimeMillis();
        long millis = (endTime - startTime);

        System.out.println(results);
        System.out.println(millis);

        executor.shutdown();
    }

    private static class HttpGetTask<T> extends RecursiveTask<List<T>> {

        private final int start;
        private final int end;
        private List<Method> methods;

        public HttpGetTask(List<Method> methods, int start, int end) {
            this.methods = new ArrayList<>(methods);
            this.start = start;
            this.end = end;
        }

        @Override
        protected List<T> compute() {
            if (end - start > 2) {
                HttpGetTask<T> subTask1 = new HttpGetTask<>(methods.subList(start, end >> 1), start, end >> 1);
                subTask1.fork();

                HttpGetTask<T> subTask2 = new HttpGetTask<>(methods.subList(end >> 1, end), 0, end >> 1);
                subTask2.fork();

                ArrayList<T> results = new ArrayList<>();
                results.addAll(subTask1.join());
                results.addAll(subTask2.join());

                return results;
            }

            ArrayList<T> results = new ArrayList<>(methods.size());
            for (int i = start; i < end; i++) {
                Method method = methods.get(i);

                try {
                    T value = (T) method.invoke(null);

                    results.add(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return results;
        }
    }
}
