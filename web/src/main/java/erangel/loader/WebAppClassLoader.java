package erangel.loader;


import erangel.Const;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;

public class WebAppClassLoader extends URLClassLoader {
    //<editor-fold desc = "attr">
    private String repository;
    private String JarPath;
    private ClassLoader appClassLoader;
    private ClassLoader parentClassLoader;
    private boolean delegate;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    // TODO
    public WebAppClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
        this.parentClassLoader = parent.getParent();
        this.appClassLoader = getSystemClassLoader();
    }

    public WebAppClassLoader() {
        super(new URL[0]);
        this.appClassLoader = getSystemClassLoader();
    }

    public WebAppClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        this.parentClassLoader = parent.getParent();
        this.appClassLoader = getSystemClassLoader();
    }

    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public boolean isDelegate() {
        return delegate;
    }

    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    public String getJarPath() {
        return this.JarPath;
    }

    public void setJarPath(String jarPath) {
        this.JarPath = jarPath;
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">
    protected synchronized void addRepository(String repository, Path path) {
        this.repository = repository;
    }

    protected synchronized void addJar(JarFile jarFile) {
    }

    /**
     * 类加载过程的三个主要步骤：
     * 1. Loading（加载）
     * 2. Linking（链接）
     * ├─ Verification（验证）
     * ├─ Preparation（准备）
     * └─ Resolution（解析） ← resolve参数控制这一步
     * 3. Initialization（初始化）
     */
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz;
        // 1.检查JVM，如果已经加载过则直接返回
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve) resolveClass(clazz);
            return clazz;
        }
        // 2.使用系统类加载器加载，防止web程序的版本产生冲突
        try {
            clazz = appClassLoader.loadClass(name);
            if (resolve) resolveClass(clazz);
            return clazz;
        } catch (ClassNotFoundException _) {
        }

        // 3.委托至父类加载器
        boolean delegated = delegate || classFilter(name);
        if (delegated) {
            ClassLoader loader = parentClassLoader;
            if (loader == null) loader = appClassLoader;
            try {
                clazz = loader.loadClass(name);
                if (resolve) resolveClass(clazz);
                return clazz;
            } catch (ClassNotFoundException _) {
            }
        }
        // 4.在本地仓库中寻找
        try {
            clazz = findClass(name);
            if (resolve) resolveClass(clazz);
            return clazz;
        } catch (ClassNotFoundException _) {
        }

        // 都找不到的话就跑出错误
        throw new ClassNotFoundException(name);
    }

    // 过滤掉特殊类
    private boolean classFilter(String name) {
        String packageName = "";
        int index = name.lastIndexOf(Const.commonCharacters.DOT);
        if (index != -1) {
            packageName = name.substring(0, index);
        } else return false;
        for (FilterPackageNames filter : FilterPackageNames.values()) {
            if (packageName.startsWith(filter.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    // 在本地仓库中查找class
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz;
        synchronized (this) {
            clazz = findLoadedClass(name);
            if (clazz != null) return clazz;
            clazz = super.findClass(name);
        }
        if (clazz == null) throw new ClassNotFoundException(name);
        return clazz;
    }

    public boolean modified() {
        return false;
    }
    //</editor-fold>


    //<editor-fold desc = "敏感类，包">
    // 要被过滤掉的包名前缀
    private enum FilterPackageNames {
        PACKAGE_JAVAX("javax");
        private final String packageName;

        FilterPackageNames(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    // 要被过滤掉的具体类名
    private enum FilterClassNames {
        CLASS_SERVLET("javax.servlet.Servlet");
        private final String className;

        FilterClassNames(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }
    }
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
