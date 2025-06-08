package livonia.resource;

import livonia.base.Const;
import livonia.base.Context;
import livonia.lifecycle.Lifecycle;
import livonia.lifecycle.LifecycleException;
import livonia.lifecycle.LifecycleListener;
import livonia.log.BaseLogger;
import livonia.utils.LifecycleHelper;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static livonia.base.Const.CharPunctuationMarks.CHAR_SOLIDUS;
import static livonia.base.Const.commonCharacters.SOLIDUS;
import static livonia.base.Const.webApp.*;

public class ResourceManager implements Lifecycle {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(ResourceManager.class);
    // 本地资源映射
    private final Map<String, List<LocalResource>> classLoaderResourceMap = new HashMap<>();
    private final Map<String, List<LocalResource>> configMap = new HashMap<>();
    private final Map<String, List<LocalResource>> stasticResMap = new HashMap<>();
    // 缓存 .class 的相对路径到绝对 Path
    private final Map<String, Path> classesPaths = new HashMap<>();
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    private Map<String, List<LocalResource>> allResources = new HashMap<>();
    // 根目录
    private String basePath = null;
    // 绑定的Context
    private Context context = null;
    // 组件启动标志位
    private boolean isStarted = false;

    public ResourceManager(Context context) {
        this.context = context;
    }

    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Map<String, List<LocalResource>> getConfigMap() {
        return configMap;
    }

    public Map<String, List<LocalResource>> getStasticResMap() {
        return stasticResMap;
    }

    public Map<String, List<LocalResource>> getClassLoaderResourceMap() {
        return classLoaderResourceMap;
    }

