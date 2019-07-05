package edu.linshu.personal.core.net.netty.bean;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/07/05 16:19
 */
public class UnixTime implements Serializable {

    private final long value;

    public UnixTime() {
        this(System.currentTimeMillis() / 1000L + 2208988800L);
    }

    public UnixTime(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return new Date((value() - 2208988800L) * 1000L).toString();
    }

}
