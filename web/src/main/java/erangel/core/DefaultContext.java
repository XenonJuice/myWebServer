package erangel.core;

import erangel.base.*;
import erangel.checkpoints.ContextCheckpoint;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.filter.ApplicationFilterConfig;
import erangel.filter.FilterDef;
import erangel.filter.FilterMap;
import erangel.lifecycle.Lifecycle;
import erangel.lifecycle.LifecycleException;
import erangel.loader.WebAppLoader;
import erangel.resource.ResourceManager;
import erangel.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * 表示一个web程序的总体上下文
 *
 * @author LILINJIAN
 * @version 2025/3/23
 */
public class DefaultContext extends VasBase implements Context {
    private static final Logger logger = LoggerFactory.getLogger(DefaultContext.class);
    //<editor-fold desc = "attr">
    private String defaultContextMapper = "erangel.core.DefaultContextMapper";
    private String basePath = "";
    // web.xml中配置的web程序的监听器
    private String[] applicationListeners = new String[0];
    private Object[] applicationListenersObjects = new Object[0];
    // 过滤器映射信息
    private FilterMap[] filterMaps = new FilterMap[0];
    // 过滤器定义，key为过滤器的名字
    private final HashMap<String, FilterDef> filterDefs = new HashMap<>();
    // 加载过后的过滤器集合，key为过滤器的名字
    private final HashMap<String, ApplicationFilterConfig> filterConfigs = new HashMap<>();
    // servlet map
    private final HashMap<String, String> servletMappings = new HashMap<>();
    // mime map
    private final HashMap<String, String> mimeMappings = new HashMap<>();
    // 展示名
    private String displayName = "";
    // 当前上下文是否可使用flag
    private boolean available = false;
    // applicationContext
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
        ContextCheckpoint basicCP = new ContextCheckpoint();
        channel.setBasicCheckpoint(basicCP);
    }

    //</editor-fold>
    //<editor-fold desc = "getter & setter">

    //<editor-fold desc = "映射器">
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
        this.defaultContextMapper = mapper.getClass().getName();
    }

    public String getMapper() {
        return defaultContextMapper;
    }

    //</editor-fold>
    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isConfigured() {
        return configured;
    }

    @Override
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
    public Endpoint createEndpoint() {
        return new DefaultEndpoint();
    }

    @Override
    public boolean getReloadable() {
        return true;
    }

    @Override
    public ServletContext getServletContext() {
        if (applicationContext == null) {
            applicationContext = new WebApplicationContext(getBasePath(), this);
        }
        return applicationContext;
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
    public synchronized void reload() {
        if (!started) throw new IllegalStateException("context : " + getName() + " is not started");
        setPaused(true);
        try {
            stop();
            start();
        } catch (LifecycleException e) {
            logger.error("reload context : {} failed , continue running the old context", getName(), e);
        } finally {
            setPaused(false);
        }
    }

    @Override
    public String findMimeMapping(String ext) {
        synchronized (mimeMappings) {
            return mimeMappings.get(ext);
        }
    }

    @Override
    public Object[] getApplicationListeners() {
        return applicationListenersObjects;
    }

    @Override
    public void setApplicationListeners(Object[] listeners) {
        this.applicationListenersObjects = listeners;
    }

    @Override
    public void addApplicationListener(String listener) {
        synchronized (applicationListenersObjects) {
            String[] newListeners = new String[applicationListenersObjects.length + 1];
            for (int i = 0; i < applicationListenersObjects.length; i++) {
                if (listener.equals(applicationListenersObjects[i])) return;
                newListeners[i] = applicationListeners[i];
            }
            newListeners[newListeners.length - 1] = listener;
            applicationListeners = newListeners;
        }
    }

    @Override
    public String[] findApplicationListeners() {
        return applicationListeners;
    }

    @Override
    public void removeApplicationListener(String listener) {
        synchronized (applicationListeners) {
            int listenerIndex = -1;
            for (int i = 0; i < applicationListeners.length; i++) {
                if (applicationListeners[i].equals(listener)) {
                    listenerIndex = i;
                    break;
                }
            }
            if (listenerIndex < 0) {
                return;
            }
            String[] updatedListeners = new String[applicationListeners.length - 1];
            System.arraycopy(applicationListeners, 0, updatedListeners, 0, listenerIndex);
            System.arraycopy(applicationListeners, listenerIndex + 1, updatedListeners, listenerIndex,
                    applicationListeners.length - listenerIndex - 1);
            applicationListeners = updatedListeners;
        }
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String findServletMapping(String s) {
        synchronized (servletMappings) {
            return servletMappings.get(s);
        }

    }

    @Override
    public String[] findServletMappings() {
        synchronized (servletMappings) {
            return servletMappings.keySet().toArray(new String[0]);
        }
    }

    @Override
    public void addServletMapping(String urlPattern, String servletName) {
        if (servletName == null) throw new NullPointerException("servletName is null");
        if (findChild(servletName) == null)
            throw new IllegalArgumentException("servletName : " + servletName + " is not found");
        urlPattern = Decoder.decode(urlPattern, StandardCharsets.UTF_8);
        synchronized (servletMappings) {
            servletMappings.put(urlPattern, servletName);
        }
    }

    @Override
    public void removeServletMapping(String urlPattern) {
        synchronized (servletMappings) {
            servletMappings.remove(urlPattern);
        }
    }

    @Override
    public void addFilterMap(FilterMap filterMap) {
        String filterName = filterMap.getFilterName();
        String servletName = filterMap.getServletName();
        String urlPattern = filterMap.getUrlPattern();
        if (findFilterDef(filterName) == null) {
            throw new IllegalArgumentException("filterName : " + filterName + " is not found");
        }
        if (servletName == null && urlPattern == null) {
            throw new IllegalArgumentException("filterMap : " + filterMap + " is invalid ：all blank");
        }
        if (servletName != null && urlPattern != null) {
            throw new IllegalArgumentException("filterMap : " + filterMap + " is invalid ：cant be both servletName and urlPattern");
        }
        synchronized (filterMaps) {
            FilterMap[] newFilterMaps = new FilterMap[filterMaps.length + 1];
            System.arraycopy(filterMaps, 0, newFilterMaps, 0, filterMaps.length);
            newFilterMaps[newFilterMaps.length - 1] = filterMap;
            filterMaps = newFilterMaps;
        }
    }

    @Override
    public FilterMap[] findFilterMaps() {
        return filterMaps;

    }

    @Override
    public FilterConfig findFilterConfig(String name) {
        synchronized (filterConfigs) {
            return filterConfigs.get(name);
        }
    }

    @Override
    public FilterDef findFilterDef(String filterName) {
        synchronized (filterDefs) {
            return filterDefs.get(filterName);
        }
    }

    @Override
    public FilterDef[] findFilterDefs() {
        synchronized (filterDefs) {
            FilterDef[] newArray = new FilterDef[filterDefs.size()];
            return filterDefs.values().toArray(newArray);
        }
    }

    @Override
    public void removeFilterDef(FilterDef filterDef) {
        synchronized (filterDefs) {
            filterDefs.remove(filterDef.getFilterName());
        }
    }

    @Override
    public void addFilterDef(FilterDef filterDef) {
        synchronized (filterDefs) {
            filterDefs.put(filterDef.getFilterName(), filterDef);
        }
    }

    @Override
    public void removeFilterMap(FilterMap filterMap) {
        synchronized (filterMaps) {
            int indexToRemove = -1;
            for (int i = 0; i < filterMaps.length; i++) {
                if (filterMaps[i] == filterMap) {
                    indexToRemove = i;
                    break;
                }
            }
            if (indexToRemove < 0) {
                return;
            }
            int newLength = filterMaps.length - 1;
            FilterMap[] updatedFilterMaps = new FilterMap[newLength];
            System.arraycopy(filterMaps, 0, updatedFilterMaps, 0, indexToRemove);
            System.arraycopy(filterMaps, indexToRemove + 1, updatedFilterMaps, indexToRemove,
                    newLength - indexToRemove);
            filterMaps = updatedFilterMaps;
        }
    }

    @Override
    public boolean getAvailable() {
        return available;
    }

    @Override
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getPath() {
        return getName();
    }

    @Override
    public void setPath(String path) {
        setName(Decoder.decode(path, StandardCharsets.UTF_8));
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
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        while (isPaused()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        super.process(request, response);
    }

    @Override
    public synchronized void start() throws LifecycleException {
        if (started) throw new LifecycleException("context : " + getName() + " is already started");
        lifecycleHelper.fireLifecycleEvent(BEFORE_START_EVENT, null);
        logger.info("context : {} starting...", getName());
        setConfigured(false);
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
            webAppLoader.setContext(this);
            setLoader(webAppLoader);
        }
        // 接下来切换类加载器，在这时候context使用的还是生命周期函数所在线程的类加载器，先保存引用
        ClassLoader oldCL = bindThread();
        if (noProblem) {
            try {
                // addMapper(new ContextMapper());
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
                logger.error("warning : context : {} start failed", getName(), e);
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
            try {
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
        setAvailable(false);
        // 将类加载器切换至自定义类加载器
        ClassLoader oldCL = bindThread();
        stopFilter();
        lifecycleHelper.fireLifecycleEvent(STOP_EVENT, null);
        started = false;
        // 关闭各种组件
        try {
            if (channel instanceof Lifecycle) ((Lifecycle) channel).stop();
            Vas[] children = findChildren();
            for (Vas child : children) {
                if (child instanceof Lifecycle)
                    ((Lifecycle) child).stop();
            }
            Mapper[] mappers = findMappers();
            for (Mapper mapper : mappers) {
                if (mapper instanceof Lifecycle)
                    ((Lifecycle) mapper).stop();
            }
            stopListener();
            if (getResources() != null) getResources().stop();
            if (getLoader() instanceof Lifecycle) ((Lifecycle) getLoader()).stop();
        } finally {
            // 还原至调用此方法的线程的类加载器
            unbindThread(oldCL);
        }
        applicationContext = null;
        logger.info("context : {} stopped succeed", getName());
        setAvailable(false);
        lifecycleHelper.fireLifecycleEvent(AFTER_STOP_EVENT, null);
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
                    logger.info("context : {} trying start listener : {} ", getName(), obj);
                    listener.contextInitialized(e);
                    logger.info("context : {} start listener : {} succeed", getName(), obj);
                }
            } catch (Exception exception) {
                logger.error("context : {} start listener : {} failed",
                        getName(), listeners[i], exception);
                noProblem = false;
            }
        }
        return noProblem;
    }

    private void stopListener() {
        logger.info("context : {} stop listeners...", getName());
        Object[] listeners = getApplicationListeners();
        if (listeners == null) return;
        ServletContext servletContext = getServletContext();
        ServletContextEvent e = new ServletContextEvent(servletContext);
        try {
            for (Object obj : listeners) {
                if (obj == null) continue;
                if (!(obj instanceof ServletContextListener listener)) continue;
                logger.info("context : {} trying stop listener : {} ", getName(), obj);
                listener.contextDestroyed(e);
                logger.info("context : {} stop listener : {} succeed", getName(), obj);
            }
        } catch (Exception exception) {
            logger.error("context : {} stop listener failed", getName(), exception);
        }
        setApplicationListeners(null);
        logger.info("context : {} stop listeners succeed", getName());
    }

    private boolean startFilter() {
        boolean noProblem = true;
        synchronized (filterConfigs) {
            filterConfigs.clear();
            for (String o : filterDefs.keySet()) {
                ApplicationFilterConfig filterConfig = null;
                try {
                    filterConfig = new ApplicationFilterConfig
                            (this, filterDefs.get(o));
                    logger.info("context : {} start filter : {} succeed", getName(), o);
                    filterConfigs.put(o, filterConfig);
                } catch (Throwable t) {
                    logger.error("context : {} start filter : {} failed", getName(), o, t);
                    noProblem = false;
                }
            }
        }
        return noProblem;
    }

    private void stopFilter() {
        logger.info("context : {} stop filters...", getName());
        synchronized (filterConfigs) {
            for (String o : filterConfigs.keySet()) {
                ApplicationFilterConfig filterConfig = filterConfigs.get(o);
                logger.info("context : {} trying stop filter : {} ", getName(), o);
                filterConfig.destroy();
                logger.info("context : {} stop filter : {} succeed", getName(), o);
            }
            filterConfigs.clear();
            logger.info("context : {} stop filters succeed", getName());
        }
    }
    //</editor-fold>
}





