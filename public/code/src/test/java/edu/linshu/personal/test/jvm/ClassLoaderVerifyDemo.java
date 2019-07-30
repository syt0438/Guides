package edu.linshu.personal.test.jvm;

/**
 * @author Linshu 745698872@qq.com
 * @date 2019/7/30 20:46
 */
public class ClassLoaderVerifyDemo {

    private static final String STATIC_FIELD_1 = "static_field_1";
    private static final String STATIC_FIELD_2 = getStaticField2();

    static {
        System.out.println(STATIC_FIELD_1);
        System.out.println(STATIC_FIELD_2);

        System.out.println("static_load_1");
        System.out.println("handler 33333333333");
    }


    private static String getStaticField2() {
        return "static_field_2";
    }

}
