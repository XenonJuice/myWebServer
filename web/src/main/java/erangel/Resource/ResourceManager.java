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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static erangel.Const.commonCharacters.SOLIDUS;
import static erangel.Const.webApp.*;

public class ResourceManager implements Lifecycle, Runnable {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(ResourceManager.class);
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    // 本地资源映射
    private Map<String, LocalResource> resourceMap = new HashMap<>();
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

    // 扫描WEB-INF/classes
    public void scanFileSystem(Path classesDir) {
        try {
            Files.walk(classesDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        // classesDir 对应 /WEB-INF/classes，
                        // 那么将 path 转换成统一的资源路径
                        String resourcePath = SOLIDUS + classesDir.relativize(path).
                                toString().replace(File.separatorChar, '/');
                        resourceMap.put(resourcePath, new FileResource(path));
                    });
        } catch (IOException e) {
            logger.error("Failed to scan files in WEB-INF/classes", e);
        }
    }

    // 扫描 jar 文件
    public void scanJarFile(File jarFile) {
        JarFile jf;
        try {
            jf = new JarFile(jarFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(
                    Const.webApp.DOTCLASS)) {
                // 此处 entry.getName() 通常已经使用 '/' 分隔
                resourceMap.put(SOLIDUS +
                        entry.getName(), new JarResource(jf, entry));
            }
        }
    }

    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
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
