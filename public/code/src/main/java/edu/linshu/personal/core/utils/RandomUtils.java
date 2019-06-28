package edu.linshu.personal.core.utils;

import java.util.Random;

/**
 * @author Song Yu Tao 745698872@qq.com
 * @date 2019/06/27 11:34
 */
public class RandomUtils {

    public static int random(int max) {
        Random random = new Random();

        return random.nextInt(max);
    }


}
