package erangel.Resource;

import erangel.*;
import erangel.log.BaseLogger;
import erangel.utils.LifecycleHelper;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static erangel.Const.commonCharacters.*;
import static erangel.Const.webApp.*;

public class ResourceManager implements Lifecycle, Runnable {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(ResourceManager.class);
    // 本地资源映射
    private final Map<String, List<LocalResource>> resourceMap = new HashMap<>();
    private final Map<String, LocalResource> propMap = new HashMap<>();
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    // 根目录
    private String basePath = null;
    // 线程
    private Thread thread = null;
    private String threadName = null;
    // 绑定的Context
    private Context context = null;
    // 组件启动标志位
    private boolean isStarted = false;

    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    //</editor-fold>
    //<editor-fold desc = "扫描资源">
    // 始终以斜杠开头 例如/com/example/LLJ.class  /web.xml

    // 扫描WEB-INF/classes
    public void scanFileSystem(Path rootDir) {
        // 递归遍历整个目录树
        try {
            Files.walk(rootDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relative = rootDir.relativize(path).toString().replace(
                                File.separatorChar, '/');
                        String resourcePath = SOLIDUS+ relative;
                        // 构造 FileResource 对象
                        LocalResource resource = new FileResource(path);
                        addResource(resourcePath, resource);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    // 扫描 jar 文件
    public void scanJarFile(File jarFile) {
        JarFile jf = null;
        try {
            jf = new JarFile(jarFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = entries.nextElement();
            if (!je.isDirectory()) {
                String resourcePath = normalizePath(je.getName());
                LocalResource resource = new JarResource(jf, je);
                addResource(resourcePath, resource);
            }
        }
    }


    // 扫描 配置文件
    public void scanAdditionalResources(Path rootPath) throws IOException {
        // 遍历整个根目录
        Files.walk(rootPath, 1)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    // 判断是否为常用的 web 配置文件或属性文件
                    if (fileName.equalsIgnoreCase(WEB_XML) ||
                            fileName.equalsIgnoreCase("context.xml") ||
                            fileName.endsWith(DOTPROP)) {
                        // 统一资源路径
                        String resourcePath = "/" + rootPath.relativize(path)
                                .toString()
                                .replace(File.separatorChar, '/');
                        logger.debug("Found resource: {}", resourcePath);
                        // 将扫描到的资源信息存入 propMap
                        propMap.put(resourcePath, new FileResource(path));
                    }
                });
    }

    //</editor-fold>
    //<editor-fold desc = "获取资源">
    public LocalResource getLoaderResource(String path) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> resources = resourceMap.get(normalizedPath);
        if (resources != null) {
            for (LocalResource res : resources) {
                if (res.exists()) {
                    return res;
                }
            }
        }
        return null;
    }

    public LocalResource[] getLoaderResources(String path) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> resources = resourceMap.get(normalizedPath);
        if (resources == null) {
            return new LocalResource[0];
        }
        List<LocalResource> result = new ArrayList<>();
        for (LocalResource res : resources) {
            if (res.exists()) {
                result.add(res);
            }
        }
        return result.toArray(new LocalResource[0]);
    }

    public LocalResource getPropResource(String path) {
        return propMap.get(path);
    }

    private String normalizePath(String path) {
        if (!path.startsWith(SOLIDUS)) path = SOLIDUS + path;
        return path;
    }

