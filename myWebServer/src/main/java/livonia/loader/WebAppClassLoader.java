package livonia.loader;


import livonia.base.Const;
import livonia.lifecycle.Lifecycle;
import livonia.lifecycle.LifecycleListener;
import livonia.log.BaseLogger;
import livonia.resource.LocalResource;
import livonia.resource.ResourceManager;
import livonia.utils.LifecycleHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static livonia.base.Const.commonCharacters.SOLIDUS;
import static livonia.base.Const.webApp.DOTCLASS;

public class WebAppClassLoader extends URLClassLoader implements Lifecycle {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(WebAppClassLoader.class);

    static {
        System.out.println("WebAppClassLoader init");
        registerAsParallelCapable();
    }

    // 资源条目
    private final Map<String, ResourceEntry> entries = new ConcurrentHashMap<>();
    // jar文件修改时刻
    private final Map<String, Long> jarTimes = new ConcurrentHashMap<>();
    // 本地仓库URL集合
    private final List<URL> localRepositories = new ArrayList<>();
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    // 本地资源对象
    private ResourceManager localResource = null;
    // 父类已被添加URL标志位
    private boolean extRepo = false;
    // 系统类加载器
    private ClassLoader sysLoader = null;
    // 父类加载器
    private ClassLoader parentClassLoader;
    // 标准委托模型标志位
    private boolean delegate;
    // 线程启动标志位
    private boolean started;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public WebAppClassLoader() {
        super(new URL[0]);
        if (getParent() == null)
            this.parentClassLoader = getSystemClassLoader();
        this.sysLoader = getSystemClassLoader();
    }

