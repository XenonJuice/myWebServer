package livonia.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class testLoader {
    public static void main(String[] args) {
        URL[] urls = new URL[0];
        URLClassLoader loader = new URLClassLoader(urls);
        System.out.println("==========================================");
        System.out.println(loader.getClass().getClassLoader());
        System.out.println("==========================================");
        System.out.println(loader.getParent());
        System.out.println("==========================================");
        System.out.println(loader.getParent().getParent());
        System.out.println("==========================================");
        System.out.println(loader.getParent().getParent().getParent());
        System.out.println("==========================================");
    }
}
