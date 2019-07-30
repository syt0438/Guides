package edu.linshu.personal.test.jvm;

import lombok.extern.log4j.Log4j2;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

/**
 * @author Linshu 745698872@qq.com
 * @date 2019/7/30 20:44
 */
@Log4j2
public class ClassLoaderMechanismTest {
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, InterruptedException {

        URL url = new URL("file:D:\\");

        URLClassLoader classloader = new URLClassLoader(new URL[]{url}, null);

        while (true) {

            Class<?> clazz = classloader.loadClass("edu.linshu.personal.test.jvm.ClassLoaderVerifyDemo");
            Object bean = clazz.getConstructor().newInstance();

            ClassLoader classLoader = clazz.getClassLoader();

            try {
                log.info("测试类的类加载器为: [{}]", classLoader);
            } catch (NullPointerException e) {
                log.info("测试类的类加载器为: null");
            }
            try {
                log.info("测试类的父类加载器为: [{}]", classLoader.getParent());
            } catch (NullPointerException e) {
                log.info("测试类的父类加载器为: null");
            }
            try {
                log.info("测试类的父类加载器的父类加载器为: [{}]", classLoader.getParent().getParent());
            } catch (NullPointerException e) {
                log.info("测试类的父类加载器的父类加载器为: null");
            }
            try {
                log.info("测试类的父类加载器的父类加载器的父类加载器为: [{}]", classLoader.getParent().getParent().getParent());
            } catch (NullPointerException e) {
                log.info("测试类的父类加载器的父类加载器的父类加载器为: null");
            }

            bean = null;
//            classLoader = null;

            System.gc();

            TimeUnit.SECONDS.sleep(5);
        }
    }
}
