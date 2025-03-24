package erangel.core;

import erangel.base.*;
import erangel.filter.ApplicationFilterConfig;
import erangel.filter.FilterDef;
import erangel.loader.WebAppLoader;
import erangel.mapper.ContextMapper;
import erangel.resource.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.HashMap;

/**
 * 表示一个web程序的总体上下文
 *
 * @author LILINJIAN
 * @version 2025/3/23
 */
public class DefaultContext extends VasBase implements Context, Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(DefaultContext.class);
    //<editor-fold desc = "attr">
    private Channel channel = new DefaultChannel(this);
    private String basePath = "";
    // web.xml中配置的web程序的监听器
    private String[] applicationListeners = new String[0];
    private Object applicationListenersObjects[] = new Object[0];
    // 过滤器
    private HashMap<String, String> filterMappings = new HashMap<>();
    private HashMap<String, FilterDef> filterDefs = new HashMap<>();
    private HashMap<String, ApplicationFilterConfig> filterConfigs = new HashMap<>();
    // 展示名
    private String displayName = "";
    // 当前上下文是否可使用flag
    private boolean available = false;
    private WebApplicationContext applicationContext = null;
    // webResources
    private ResourceManager resources = null;
    // 暂停标志位
    private boolean paused = false;
    // 配置文件加载状态标志位
    private boolean configured = false;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public DefaultContext() {
        // TODO 设置一个上下文检查点
        // channel.setBasicCheckpoint();
    }

    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public boolean getReloadable() {
        return true;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public synchronized ResourceManager getResources() {
        return resources;
    }

    @Override
    public synchronized void setResources(ResourceManager resources) {
        this.resources = resources;
    }

    @Override
    public void reload() {

    }

    @Override
    public String findMimeMapping(String ext) {
        return "";
    }

    @Override
    public Object[] getApplicationListeners() {
        return new Object[0];
    }

    @Override
    public void setApplicationListeners(Object[] listeners) {

    }

    @Override
    public void addApplicationListener(Object listener) {

    }

    @Override
    public String[] findApplicationListeners() {
        return applicationListeners;
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String findServletMapping(String s) {
        return "";
    }

    @Override
    public boolean getAvailable() {
        return false;
    }

    @Override
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public void setPath(String path) {

    }

    //</editor-fold>
    //<editor-fold desc = "thread bind">
    // 绑定当前线程，一般用于启动 关闭 重载时
    private ClassLoader bindThread() {
        // 取得当前线程的类加载器，多半是系统类加载器
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        logger.debug("bind thread oldCL : {}", oldCL);
        // 取得刚才设置好的webAppCL
        ClassLoader myLoader = getLoader().getClassLoader();
        logger.debug("bind thread newCL : {}", myLoader);
        if (myLoader == null) return oldCL;
        // 设置当前线程类加载器为自定义类加载器
        Thread.currentThread().setContextClassLoader(myLoader);
        return oldCL;
    }

    // 复原为原来线程的类加载器
    private void unbindThread(ClassLoader oldCL) {
        Thread.currentThread().setContextClassLoader(oldCL);
    }

    //</editor-fold>
    //<editor-fold desc = "生命周期">
    @Override
    public synchronized void start() throws LifecycleException {
        if (started) throw new LifecycleException("context : " + getName() + " is already started");
        lifecycleHelper.fireLifecycleEvent(BEFORE_START_EVENT, null);
        logger.info("context : {} starting...", getName());
        setAvailable(false);
        boolean noProblem = true;
        // 资源管理器设置
        try {
            ResourceManager resources = new ResourceManager(this);
            setResources(resources);
            resources.start();
        } catch (Exception e) {
            noProblem = false;
        }
        // 设置自定义类加载器
        if (noProblem && getLoader() == null) {
            WebAppLoader webAppLoader = new WebAppLoader(getParentClassLoader());
            setLoader(webAppLoader);
        }
        // 接下来切换类加载器，在这时候context使用的还是生命周期函数所在线程的类加载器，先保存引用
        ClassLoader oldCL = bindThread();
        if (noProblem) {
            try {
                addMapper(new ContextMapper());
                // 启动自定义的类加载器
                Loader loader = getLoader();
                // 启动自定义类加载器
                if (loader instanceof Lifecycle) ((Lifecycle) loader).start();
                started = true;
                // 复原为原来的类加载器
                unbindThread(oldCL);
                // 再次切换到刚才被启动的类加载器
                oldCL = bindThread();
                // 启动子容器
                Vas[] children = findChildren();
                for (Vas child : children) {
                    if (child instanceof Lifecycle)
                        ((Lifecycle) child).start();
                }
                // 启动通道
                if (channel instanceof Lifecycle) ((Lifecycle) channel).start();
                lifecycleHelper.fireLifecycleEvent(START_EVENT, null);
            } catch (Exception e) {
                noProblem = false;
                logger.error("waringing : context : {} start failed", getName(), e);
            } finally {
                // 恢复到原先线程的类加载器
                unbindThread(oldCL);
            }
        }
        if (!isConfigured()) {
            logger.error("context : {} start failed,cause configure failed", getName());
            noProblem = false;
        }
        // 切换到自定义类加载器
        oldCL = bindThread();
        // 启动过滤器和监听器
        if (noProblem) {
            if (!startListener()) noProblem = false;
        }
        if (noProblem) {
            if (!startFilter()) noProblem = false;
        }
        // 复原加载器
        unbindThread(oldCL);
        if (noProblem) {
            logger.info("context : {} started succeed", getName());
            setAvailable(true);
        } else {
            logger.error("context : {} start failed", getName());
           try{
               stop();
           } catch (Exception e) {
               logger.error("context : {} start failed, and try to stop but failed", getName(), e);
           }
            setAvailable(false);
        }
        lifecycleHelper.fireLifecycleEvent(AFTER_START_EVENT, null);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("context : " + getName() + " is not started");
        lifecycleHelper.fireLifecycleEvent(BEFORE_STOP_EVENT, null);
        logger.info("context : {} stopping...", getName());
    }

    //</editor-fold>
    //<editor-fold desc = "过滤器和监听器启动 停止">
    private boolean startListener() {
        logger.info("context : {} start listeners...", getName());
        // 取得web资源的类加载器
        ClassLoader cl = getLoader().getClassLoader();
        boolean noProblem = true;
        String[] listeners = findApplicationListeners();
        Object[] listenersObjects = new Object[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            try {
                Class<?> listenerClass = cl.loadClass(listeners[i]);
                listenersObjects[i] = listenerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                noProblem = false;
                logger.error("context : {} start listener : {} failed",
                        getName(), listeners[i], e);
            }
            if (!noProblem) return false;
            setApplicationListeners(listenersObjects);
            Object[] applicationListenersObjects = getApplicationListeners();
            // 说明web程序没有配置监听器而不是我们加载失败了
            if (applicationListenersObjects == null) return noProblem;
            // 发布servlet事件
            ServletContext servletContext = getServletContext();
            ServletContextEvent e = new ServletContextEvent(servletContext);
            try {
                for (Object obj : applicationListenersObjects) {
                    if (obj == null) continue;
                    if (!(obj instanceof ServletContextListener listener)) continue;
                    listener.contextInitialized(e);
                }
            } catch (Exception exception) {
                logger.error("context : {} start listener : {} failed",
                        getName(), listeners[i], exception);
                noProblem = false;
            }
        }
        return noProblem;
    }

    private boolean stopListener() {
        logger.info("context : {} stop listeners...", getName());
        boolean noProblem = true;
        Object[] listeners = getApplicationListeners();
        if (listeners == null) return true;
        ServletContext servletContext = getServletContext();
        ServletContextEvent e = new ServletContextEvent(servletContext);
        try {
            for (Object obj : listeners) {
                if (obj == null) continue;
                if (!(obj instanceof ServletContextListener listener)) continue;
                listener.contextDestroyed(e);
            }
        } catch (Exception exception) {
            logger.error("context : {} stop listener failed", getName(), exception);
            noProblem = false;
        }
        setApplicationListeners(null);
        return noProblem;
    }

    private boolean startFilter() {
        boolean noProblem = true;
        synchronized (filterConfigs) {
            filterConfigs.clear();
            for (String o : filterDefs.keySet()) {
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig
                            (this, (FilterDef) filterDefs.get(o));
                    filterConfigs.put(o, filterConfig);
                } catch (Throwable t) {
                    logger.error("context : {} start filter : {} failed", getName(), o, t);
                    noProblem = false;
                }
            }
        }
        return noProblem;
    }

    private boolean stopFilter() {
        logger.info("context : {} stop filters...", getName());
        synchronized (filterConfigs) {
            for (String o : filterConfigs.keySet()) {
                ApplicationFilterConfig filterConfig = filterConfigs.get(o);
                filterConfig.destroy();
            }
            filterConfigs.clear();
        }
        return true;
    }
    //</editor-fold>
}





