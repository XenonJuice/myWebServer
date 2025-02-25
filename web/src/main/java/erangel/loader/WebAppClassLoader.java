package erangel.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class WebAppClassLoader extends URLClassLoader {
    // TODO
    public WebAppClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
    }
    public static void main(String[] args) {
        String fullyQualifiedName = WebAppClassLoader.class.getName();
        System.out.println("当前类的全限定名：" + fullyQualifiedName);

        // 获取当前方法的名称
        String currentMethodName = new Throwable().getStackTrace()[0].getMethodName();
        System.out.println("当前方法的名称：" + currentMethodName);

        // 获取当前方法所在类的全限定名
        String currentClassName = new Throwable().getStackTrace()[0].getClassName();
        System.out.println("当前方法所在类的全限定名：" + currentClassName);
    }
    public WebAppClassLoader() {
        super(new URL[0]);
    }
    public WebAppClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }


}
