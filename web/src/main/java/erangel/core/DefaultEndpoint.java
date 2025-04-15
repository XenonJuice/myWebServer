package erangel.core;

import erangel.base.Context;
import erangel.base.Endpoint;
import erangel.base.LifecycleException;
import erangel.base.Loader;
import erangel.checkpoints.ContextCheckpoint;
import erangel.checkpoints.EndpointCheckpoint;
import erangel.loader.WebAppClassLoader;
import erangel.log.BaseLogger;
import org.slf4j.Logger;

import javax.servlet.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public final class DefaultEndpoint extends VasBase implements Endpoint, ServletConfig {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(DefaultEndpoint.class);
    // 初始化param
    private final HashMap<String, String> initParams = new HashMap<>();
    // 初始化后的servlet实例
    private Servlet sInstance = null;
    private String servletClass = null;
    // 初始化标志位
    private volatile boolean initialized = false;
    // 内部类
    private InnerServletConfig innerServletConfig = new InnerServletConfig(this);
    // 当前活跃访问数量
    private volatile int count = 0;
    // 正在卸载servlet标志位
    private boolean isUnloading = false;
    // 可用性：OL可用 INT MAXVALUE不可用
    private long available = 0L;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public DefaultEndpoint() {
        channel.setBasicCheckpoint(new EndpointCheckpoint());
    }

    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public long getAvailable() {
        return available;
    }

    public void setAvailable(long available) {
        long bignumber = System.currentTimeMillis();
        if (available > bignumber) this.available = available;
        else this.available = 0L;
    }

    //</editor-fold>
    //<editor-fold desc = "实现或重写父类，接口">
    @Override
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;

    }

    @Override
    public synchronized void load() throws ServletException {
        sInstance = loadServlet();
        if (!initialized) initServlet(sInstance);
    }

    public synchronized Servlet loadServlet() throws ServletException {
        if (sInstance != null) return sInstance;
        if (servletClass == null) {
            unavailable(null);
            throw new ServletException("servlet class is null");
        }
        Servlet servlet;
        String target = servletClass;
        Loader loader = getLoader();
        try {
            if (loader == null) {
                unavailable(null);
                throw new ServletException("loader is null");
            }
            ClassLoader classLoader = loader.getClassLoader();
            Class<?> clazz;
            if (classLoader != null) {
                logger.info("try loading servlet : {} using classloader : {} ", target, ((WebAppClassLoader) classLoader).toString());
                try {
                    clazz = classLoader.loadClass(target);
                } catch (Exception e) {
                    unavailable(null);
                    throw new ServletException("load servlet failed", e);
                }
            } else {
                logger.info("try loading servlet : {} using system classloader ", target);
                try {
                    clazz = Class.forName(target);
                } catch (ClassNotFoundException e) {
                    throw new ServletException(e);
                }
            }
            if (clazz == null) {
                unavailable(null);
                throw new ServletException("load servlet failed");
            }
            try {
                servlet = (Servlet) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                unavailable(null);
                throw new ServletException("load servlet failed", e);
            }
        } finally {
            logger.info("===> load servlet : {} success <===", target);
        }
        return servlet;
    }

    private void initServlet(Servlet servlet) throws ServletException {
        if (!initialized) {
            try {
                servlet.init(innerServletConfig);
                initialized = true;
            } catch (UnavailableException e) {
                unavailable(e);
                throw new ServletException("init servlet failed", e);
            } catch (RuntimeException e) {
                throw new ServletException("init servlet failed", e);
            }
        }
    }

    @Override
    public synchronized void unload() throws ServletException {
        if (sInstance == null) return;
        isUnloading = true;
        if (getCount() > 0) {
            // 最大等待时间设为1000毫秒
            long maxWaitTime = 1000;
            long startTime = System.currentTimeMillis();
            while (getCount() > 0 && (System.currentTimeMillis() - startTime) < maxWaitTime) {
                try {
                    Thread.sleep(50); // 每50毫秒检查一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        // 获取当前servlet实例的类加载器
        ClassLoader webCL = sInstance.getClass().getClassLoader();
        // 获取当前上下文的类加载器
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        // 若超时仍有servlet在被使用，则输出日志并销毁实例
        if (getCount() > 0) {
            logger.warn("endpoint : {} is unloading but still has {} servlets allocated", getName(), count);
        }
        try {
            // 使用web程序类加载
            Thread.currentThread().setContextClassLoader(webCL);
            sInstance.destroy();
        } catch (Exception e) {
            logger.error("endpoint : {} destroy servlet failed", getName(), e);
            sInstance = null;
            initialized = false;
            isUnloading = false;
            throw new ServletException("endpoint : " + getName() + " destroy servlet failed", e);
        } finally {
            // 将原加载器放回
            Thread.currentThread().setContextClassLoader(oldCL);
        }
        sInstance = null;
        initialized = false;
        isUnloading = false;
        logger.info("endpoint : {} unload success", getName());
    }


    @Override
    public Servlet salloc() throws ServletException {
        logger.debug("endpoint : {} allocate servlet", getName());
        if (isUnloading) throw new ServletException("endpoint : " + getName() + " is unloading");
        boolean isNew = false;
        // 当servlet不存在或未被初始化时
        if (sInstance == null || !initialized) {
            synchronized (this) {
                if (sInstance == null) {
                    try {
                        sInstance = loadServlet();
                        isNew = true;
                        countUp();
                    } catch (ServletException e) {
                        throw new ServletException("endpoint : " + getName() + " load servlet failed when allocating", e);
                    }
                }
                if (!initialized) {
                    initServlet(sInstance);
                }
            }
        }
        // 不是新生成的servlet实例，当前活跃访问+1
        if (!isNew) countUp();
        return sInstance;
    }

    @Override
    public void sfree() {
        logger.debug("endpoint : {} free servlet", getName());
        countDown();
    }

    @Override
    public String findInitParam(String name) {
        synchronized (initParams) {
            return initParams.get(name);
        }
    }

    @Override
    public String[] findInitParams() {
        synchronized (initParams) {
            return initParams.values().toArray(new String[0]);
        }
    }

    @Override
    public void removeInitParam(String name) {
        synchronized (initParams) {
            initParams.remove(name);
        }
    }

    @Override
    public boolean isUnavailable() {
        if (available == 0L) return false;
        if (available < System.currentTimeMillis()) {
            available = 0L;
            return false;
        }
        return true;
    }

    @Override
    public void unavailable(UnavailableException unavailable) {
        if (unavailable == null) {
            logger.warn("mark endpoint : {} as unavailable cause : {}", getName(), String.valueOf(unavailable));
            setAvailable(Long.MAX_VALUE);
        } else if (unavailable.isPermanent()) {
            logger.warn("mark endpoint : {} as unavailable cause : {}", getName(), unavailable.getMessage());
            setAvailable(Long.MAX_VALUE);
        } else {
            int timeout = unavailable.getUnavailableSeconds();
            if (timeout < 0) timeout = 1;
            setAvailable(System.currentTimeMillis() + timeout * 1000L);
        }
    }

    @Override
    public String getServletName() {
        return getName();
    }

    @Override
    public ServletContext getServletContext() {
        if (parent == null) return null;
        if (!(parent instanceof Context)) return null;
        return ((Context) parent).getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        return findInitParam(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        synchronized (initParams) {
            return Collections.enumeration(initParams.keySet());
        }
    }

    //</editor-fold>
    //<editor-fold desc = "线程安全算数">
    private synchronized void countUp() {
        count++;
    }

    private synchronized void countDown() {
        count--;
    }

    private synchronized void countReset() {
        count = 0;
    }

    private synchronized int getCount() {
        return count;
    }

    //</editor-fold>
    //<editor-fold desc = "生命周期">
    @Override
    public void start() throws LifecycleException {
        super.start();
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            unload();
        } catch (ServletException e) {
            logger.error("endpoint : {} unload failed", getName(), e);
        }
        super.stop();
    }

    //</editor-fold>
    //<editor-fold desc = "内部类">
    public static final class InnerServletConfig implements ServletConfig {
        private ServletConfig config = null;

        public InnerServletConfig(DefaultEndpoint endpoint) {
            this.config = endpoint;
        }

        @Override
        public String getServletName() {
            return this.config.getServletName();
        }

        @Override
        public ServletContext getServletContext() {
            return this.config.getServletContext();
        }

        @Override
        public String getInitParameter(String name) {
            return config.getInitParameter(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return config.getInitParameterNames();
        }
    }
    //</editor-fold>
}
