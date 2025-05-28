package erangel.loader;


import erangel.base.Const;
import erangel.base.Context;
import erangel.base.Loader;
import erangel.base.Vas;
import erangel.lifecycle.Lifecycle;
import erangel.lifecycle.LifecycleException;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import erangel.utils.LifecycleHelper;
import org.slf4j.Logger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 类加载器层次结构：
 * <ul>
 *   <li>Bootstrap ClassLoader ：in JVM，load  java.lang.*、java.util.*
 *   <li>Platform ClassLoader ：load XML,JavaFX...
 *   <li>Application ClassLoader ：load custom application code and third-party dependencies, such as myWebApplication.class, mysql.jar.
 *   <li>Common ClassLoader ：load some common parts for Server and webApplication ，sych as JDBC Driver，LOG4J..
 *     <ul>
 *       <li>myWebServerClassLoader1 ：load core parts of this server,such as NormalContext,webApplicationContext,HttpConnector...</li>
 *       <li>WebApp ClassLoader1 ：load WEB-INF/classes & WEB-INF/lib</li>
 *       <li>WebApp ClassLoader2 ：same as above</li>
 *       <li>...</li>
 *     </ul>
 * </ul>
 */

public class WebAppLoader implements Loader, Runnable, Lifecycle, PropertyChangeListener {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(WebAppLoader.class);
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    // 属性更改助手
    protected PropertyChangeSupport propHelper = new PropertyChangeSupport(this);
    // 当前loader所处容器
    private Vas vas = null;
    // 与此loader所过连的web上下文
    private Context context = null;
    // 是否遵循默认委派模型
    private boolean delegate = false;
    // 是否可重载
    private boolean reloadable = false;
    // 此组件的父加载器
    private ClassLoader parentClassLoader = null;
    // 被这个loader组件管理的真正的用于加载web程序的类加载器
    private WebAppClassLoader webAppClassLoader = null;
    // 定期检查时常
    private final long checkPeriod = 10000L;
    // 使用的类加载器的全限定名
    private String loadClass = WebAppClassLoader.class.getName();
    // 组件启动标志位
    private boolean isStarted = false;
    // 后台线程
    private Thread thread = null;
    // 线程名
    private String threadName = "WebAppLoader";
    // 关联的web资源

    // 确认关闭
    private boolean closeRequried = false;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    // 默认构造器 调用可以传入一个父加载器的构造器
    public WebAppLoader() {
        this(null);
    }

    // 传入一个父加载器的构造器
    public WebAppLoader(ClassLoader parent) {
        this.parentClassLoader = parent;
    }

    //</editor-fold>
    //<editor-fold desc = "getter&setter">
    @Override
    public ClassLoader getClassLoader() {
        return this.webAppClassLoader;
    }

    @Override
    public Vas getVas() {
        return this.vas;
    }

    @Override
    public void setVas(Vas vas) {
        Vas oldVas = this.vas;
        this.vas = vas;
        propHelper.firePropertyChange("vas", oldVas, this.vas);
        // 绑定新的容器
        if (this.vas != null) {
            setReloadable(true);
        }
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public void setContext(Context context) {
        if (context == this.context) return;
        Context oldContext = this.context;
        this.context = context;
        propHelper.firePropertyChange("Context", oldContext, this.context);
        if (this.context != null) {
            setReloadable(this.context.getReloadable());
        }
    }

    @Override
    public boolean getDelegate() {
        return this.delegate;
    }

    /**
     * 设置是否使用标准委托模型
     * 通知任何已注册的监听器此更改。
     *
     * @param delegate 标准委托模型。值为 {@code true}
     *                 将加载器设置为非标准委托模式，而 {@code false}
     */
    @Override
    public void setDelegate(boolean delegate) {
        boolean oldDelegate = this.delegate;
        this.delegate = delegate;
        propHelper.firePropertyChange("Delegate", oldDelegate, this.delegate);
    }

    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }

    @Override
    public void setReloadable(boolean reloadable) {
        boolean oldReloadable = this.reloadable;
        this.reloadable = reloadable;
        propHelper.firePropertyChange("Reloadable", oldReloadable, this.reloadable);
        if (!isStarted) return;
        // 若之前一次不可重载而现在可重载则运行线程
        if (!oldReloadable && reloadable) {
            threadStart();
            // 若之前一次可重载而现在不可重载则终止线程
        } else if (oldReloadable && !reloadable) {
            threadStop();
        }
    }

    public String getLoadClass() {
        return this.loadClass;
    }

    public void setLoadClass(String loadClass) {
        this.loadClass = loadClass;
    }
    //</editor-fold>
    //<editor-fold desc = "其他方法">

    @Override
    public String toString() {
        return "WebAppLoader{" +
                "loadClass='" + loadClass + '\'' +
                ", delegate=" + delegate +
                ", reloadable=" + reloadable +
                ", parentClassLoader=" + parentClassLoader +
                ", webAppClassLoader=" + webAppClassLoader +
                ", thread=" + thread +
                ", threadName='" + threadName + '\'' +
                '}';
    }

