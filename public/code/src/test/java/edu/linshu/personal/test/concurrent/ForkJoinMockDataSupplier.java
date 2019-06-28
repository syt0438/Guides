package edu.linshu.personal.test.concurrent;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/18 20:58
 */
class ForkJoinMockDataSupplier {

    static UserInfo getUserInfo() {
        try {
            TimeUnit.SECONDS.sleep(3);

            System.out.println("user finished");

            return new UserInfo("linshu");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static OrderInfo getOrderInfo() {
        try {
            TimeUnit.SECONDS.sleep(3);

            System.out.println("order finished");
            return new OrderInfo(1222.32D);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static PointInfo getPoints() {
        try {
            TimeUnit.SECONDS.sleep(3);

            System.out.println("points finished");
            return new PointInfo(1777L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static GoodInfo getGoodInfo() {
        try {
            TimeUnit.SECONDS.sleep(3);

            System.out.println("good finished");
            return new GoodInfo("真丝睡衣", 778.5D);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class OrderInfo implements Serializable {
        private BigDecimal totalPrice;

        OrderInfo(Double totalPrice) {
            this.totalPrice = new BigDecimal(totalPrice);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", OrderInfo.class.getSimpleName() + "[", "]")
                    .add("totalPrice=" + totalPrice)
                    .toString();
        }
    }

    static class UserInfo implements Serializable {
        private String uid;

        UserInfo(String uid) {
            this.uid = uid;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", UserInfo.class.getSimpleName() + "[", "]")
                    .add("uid='" + uid + "'")
                    .toString();
        }
    }

    static class PointInfo implements Serializable {
        private Long points;

        PointInfo(Long points) {
            this.points = points;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", PointInfo.class.getSimpleName() + "[", "]")
                    .add("points=" + points)
                    .toString();
        }
    }

    static class GoodInfo implements Serializable {
        private String goodName;
        private BigDecimal price;

        GoodInfo(String goodName, double price) {
            this.goodName = goodName;
            this.price = new BigDecimal(price);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", GoodInfo.class.getSimpleName() + "[", "]")
                    .add("goodName='" + goodName + "'")
                    .add("price=" + price)
                    .toString();
        }
    }
}