    //</editor-fold>
    //<editor-fold desc = "扫描资源">
    // 始终以斜杠开头 例如/com/example/
    public void scanFileSystem(Path rootDir) {
        // 递归遍历整个目录树
        try {
            Files.walk(rootDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relative = rootDir.relativize(path).toString().replace(
                                File.separatorChar, CHAR_SOLIDUS);
                        String resourcePath = SOLIDUS + relative;
                        // 构造 FileResource 对象
                        LocalResource resource = new FileResource(path);
                        addStaticResources(resourcePath, resource);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void scanClassDir(Path classesDir) {
        try {
            Files.walk(classesDir)
                    .filter(p -> p.toString().endsWith(DOTCLASS))
                    .forEach(p -> {
                        // 计算相对路径，并统一为/分隔
                        String rel = classesDir.relativize(p)
                                .toString()
                                .replace(File.separatorChar, CHAR_SOLIDUS);
                        // 跳过受保护的包
                        if (isProtectedClass(rel)) {
                            logger.info("跳过受保护的类文件: {}", rel);
                            return;
                        }
                        // 正常注册
                        classesPaths.put(SOLIDUS + rel, p);
                    });
        } catch (IOException e) {
            logger.error("初始化 class 路径失败: {}", classesDir, e);
        }
    }

    // 过滤规范API包
    private boolean isProtectedClass(String relPath) {
        // 去掉末尾 ".class"
        String pkg = relPath.substring(0, relPath.length() - 6);
        // 以 '/' 分隔的包名
        return pkg.startsWith("javax/servlet/") || pkg.startsWith("jakarta/servlet/");
    }

    // 扫描 jar 文件
    public void scanJarFile(File jarFile) {
        // 打开 JarFile，并把所有条目读到一个列表中，方便两次遍历
        List<JarEntry> entries;
        try (JarFile jf = new JarFile(jarFile)) {
            entries = Collections.list(jf.entries());
        } catch (IOException e) {
            logger.warn("无法打开 JAR 进行校验，跳过扫描: {}", jarFile, e);
            return;
        }

        // 校验阶段：如果包含 Servlet API、Jakarta Servlet、Catalina 核心等类，就 skip
        for (JarEntry je : entries) {
            String name = je.getName();
            // Servlet API
            if (name.equals("javax/servlet/Servlet.class")
                    || name.startsWith("javax/servlet/")) {
                logger.info("跳过 servlet-api JAR: {}", jarFile.getName());
                return;
            }
            if (name.equals("jakarta/servlet/Servlet.class")
                    || name.startsWith("jakarta/servlet/")) {
                logger.info("跳过 jakarta servlet-api JAR: {}", jarFile.getName());
                return;
            }
        }

        // 真正扫描阶段：所有校验通过的 JAR 才做全量索引
        try (JarFile jf = new JarFile(jarFile)) {
            Enumeration<JarEntry> e = jf.entries();
            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if (!je.isDirectory()) {
                    String resourcePath = normalizePath(je.getName());
                    LocalResource resource = new JarResource(jf, je, Path.of(resourcePath));
                    addClassLoaderResources(resourcePath, resource);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("扫描 JAR 失败: " + jarFile, ex);
        }
    }

    // 扫描 配置文件
    public void scanConfigResources(Path rootPath) {
        // 遍历整个根目录
        try {
            Files.walk(rootPath, 1)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        // 判断是否为常用的 web 配置文件或属性文件
                        if (fileName.equalsIgnoreCase(WEB_XML) ||
                                fileName.equalsIgnoreCase("context.xml") ||
                                fileName.endsWith(DOTPROP)) {
                            // 统一资源路径
                            String resourcePath = SOLIDUS + rootPath.relativize(path)
                                    .toString()
                                    .replace(File.separatorChar, '/');
                            logger.debug("Found resource: {}", resourcePath);
                            // 将扫描到的资源信息存入 configMap
                            LocalResource resource = new FileResource(path);
                            addConfigResources(resourcePath, resource);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取位于指定目录路径下的一组资源路径。
     *
     * @param path 要列出资源路径的目录路径。必须以 '/' 结尾。
     * @return 指定目录下的一组资源路径。
     * 集合中的路径可以表示文件或目录。
     * 如果目录中没有资源或输入无效，则返回一个空集合。
     */
    public Set<String> listResourcePaths(String path) {
        // 检查路径必须以 '/' 结尾
        if (!path.endsWith(SOLIDUS)) throw new IllegalArgumentException("路径必须以 / 结尾");
        LocalResource[] resources = getLoaderResources(path);
        if (resources == null) {
            return Collections.emptySet();
        }

        Set<String> resourcePaths = new HashSet<>();
        for (LocalResource resource : resources) {
            URL resourceURL = resource.getURL();
            String resourcePath = resourceURL.getPath();
            // 规范为子路径
            int index = resourcePath.indexOf(path);
            if (index >= 0) {
                resourcePath = resourcePath.substring(index);
            }
            // 如果为目录且路径不以 '/' 结尾，则添加 '/'
            if (resource.isDirectory() && !resourcePath.endsWith(SOLIDUS)) {
                resourcePath += SOLIDUS;
            }
            resourcePaths.add(resourcePath);
        }
        return resourcePaths;
    }

    // combine
    private void combine() {
        allResources.clear();
        Map<String, List<LocalResource>> tempMap = new HashMap<>();
        tempMap.putAll(classLoaderResourceMap);
        tempMap.putAll(configMap);
        tempMap.putAll(stasticResMap);
        allResources = tempMap;
        logger.debug("allResources size : {}", allResources.size());
        for (Map.Entry<String, List<LocalResource>> entry : allResources.entrySet()) {
            String path = entry.getKey();
            List<LocalResource> resources = entry.getValue();
            // logger.debug("path : {}, }", path);
            for (LocalResource resource : resources) {
                // logger.debug("resource : {}", resource);
                String name = resource.getName();
                // logger.debug("name : {}", name);
            }
        }
        logger.debug("combine finished");
    }


    //</editor-fold>
    //<editor-fold desc = "获取资源">
    public LocalResource getResource(String path) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> resources = allResources.get(normalizedPath);
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        for (LocalResource res : resources) {
            if (res.exists()) {
                return res;
            } else {
                logger.warn("Resource {} not found", path);
                return null;
            }
        }
        return null;
    }

    public LocalResource[] getResources(String path) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> resources = allResources.get(normalizedPath);
        if (resources == null) {
            return new LocalResource[0];
        }
        return resources.toArray(new LocalResource[0]);
    }

    public LocalResource getLoaderResource(String path) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> resources01 = classLoaderResourceMap.get(normalizedPath);
        if (resources01 != null) {
            for (LocalResource res : resources01) {
                if (res.exists()) {
                    return res;
                }
            }
        }
        Path target =classesPaths.get(normalizedPath);
        if (target != null) {
            return new FileResource(target);
        }
        return null;
    }

    public LocalResource getClassResource(String path) {
        Path p = classesPaths.get(path);
        if (p == null) {
            return null;
        }
        return new FileResource(p);
    }

    public LocalResource[] getLoaderResources(String path) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> resources = classLoaderResourceMap.get(normalizedPath);
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

    private String normalizePath(String path) {
        if (!path.startsWith(SOLIDUS)) path = SOLIDUS + path;
        return path;
    }

    public void addClassLoaderResources(String path, LocalResource resource) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> list = classLoaderResourceMap.get(normalizedPath);
        if (list == null) {
            list = new ArrayList<>();
            list.add(resource);
            classLoaderResourceMap.put(normalizedPath, list);
        }
        list.add(resource);
    }

    public void addConfigResources(String path, LocalResource resource) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> list = configMap.get(normalizedPath);
        if (list == null) {
            list = new ArrayList<>();
            list.add(resource);
            configMap.put(normalizedPath, list);
        }
        list.add(resource);
    }

    public void addStaticResources(String path, LocalResource resource) {
        String normalizedPath = normalizePath(path);
        List<LocalResource> list = stasticResMap.get(normalizedPath);
        if (list == null) {
            list = new ArrayList<>();
            list.add(resource);
            stasticResMap.put(normalizedPath, list);
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
        Path staticDir = webInfRoot.resolve(RESOURCES_ONLY);
        // 扫描WEB-INF/classes下的所有 class 文件
        scanClassDir(classesDir);
        // 扫描静态资源
        scanFileSystem(staticDir);
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
        // 扫描所有配置文件
        scanConfigResources(webInfRoot);
        // 扫描所有静态文件
        scanFileSystem(staticDir);
        // 将所有映射统一管理
        combine();
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
    public LocalResource[] getAllLibResources() {
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
    //<editor-fold desc = "生命周期">
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycleHelper.removeLifecycleListener(listener);
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycleHelper.addLifecycleListener(listener);
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
        classLoaderResourceMap.clear();
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


    @Override
    public void stop() throws LifecycleException {
        if (!isStarted) throw new LifecycleException("ResourceManager is not started");
        logger.debug("LifeCycle : ResourceManager is stopping");
        lifecycleHelper.fireLifecycleEvent(STOP_EVENT, null);
        isStarted = false;
        logger.debug("LifeCycle : ResourceManager is stopped");
    }
    //</editor-fold>
}
