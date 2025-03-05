package erangel.loader;


import ch.qos.logback.core.spi.LifeCycle;
import erangel.Const;
import erangel.log.BaseLogger;
import erangel.webResource.FileSystemResourceContext;
import erangel.webResource.ResourceContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class WebAppClassLoader extends URLClassLoader implements LifeCycle {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(WebAppClassLoader.class);
    // 资源条目
    private final Map<String, ResourceEntry> entries = new HashMap<>();
    private String[] repositories = new String[0];
    private Path[] paths = new Path[0];
    // jar路径
    private String JarPath;
    // jar文件名数组
    private String[] JarNames = new String[0];
    // jar文件属性
    private Map<String, String> JarAttributes = new HashMap<>();
    // jar文件对象
    private String[] JarFiles = new String[0];
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
    public synchronized void addRepository(String repository, Path path) {
        if (repository == null) return;
        logger.debug("add repository: {}", repository);
        // 将新入仓库添加到内部记录中
        String[] newRepositories = new String[repositories.length + 1];
        System.arraycopy(repositories, 0, newRepositories, 0, repositories.length);
        newRepositories[repositories.length] = repository;
        repositories = newRepositories;

        Path[] newPaths = new Path[paths.length + 1];
        System.arraycopy(paths, 0, newPaths, 0, paths.length);
        newPaths[paths.length] = path;
        paths = newPaths;
        // 设置更新追踪
        ResourceContext tmp = new FileSystemResourceContext(path);
        try {
            List<Path> paths = tmp.listResources(path.toString());
            for (Path p : paths) {
                trackFiles(p);
            }
        } catch (IOException _) {
        }


    }

    private void trackFiles(Path path) {
        logger.debug("trackFiles: Path ：{} ， Name：{}",
                path, path.getFileName());
        String pathStr = path.toString();
        if (entries.containsKey(pathStr)) return;
        ResourceEntry entry = new ResourceEntry();
        entry.lastModified = path.toFile().lastModified();
        synchronized (entries) {
            entries.put(pathStr, entry);
        }
    }

    private void addRepository(String repository) {
        if (repository == null) return;
        try {
            URI uri = new URI(repository);
            URL url = uri.toURL();
            super.addURL(url);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }


    public synchronized void addJar(Path jarPath, JarFile jarFile, String jarFileName, long lastModified) throws IOException {
        if (jarFile == null ||
                jarFileName == null ||
                jarFileName.isEmpty()) return;
        logger.debug("add jar: jarName :{} ,lastModified : {}", jarFileName, lastModified);
        // 将JAR文件名全部放入String JarNames[]
        if (this.JarPath != null && jarFileName.startsWith(this.JarPath)) {
            while (jarFileName.startsWith(Const.commonCharacters.SOLIDUS)) {
                jarFileName = jarFileName.substring(1);
            }
            String[] namesArray = new String[JarNames.length + 1];
            System.arraycopy(JarNames, 0, namesArray, 0, JarNames.length);
            namesArray[JarNames.length] = jarFileName;
            JarNames = namesArray;
            JarAttributes.put(jarFileName, String.valueOf(lastModified));
        }
        if (!checkJar(jarPath)) return;
        // 将JAR的路径也添加到PATH数组中
        Path[] newJarPath = new Path[paths.length + 1];
        System.arraycopy(paths, 0, newJarPath, 0, paths.length);
        newJarPath[paths.length] = jarPath;
        paths = newJarPath;

    }

    // 检查Jar是否是敏感文件
    private boolean checkJar(Path jarPath) throws IOException {
        JarFile jarFile = new JarFile(jarPath.toFile());
        for (FilterClassNames filter : FilterClassNames.values()) {
            Class<?> clazz;
            String className = String.valueOf(filter);
            try {
                if (parentClassLoader != null)
                    clazz = parentClassLoader.loadClass(className);
                else clazz = Class.forName(className);
                // 设置更新追踪
                trackFiles(jarPath);
                if (!entries.containsKey(jarPath.toString())) {
                    entries.put(jarPath.toString(), new ResourceEntry());
                    entries.get(jarPath.toString()).loadedClass = clazz;
                    entries.get(jarPath.toString()).lastModified = jarPath.toFile().lastModified();
                    logger.debug("checkJar: add entry: {}", jarPath);
                }

            } catch (ClassNotFoundException e) {
                clazz = null;
            }
            if (clazz == null) continue;
            jarFile.close();
            return false;
        }
        return true;
    }

    /**
     * 加载类
     * 类加载过程的三个主要步骤：
     * 1. Loading（加载）
     * 2. Linking（链接）
     * ├─ Verification（验证）
     * ├─ Preparation（准备）
     * └─ Resolution（解析） ← resolve参数控制这一步
     * 3. Initialization（初始化）
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz;
        // 检查本地缓存，如果已经加载过则直接返回
        clazz = findLoadedClassFromMap(name);
        if (clazz != null) {
            logger.debug("loadClass: findLoadedClassFromMap: {}", clazz);
            if (resolve) resolveClass(clazz);
            return clazz;
        }
        // 检查JVM缓存，如果已经加载过则直接返回
        clazz = findLoadedClass(name);
        if (clazz != null) {
            logger.debug("loadClass: findLoadedClass: {}", clazz);
            if (resolve) resolveClass(clazz);
            return clazz;
        }
        // 使用系统类加载器加载，防止web程序的版本产生冲突
        try {
            clazz = appClassLoader.loadClass(name);
            entries.put(name, new ResourceEntry());
            entries.get(name).loadedClass = clazz;
            logger.debug("loadClass: appClassLoader.loadClass: {}", clazz);
            if (resolve) resolveClass(clazz);
            return clazz;
        } catch (ClassNotFoundException _) {
        }
        // 委托至父类加载器
        boolean delegated = delegate || classFilter(name);
        if (delegated) {
            ClassLoader loader = parentClassLoader;
            if (loader == null) loader = appClassLoader;
            logger.debug("loadClass: parentClassLoader: {}", loader);
            try {
                clazz = loader.loadClass(name);
                logger.debug("loadClass: parentClassLoader.loadClass: {}", clazz);
                if (resolve) resolveClass(clazz);
                return clazz;
            } catch (ClassNotFoundException _) {
            }
        }
        // 在本地仓库中寻找
        try {
            clazz = findClass(name);
            if (resolve) resolveClass(clazz);
            return clazz;
        } catch (ClassNotFoundException _) {
        }
        // 都找不到的话就跑出错误
        throw new ClassNotFoundException(name);
    }

    /**
     * 在本地资源条目映射中搜索已加载的类。
     *
     * @param name 要搜索的类的二进制名称。
     * @return 如果找到，则返回已加载的 Class 对象；
     * 如果类不在条目映射中或相应的 ResourceEntry 没有加载的类，则返回 null。
     */
    private Class<?> findLoadedClassFromMap(String name) {
        if (entries.containsKey(name)) {
            ResourceEntry entry = entries.get(name);
            if (entry.loadedClass != null) {
                return entry.loadedClass;
            }
        }
        return null;
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

    public boolean modified() throws IOException {
        return false;
    }

    public void start() {
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
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
    //<editor-fold desc = "内部类">
    // 资源条目
    public static class ResourceEntry {
        private volatile Class<?> loadedClass = null;
        private long lastModified;
    }

    //</editor-fold>
    //<editor-fold desc = "only for test">
    public String[] getRepositories() {
        return repositories;
    }

    public void setRepositories(String[] repositories) {
        this.repositories = repositories;
    }

    public Map<String, ResourceEntry> getEntries() {
        return entries;
    }

    public Path[] getPaths() {
        return paths;
    }

    public void setPaths(Path[] paths) {
        this.paths = paths;
    }

    public String[] getJarNames() {
        return JarNames;
    }

    public void setJarNames(String[] jarNames) {
        JarNames = jarNames;
    }

    public Map<String, String> getJarAttributes() {
        return JarAttributes;
    }

    public void setJarAttributes(Map<String, String> jarAttributes) {
        JarAttributes = jarAttributes;
    }

    public String[] getJarFiles() {
        return JarFiles;
    }

    public void setJarFiles(String[] jarFiles) {
        JarFiles = jarFiles;
    }

    public ClassLoader getAppClassLoader() {
        return appClassLoader;
    }

    public void setAppClassLoader(ClassLoader appClassLoader) {
        this.appClassLoader = appClassLoader;
    }

    public ClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public void setParentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
