package erangel.loader;

import erangel.Context;
import erangel.Loader;
import erangel.Vas;

public class WebAppLoader implements Loader {
    private Vas vas = null;
    private Context context = null;
    private boolean delegate = false;
    private boolean reloadable = false;
    private ClassLoader parentClassLoader = null;
    private WebAppClassLoader webAppClassLoader = null;

    public String getLoadClass() {
        return this.loadClass;
    }

    public void setLoadClass(String loadClass) {
        this.loadClass = loadClass;
    }

    // TODO add a suitable class loader
    private String loadClass = null;

    public WebAppLoader() {
        this(null);
    }
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
