package edu.linshu.personal.core.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/05/24 14:19
 */
public class UnsafeUtils {
    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}
