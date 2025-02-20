package erangel.loader;

import java.net.URL;
import java.net.URLClassLoader;

public class WebAppClassLoader extends URLClassLoader {
    // TODO
    public WebAppClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
    }
}
