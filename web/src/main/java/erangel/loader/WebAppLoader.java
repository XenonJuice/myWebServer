package erangel.loader;

import erangel.Context;
import erangel.Loader;
import erangel.Vas;

public class WebAppLoader implements Loader {
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
    // 使用的类加载器的全限定名
    private String loadClass = WebAppClassLoader.class.getName();

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



    private void sleep(){
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException _) {;}
    }
}