    /**
     * 创建并返回一个用于加载webapp的类加载器
     *
     * @return 加载webapp的类加载器
     * @see WebAppClassLoader
     */
    private WebAppClassLoader createWebAppClassLoader() throws
            ClassNotFoundException, InvocationTargetException,
            InstantiationException, IllegalAccessException,
            NoSuchMethodException {
        // 返回值
        WebAppClassLoader webAppClassLoader;

        // 获取到WebAppClassLoader的类对象
        Class<?> clazz = Class.forName(loadClass);
        // 若当前的父加载器为null，则获取Context容器的父加载器
        // ##这里应该是在容器启动的bootstrap类中声明的更高级的加载器，
        // ##比如说这个服务器整体的类加载器
        if (parentClassLoader == null)
            parentClassLoader = context.getParentClassLoader();
        // 声明WebAppClassLoader构造器的参数类型r
        Class<?>[] argTypes = {ClassLoader.class};
        // 实际传给构造方法的参数对象数组
        // 也就是parentClassLoader
        Object[] args = {parentClassLoader};
        // 获取到WebAppClassLoader的有参构造器
        // ## public WebAppClassLoader(ClassLoader parent)；
        Constructor<?> constr = clazz.getConstructor(argTypes);
        // 用刚才获取到的构造期创建一个类加载器并返回
        webAppClassLoader = (WebAppClassLoader) constr.newInstance(args);
        return webAppClassLoader;
    }

    @Override
    public boolean modified() {
        return webAppClassLoader.modified();
    }


    //</editor-fold>
    //<editor-fold desc = "线程相关">
    @Override
    public void start() throws LifecycleException {
        if (isStarted) throw new LifecycleException("webAppLoader is already started");
        logger.debug("LifeCycle : webAppLoader is starting");
        lifecycleHelper.fireLifecycleEvent(START_EVENT, null);
        isStarted = true;
        try {
            webAppClassLoader = createWebAppClassLoader();
            webAppClassLoader.setLocalResource(context.getResources());
            webAppClassLoader.setDelegate(this.delegate);
            webAppClassLoader.start();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        if (reloadable) threadStart();
    }

    @Override
    public void stop() throws LifecycleException {
        if (!isStarted) throw new LifecycleException("webAppLoader is not started");
        logger.debug("LifeCycle : webAppLoader is stopping");
        lifecycleHelper.fireLifecycleEvent(STOP_EVENT, null);
        isStarted = false;
        if (reloadable) threadStop();
        if (webAppClassLoader != null) webAppClassLoader.stop();
        webAppClassLoader = null;
        logger.debug("LifeCycle : webAppLoader is stopped");
    }

    private void threadSleep() {
        try {
            Thread.sleep(checkPeriod);
        } catch (InterruptedException _) {
        }
    }

    // 检查重新部署或者关闭指令
    @Override
    public void run() {
        while (!closeRequried) {
            threadSleep();
            // 如果在这时候收到关闭指令的话，可以直接退出
            if (!isStarted) break;
            // 如果没有检测到web资源的变化，继续
            if (!modified()) continue;
            // 处理重新部署需求
            askForReload();
            break;
        }
    }

    //要求context容器重载
    private void askForReload() {
        Thread reloadThread = new Thread(((Context) vas)::reload);
        reloadThread.start();
    }

    private void threadStart() {
        if (thread != null) return;
        logger.debug("webAppLoader is starting");
        if (!reloadable) throw new IllegalStateException("webapp is not reloadable");
        threadName = this.getClass().getSimpleName() +
                Const.PunctuationMarks.LEFT_BRACKET +
                vas.getName() +
                Const.PunctuationMarks.RIGHT_BRACKET;
        closeRequried = false;
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void threadStop() {
        if (thread == null) return;
        logger.debug("webAppLoader is stopping");
        closeRequried = true;
        // 打断可能处于的threadSleep状态
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException _) {
        } finally {
            thread = null;
        }
    }


    //
    //</editor-fold>

    //<editor-fold desc = "监听器相关">

    /**
     * 当Context 容器发生了状态变化时，通知到这个loader
     *
     * @param evt 事件发生源
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Context context = (Context) evt.getSource();
        String propertyName = evt.getPropertyName();
        if (propertyName.equals("reloadable")) {
            boolean reloadable = (Boolean) evt.getNewValue();
            setReloadable(reloadable);
        } else if (propertyName.equals("contextPath")) {
            String contextPath = (String) evt.getNewValue();
            if (contextPath != null && !contextPath.isEmpty()) {
                if (contextPath.charAt(0) != '/') {
                    contextPath = "/" + contextPath;
                }
            }
        }
    }

    /**
     * 将一个生命周期监听器从组件中移除。
     *
     * @param listener 要移除的生命周期监听器
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycleHelper.removeLifecycleListener(listener);

    }

    /**
     * 将一个生命周期监听器添加到这个生命周期中。这个监听器将被通知
     * 生命周期事件，比如启动和停止。
     *
     * @param listener 要添加的生命周期监听器
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycleHelper.addLifecycleListener(listener);
    }

    /**
     * 找到所有与此生命周期关联的监听器，如果没有相关监听器
     * 则返回一个长度为0的数组
     */
    @Override
    public LifecycleListener[] findLifecycleListener() {
        return lifecycleHelper.findLifecycleListeners();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propHelper.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propHelper.removePropertyChangeListener(listener);
    }

    //</editor-fold>
}
