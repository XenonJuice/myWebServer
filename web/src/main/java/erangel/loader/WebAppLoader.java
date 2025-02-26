package erangel.loader;


import erangel.*;
import erangel.connector.http.Const;
import erangel.connector.http.Const.*;
import erangel.utils.LifecycleHelper;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private long checkPeriod = 10000L;
    // 使用的类加载器的全限定名
    private String loadClass = WebAppClassLoader.class.getName();
    // 组件启动标志位
    private boolean isStarted = false;
    // 后台线程
    private Thread thread = null;
    // 线程名
    private String threadName = "WebAppLoader";

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
        // 如果存在旧容器，则解绑
        if (this.vas != null) {
            this.vas.removePropertyChangeListener(this);
        }
        Vas oldVas = this.vas;
        this.vas = vas;
        propHelper.firePropertyChange("vas", oldVas, this.vas);
        // 绑定新的容器
        if (this.vas != null) {
            setReloadable(true);
            this.vas.addPropertyChangeListener(this);
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
            this.context.addPropertyChangeListener(this);
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
            theadStop();

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
    public void addRepository(String repository) {

    }

    /**
     * 配置类加载器以使用Web应用程序所需的类和JAR文件库。此方法设置两个主要库：
     * <p>
     * 1. 类库位于Web应用程序结构中的`/WEB-INF/classes`。如果该目录存在，
     * 它的路径将添加到类加载器中。如果实际路径不可用，则会将类复制到工作目录中然后进行部署。
     * <p>
     * 2. JAR库位于Web应用程序结构中的`/WEB-INF/lib`。
     * 该目录中找到的所有`.jar`文件将被处理并添加到类加载器中。
     * JAR文件可能会被复制到工作目录，以避免锁定问题，使得更新或移除操作在不干扰的情况下进行。
     * <p>
     * 此方法执行以下关键操作：
     * <p>
     * - 从容器中检索Servlet上下文和工作目录。
     * <p>
     * - 如果存在，则将`/WEB-INF/classes`目录添加到类加载器库以加载类。
     * <p>
     * - 处理`/WEB-INF/lib`目录，识别JAR文件，并将它们作为资源添加到类加载器的JAR库中。
     * <p>
     * - 根据需要处理实际路径或将内容复制到工作目录。
     * <p>
     * 处理任何缺失的目录或无效的目录结构（例如，缺少`/WEB-INF/classes`或`/WEB-INF/lib`）
     * <p>
     * 捕获NamingException和IOException等异常时，输出日志
     */
    private void setRepository() {
        ServletContext servletContext = context.getServletContext();
        if (servletContext == null) return;
        // 取得临时工作目录
        Path tmpWorkDir =
                ((File) servletContext.
                        getAttribute(ServletContext.TEMPDIR)).toPath();
        // FIXME may change it to log output
        System.out.println("临时工作目录的绝对路径为：" + tmpWorkDir.toAbsolutePath());
        // 获取到当前容器关联的目录上下文
        DirContext res = vas.getResources();
        //<editor-fold desc = "WEB-INF/classes">
        // 处理webInfClasses
        DirContext classes;
        try {
            // 找到classes目录下的资源
            classes = (DirContext) res.lookup(webApp.CLASSES);
        } catch (NamingException e) {
            // FIXME log output
            System.out.println("try to find" + webApp.CLASSES + e.getMessage() + Const.commonCharacters.BLANK + webApp.CLASSES + " not found");
            throw new RuntimeException(e);
        }
        if (classes != null) {
            // class资源仓库
            Path classesRepo;
            // 取得绝对路径
            String absoluteClassesPath =
                    servletContext.getRealPath(webApp.CLASSES);
            // 如果可以获取到真实系统路径。直接将一个Path对象指向class资源目录
            if (absoluteClassesPath != null) {
                classesRepo = Path.of(absoluteClassesPath);
            } else {
                // 若无真实路径，在工作目录下创建文件夹并将classes拷贝过去
                classesRepo = tmpWorkDir.resolve
                        (webApp.CLASSES.replace
                                (commonCharacters.SOLIDUS, File.separator));
                try {
                    Files.createDirectories(classesRepo);
                    copyToWorkDir(classes, classesRepo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // FIXME 这里加一个处理结果的日志输出，把/WEB-INF/classes部署到哪里去了之类的
            // 将仓库名，仓库位置添加到webapp类加载器的仓库中
            webAppClassLoader.addRepository(
                    webApp.CLASSES+commonCharacters.SOLIDUS,
                    classesRepo);
        }
        //</editor-fold>
        //<editor-fold desc = "WEB-INF/lib">

        //</editor-fold>


    }

    private void copyToWorkDir(DirContext srcRes, Path workDir) {
        try {
            // 遍历源目录下所有的资源，获得所有资源的 name 和 class 信息
            NamingEnumeration<NameClassPair> enumRes =
                    srcRes.list(Const.commonCharacters.EMPTY);
            while (enumRes.hasMoreElements()) {
                NameClassPair pair = enumRes.nextElement();
                // 拿到当前条目的名称
                String name = pair.getName();
                // 用名称在源目录中lookup，得到这个条目的实际资源对象
                Object object = srcRes.lookup(name);
                // 拼装子路径，表示目标目录下这个子条目对应的完整路径
                Path childPath = workDir.resolve(name);

                if (object instanceof DirContext) {
                    // 说明是子目录，递归处理
                    copyToWorkDir((DirContext) object, childPath);
                } else if (object instanceof InputStream) {
                    // 说明是 JNDI 返回的文件流
                    try (InputStream is = (InputStream) object) {
                        // 确保目标文件所在的父目录也存在，如果不存在就创建
                        if (Files.notExists(childPath.getParent())) {
                            Files.createDirectories(childPath.getParent());
                        }
                        // 把输入流复制到 childPath 对应的文件中
                        Files.copy(is, childPath);
                    }
                }
            }
        } catch (NamingException | IOException e) {
            //FIXME
            throw new RuntimeException(e);
        }

    }

    @Override
    public String[] findRepositories() {
        return new String[0];
    }

    @Override
    public boolean modified() {
        return false;
    }

    //</editor-fold>
    //<editor-fold desc = "线程相关">
    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private void sleep() {
        try {
            Thread.sleep(checkPeriod);
        } catch (InterruptedException _) {
        }
    }

    @Override
    public void run() {

    }

    private void theadStop() {
    }

    private void threadStart() {
    }
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