    public WebAppClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
        if (getParent() == null)
            this.parentClassLoader = getSystemClassLoader();
        this.sysLoader = getSystemClassLoader();
    }

    @SafeVarargs
    public static Enumeration<URL> combineURLEnumerations(Enumeration<URL>... enumerations) {
        List<URL> combined = new ArrayList<>();
        for (Enumeration<URL> e : enumerations) combined.addAll(Collections.list(e));
        return Collections.enumeration(combined);
    }

    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public ResourceManager getLocalResource() {
        return localResource;
    }

    public void setLocalResource(ResourceManager localResource) {
        this.localResource = localResource;
    }

    public boolean isDelegate() {
        return delegate;
    }

    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">
    // 记录文件的修改时间
    public void recordModifiedTime(String path, LocalResource localResource) {
        if (entries.containsKey(path)) return;
        ResourceEntry entry = new ResourceEntry();
        entry.lastModified = localResource.getLastModified();
        synchronized (entries) {
            if (!entries.containsKey(path)) entries.put(path, entry);
        }

    }

    // 检查MAP
    private Class<?> findLoadedClassFromMap(String name) {
        if (!started) throw new IllegalStateException("WebAppClassLoader has not been started");
        String path = changeNameToPath(name, true);
        ResourceEntry entry = entries.get(path);
        if (entry != null) return entry.loadedClass;
        return null;
    }

    // 检查本地磁盘，在此之前由于可能被其他线程已经加载到MAP中，由此再次检查MAP
    private Class<?> findClassFromLocal(String name) {
        if (name == null) return null;
        LocalResource loadableResource = null;
        String path = changeNameToPath(name, true);
        ResourceEntry entry = entries.get(path);

        // 1.检查MAP中是否有保存过该类的类对象的引用，没有的情况下从本地磁盘中试图获取
        //　如果当前内部缓存中没有该对象，则从磁盘中获取
        if (entry == null) {
            loadableResource = localResource.getLoaderResource(path);
            // 本地磁盘目录中也不存在 则返回null
            if (!loadableResource.exists()) return null;
            // 找到资源，则创建一个ResourceEntry存入到缓存中
            entry = new ResourceEntry();
            entry.lastModified = loadableResource.getLastModified();
            // 并发Map也无法保证跨操作的线程安全
            // 如果这时有另一线程试图向Map中添加特定的类，无锁状态下
            // 这里可能获得与被添加的特定类的实例不同的实例
            synchronized (entries) {
                // 确定在此操作之前是否由其他线程从此MAP中添加了实例
                ResourceEntry oldEntry = entries.get(path);
                // 如果未被添加，则添加到MAP中
                if (oldEntry == null) {
                    entries.put(path, entry);
                    // 如果已存在，则将获取的对象指向已经在MAP中存在的entry
                } else {
                    entry = oldEntry;
                }
            }
        }
        // 2.检查MAP后，有保存过该类的类对象的引用，试图直接从MAP获取类对象
        // 获取刚刚得到的对象的类实例
        Class<?> clazz = entry.loadedClass;
        if (clazz != null) return clazz;
        // 若获取到的MAP中的类对象为空,说明在进行到这一环节之前由其他线程修改了Map中本次获取到类的实例
        // 再次试图从本地磁盘加载内容
        synchronized (getClassLoadingLock(name)) {
            clazz = entry.loadedClass;
            if (clazz != null) return clazz;
            // 确认本地磁盘对象的状态
            if (loadableResource == null) loadableResource = localResource.getLoaderResource(path);
            // 若本地磁盘中字眼不存在，则返回null
            if (!loadableResource.exists()) return null;
            // 获取字节对象
            byte[] bytes;
            bytes = loadableResource.getContent();
            // 处理包名
            // String packageName = null;
            // int dotPos = name.lastIndexOf(DOT);
            // if (dotPos!=-1) packageName = name.substring(0, dotPos);
            //                        Package pkg;
            // // 若截取后的包名不为空，则获取指定包的信息对象
            // if (packageName!=null){
            //                pkg =getDefinedPackage(packageName);
            //                if (pkg == null){
            //                    definePackage(packageName, null, null, null,
            //                            null, null, null, null);
            //                    pkg=getDefinedPackage(packageName);
            //                }
            // }
            // defineClass
            clazz = defineClass(name, bytes, 0, bytes.length, (CodeSource) null);
            entry.loadedClass = clazz;
        }
        return clazz;
    }

    // resource ：com.example.MyClass
    // true ：/com/example/MyClass.class
    // false ：com/example/MyClass.class
    private String changeNameToPath(String name, boolean useSolidus) {
        StringBuilder path = new StringBuilder(15);
        if (useSolidus) {
            path.append(SOLIDUS);
        }
        path.append(name.replace(Const.commonCharacters.DOT,
                SOLIDUS));
        path.append(DOTCLASS);
        return path.toString();
    }

    // resource ：com/example/MyClass 🫱/com/example/MyClass.class
    private String nameToPath(String name) {
        if (name.startsWith(SOLIDUS)) return name;
        return SOLIDUS + name;
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

    // 检查web资源是否有更新
    public boolean modified() {
        logger.debug("检查web资源是否有更新，当前已被加载的类的总数为: {}", entries.size());
        for (Map.Entry<String, ResourceEntry> entry : entries.entrySet()) {
            long oldTime = entry.getValue().lastModified;
            long newTime = localResource.getClassResource(entry.getKey()).getLastModified();
            if (oldTime != newTime) {
                logger.debug("监测到web资源变化: {} , oldTime: {} , newTime: {}",
                        entry.getKey(), new Date(oldTime), new Date(newTime));
                return true;
            }
        }
        LocalResource[] jars = localResource.getAllLibResources();
        int jarsSize = jars.length;
        for (LocalResource jar : jars) {
            if (jar.getName().endsWith(Const.webApp.DOTJAR) && jar.canRead()) {
                Long oldTime = jarTimes.get(jar.getName());
                if (oldTime == null) {
                    logger.info("webAppClassLoader 检查更新 ：jar has been added: {} in Context : {}", jar.getName(), localResource.getContext().getName());
                } else if (oldTime != jar.getLastModified()) {
                    logger.info("webAppClassLoader 检查更新 ：jar has been modified: {} in Context : {}", jar.getName(), localResource.getContext().getName());
                    return true;
                }

            }
        }
        if (jarsSize < jarTimes.size()) {
            logger.info("webAppClassLoader modified ：jar has been deleted: {} ", localResource.getContext().getName());
            return true;
        }
        return false;
    }

    //</editor-fold>
    //<editor-fold desc = "only for test">
    public Map<String, ResourceEntry> getEntries() {
        return entries;
    }


    public ClassLoader getSysLoader() {
        return sysLoader;
    }

    public void setSysLoader(ClassLoader cl) {
        this.sysLoader = cl;
    }

    public ClassLoader getParentClassLoader() {
        return parentClassLoader;
    }

    public void setParentClassLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    /**
     * 通过名称加载指定的类。该实现使用委托模型，
     * 尝试从多个来源来加载类，例如本地缓存、
     * JVM 缓存、系统类加载器和父类加载器。如果这些都无法成功，
     * 则抛出 ClassNotFoundException。
     *
     * @param name 要加载的类的完全限定名。
     * @return 请求的类名的结果 Class 对象。
     * @throws ClassNotFoundException 如果无法通过任何机制找到类。
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz;
            if (!started) throw new IllegalStateException("WebAppClassLoader has not been started");
            // 检查MAP
            clazz = findLoadedClassFromMap(name);
            if (clazz != null) {
                logger.debug("loadClass: findLoadedClassFromMap: {}", clazz);
                if (resolve) resolveClass(clazz);
                return clazz;
            }

            // 检查JVM，如果已经加载过则直接返回
            clazz = findLoadedClass(name);
            if (clazz != null) {
                logger.debug("loadClass: findLoadedClass: {}", clazz);
                if (resolve) resolveClass(clazz);
                return clazz;
            }

            // 使用系统类加载器加载，防止web程序的版本产生冲突
            // 这里如果是web程序的用户自定义类的话就会忽略
            boolean loadingFromSysLoader = false;
            String resourceName = changeNameToPath(name, false);
            URL url = sysLoader.getResource(resourceName);
            if (url != null) loadingFromSysLoader = true;
            if (loadingFromSysLoader) {
                try {
                    clazz = sysLoader.loadClass(name);
                    // logger.debug("loadClass:{} sysLoader.loadClass: {}",sysLoader.toString(), clazz);
                    if (resolve) resolveClass(clazz);
                    return clazz;
                } catch (ClassNotFoundException _) {
                }
            }

            // 委托至父类加载器
            boolean delegated = delegate || classFilter(name);
            if (delegated) {
                ClassLoader loader = parentClassLoader;
                if (loader == null) loader = sysLoader;
                logger.debug("loadClass: parentClassLoader: {}", loader);
                try {
                    clazz = loader.loadClass(name);
                    logger.debug("loadClass: parentClassLoader.loadClass: {}", clazz);
                    if (clazz != null) {
                        if (resolve) resolveClass(clazz);
                        return clazz;
                    }
                } catch (ClassNotFoundException _) {
                }
            }

            // 检查本地仓库
            clazz = this.findClass(name);
            if (clazz != null) {
                // logger.debug("loadClass: findLoadedClassFromLocal: {}", clazz);
                if (resolve) resolveClass(clazz);
                return clazz;
            }
        }
        // 都找不到的话就跑出错误
        throw new ClassNotFoundException(name);
    }

    // 检查本地仓库
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz;
        clazz = findClassFromLocal(name);
        // 本地仓库找不到时，试图从调用父类的findClass从addURL添加后的资源中寻找
        if (clazz == null && extRepo) clazz = super.findClass(name);
        if (clazz == null) {
            logger.error("findClass: super.findClass failed: {}", name);
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    //</editor-fold>
    //<editor-fold desc = "重写父类的部分方法">
    @Override
    public URL findResource(String name) {
        if (!started) throw new IllegalStateException("WebAppClassLoader has not been started");
        logger.debug("findResource: {}", name);
        URL url = null;
        String resourceName = nameToPath(name);
        LocalResource resource = localResource.getLoaderResource(resourceName);
        if (resource.exists()) {
            url = resource.getURL();
            recordModifiedTime(resourceName, resource);
        }
        if (url == null && extRepo) url = super.findResource(name);
        return url;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        logger.debug("findResources: {}", name);
        Set<URL> urls = new LinkedHashSet<>();
        String resourceName = nameToPath(name);
        LocalResource[] resource = localResource.getLoaderResources(resourceName);
        for (LocalResource res : resource) {
            if (res.exists()) urls.add(res.getURL());
        }
        if (extRepo) {
            Enumeration<URL> superUrls = super.findResources(name);
            while (superUrls.hasMoreElements()) {
                urls.add(superUrls.nextElement());
            }
        }
        return Collections.enumeration(urls);
    }

    @Override
    public URL getResource(String name) {
        if (!started) throw new IllegalStateException("WebAppClassLoader has not been started");
        URL url = null;
        logger.debug("getResource: {}", name);
        // 委托给父类或者为敏感类时
        if (delegate || classFilter(name)) {
            ClassLoader loader = parentClassLoader;
            if (loader == null) loader = sysLoader;
            url = loader.getResource(name);
            if (url != null) return url;
        }
        // 从本地仓库寻找
        url = findResource(name);
        return url;
        // 都找不到时返回空
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        logger.debug("getResources: {}", name);
        Enumeration<URL> parentResources = getParent().getResources(name);
        Enumeration<URL> localResources = findResources(name);
        // 合并枚举
        boolean delegated = delegate || classFilter(name);
        if (delegated) {
            return combineURLEnumerations(parentResources, localResources);
        } else {
            return combineURLEnumerations(localResources, parentResources);
        }
    }

    @Override
    protected void addURL(URL url) {
        logger.debug("addURL: {}", url);
        super.addURL(url);
        extRepo = true;
    }

    @Override
    public URL[] getURLs() {
        logger.debug("getURLs: {}", Arrays.toString(super.getURLs()));
        ArrayList<URL> result = new ArrayList<>();
        result.addAll(localRepositories);
        result.addAll(Arrays.asList(super.getURLs()));
        return result.toArray(new URL[0]);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (!started) throw new IllegalStateException("WebAppClassLoader has not been started");
        logger.debug("getResourceAsStream: {}", name);
        InputStream inputStream = null;
        boolean delegated = delegate || classFilter(name);
        // 委托给父类
        if (delegated) {
            ClassLoader loader = parentClassLoader;
            if (loader == null) loader = sysLoader;
            inputStream = loader.getResourceAsStream(name);
            return inputStream;
        }
        // 寻找本地仓库
        String resourceName = nameToPath(name);
        LocalResource resource = localResource.getLoaderResource(resourceName);
        if (resource.exists()) {
            inputStream = resource.getInputStream();
            recordModifiedTime(resourceName, resource);
        }
        if (extRepo && inputStream == null) {
            URL url = super.getResource(name);
            if (url != null) {
                try {
                    inputStream = url.openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return inputStream;
            }
        }
        // 都找不到的话就返回null
        return null;
    }

    //</editor-fold>
    //<editor-fold desc = "生命周期">
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        // 不做实现
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        // 不做实现
    }

    @Override
    public LifecycleListener[] findLifecycleListener() {
        // 不做实现
        return null;
    }

    @Override
    public void start() {
        lifecycleHelper.fireLifecycleEvent(BEFORE_START_EVENT, null);
        LocalResource[] classes = localResource.getAllClassesResources();
        for (LocalResource clazz : classes) {
            if (clazz.isDirectory() && clazz.canRead()) localRepositories.add(clazz.getURL());
        }
        LocalResource[] jars = localResource.getAllLibResources();
        for (LocalResource jar : jars) {
            if (jar.getName().endsWith(Const.webApp.DOTJAR) && jar.canRead()) {
                localRepositories.add(jar.getURL());
                jarTimes.put(jar.getName(), jar.getLastModified());
            }
        }
        started = true;
        lifecycleHelper.fireLifecycleEvent(START_EVENT, null);
    }

    @Override
    public void stop() {
        entries.clear();
        jarTimes.clear();
        localRepositories.clear();
        localResource = null;
        started = false;
        lifecycleHelper.fireLifecycleEvent(STOP_EVENT, null);
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
}
