package erangel.loader;


import erangel.*;
import erangel.utils.LifecycleHelper;

public class WebAppLoader implements Loader, Runnable, Lifecycle {
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
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
    // 被这个loader组件管理的真正的加载器
    private WebAppClassLoader webAppClassLoader = null;
    // 定期检查时常
    private long checkPeriod = 10000L;
    // 使用的类加载器的全限定名
    private String loadClass = WebAppClassLoader.class.getName();
    // 线程名
    private String threadName = "WebAppLoader";
    // 组件启动标志位
    private boolean started = false;


    // 默认构造器 调用可以传入一个父加载器的构造器
    public WebAppLoader() {
        this(null);
    }

    // 传入一个父加载器的构造器
    public WebAppLoader(ClassLoader parent) {
        this.parentClassLoader = parent;
    }


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
        this.vas = vas;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public boolean getDelegate() {
        return this.delegate;
    }

    @Override
    public void setDelegate(boolean delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean getReloadable() {
        return this.reloadable;
    }

    @Override
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    @Override
    public void addRepository(String repository) {

    }

    public String getLoadClass() {
        return this.loadClass;
    }

    public void setLoadClass(String loadClass) {
        this.loadClass = loadClass;
    }

    @Override
    public String[] findRepositories() {
        return new String[0];
    }

    @Override
    public boolean modified() {
        return false;
    }


    private void sleep() {
        try {
            Thread.sleep(checkPeriod);
        } catch (InterruptedException _) {
            ;
        }
    }

    /**
     * 将一个生命周期监听器从组件中移除。
     *
     * @param listener 要移除的生命周期监听器
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {

    }

    /**
     * 将一个生命周期监听器添加到这个生命周期中。这个监听器将被通知
     * 生命周期事件，比如启动和停止。
     *
     * @param listener 要添加的生命周期监听器
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {

    }

    /**
     * 找到所有与此生命周期关联的监听器，如果没有相关监听器
     * 则返回一个长度为0的数组
     */
    @Override
    public LifecycleListener[] findLifecycleListener() {
        return new LifecycleListener[0];
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void run() {

    }
}