    public void addResource(String path, LocalResource resource) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> list = resourceMap.get(normalizedPath);
        if (list == null) {
            list = new ArrayList<>();
            resourceMap.put(normalizedPath, list);
        }
        list.add(resource);
    }

    //</editor-fold>
    //<editor-fold desc = "创建资源映射">
    private void createResourceMapping() {
        // 构造 classes 和 lib 目录的真实路径
        String innerBase = basePath + WEB_INF;
        Path webInfRoot = Path.of(innerBase);
        Path classesDir = webInfRoot.resolve(CLASSES_ONLY);
        Path libDir = webInfRoot.resolve(LIB_ONLY);
        // 扫描WEB-INF/classes下的所有 class 文件
        scanFileSystem(classesDir);
        // 扫描 WEB-INF/lib 下的所有 jar 文件
        try (DirectoryStream<Path> jarStream = Files.newDirectoryStream(libDir, "*.jar")) {
            for (Path jarPath : jarStream) {
                File jarFile = jarPath.toFile();
                scanJarFile(jarFile);
                logger.debug("Scanned jar file: {}", jarFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to scan jar files in WEB-INF/lib", e);
        }
    }
    //<editor-fold desc = "扫描WEB-INF/classes">
    /**
     * 获取WEB-INF/classes目录下所有文件，并以LocalResource数组形式返回
     *
     * @return 包含所有文件的LocalResource数组
     */
    public LocalResource[] getAllClassesResources() {
        // 确保basePath被设置
        if (basePath == null) {
            logger.error("basePath未设置，无法获取classes资源");
            return new LocalResource[0];
        }
        Path classesDir = Path.of(basePath + CLASSES);
        // 检查目录是否存在
        if (!Files.exists(classesDir) || !Files.isDirectory(classesDir)) {
            logger.warn("未找到WEB-INF/classes目录: {}", classesDir);
            return new LocalResource[0];
        }
        try (var stream = Files.walk(classesDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(FileResource::new)
                    .toArray(LocalResource[]::new);
        } catch (IOException e) {
            logger.error("遍历WEB-INF/classes时出错", e);
            return new LocalResource[0];
        }
    }
    //</editor-fold>

    //<editor-fold desc = "扫描WEB-INF/lib">
    /**
     * 获取WEB-INF/lib目录下所有JAR文件，并以LocalResource数组形式返回
     *
     * @return 包含所有JAR文件的LocalResource数组
     */
    public LocalResource[] getAllLibResources()  {
        // 确保basePath被设置
        if (basePath == null) {
            logger.error("basePath未设置，无法获取lib资源");
            return new LocalResource[0];
        }
        Path libDir = Path.of(basePath + Const.webApp.LIB);
        // 检查目录是否存在
        if (!Files.exists(libDir) || !Files.isDirectory(libDir)) {
            logger.warn("未找到WEB-INF/lib目录: {}", libDir);
            return new LocalResource[0];
        }
        try (var stream = Files.list(libDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(Const.webApp.DOTJAR))
                    .map(FileResource::new)
                    .toArray(LocalResource[]::new);
        } catch (IOException e) {
            logger.error("遍历WEB-INF/lib时出错", e);
            return new LocalResource[0];
        }
    }
    //</editor-fold>
    //</editor-fold>
    //<editor-fold desc = "生命周期">
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {

    }

    @Override
    public LifecycleListener[] findLifecycleListener() {
        return new LifecycleListener[0];
    }

    @Override
    public void start() throws LifecycleException {
        if (isStarted) throw new LifecycleException("ResourceManager is already started");
        logger.debug("LifeCycle : ResourceManager is starting");
        lifecycleHelper.fireLifecycleEvent(START_EVENT, null);
        isStarted = true;
        threadStart();
    }

    private void threadStart() {
        logger.debug("ResourceManager is starting");
        threadName = this.getClass().getSimpleName();
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }


    @Override
    public void stop() throws LifecycleException {
        if (!isStarted) throw new LifecycleException("ResourceManager is not started");
        logger.debug("LifeCycle : ResourceManager is stopping");
        lifecycleHelper.fireLifecycleEvent(STOP_EVENT, null);
        isStarted = false;
        threadStop();
        logger.debug("LifeCycle : ResourceManager is stopped");
    }

    private void threadStop() {
        if (thread == null) return;
        logger.debug("ResourceManager is stopping");
        thread = null;
    }

    @Override
    public void run() {
        resourceMap.clear();
        if (context == null) throw new IllegalStateException("ResourceManager ：context is null！");
        basePath = context.getBasePath();
        // /Users/lilinjian/workspace/webapp/WEB-INF/
        //    ├── classes/          // 存放解压后的 .class 文件
        //    │      └── com/example/MyClass.class
        //    └── lib/              // 存放 jar 包
        //           └── llj.jar    // jar 包内部可能包含 com/example/LLJClass.class 等
        logger.debug("basePath ：{}", basePath);
        createResourceMapping();
    }


    //</editor-fold>
}
