package livonia.core;

import livonia.base.*;
import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;
import livonia.lifecycle.Lifecycle;
import livonia.lifecycle.LifecycleException;
import livonia.lifecycle.LifecycleListener;
import livonia.log.BaseLogger;
import livonia.utils.LifecycleHelper;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * VasBase类是对Vas和Lifecycle接口的抽象实现。
 * 提供了容器管理和生命周期控制所需的基本功能和结构，
 */
public abstract class VasBase implements Vas, Lifecycle, Channel {

    //<editor-fold desc = "attr">
    // logger
    protected static final Logger logger = BaseLogger.getLogger(VasBase.class);
    // 本容器的子容器合集
    protected final Map<String, Vas> children = new HashMap<>();
    // 与此容器关联的映射器集合
    protected final HashMap<String, Mapper> mappers = new HashMap<>();
    // 生命周期助手
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    // 与此容器相关联的channel
    protected Channel channel = new DefaultChannel(this);
    // 与此容器正在关联的映射器
    protected Mapper mapper = null;
    // 容器名
    protected String name = "";
    // 父容器
    protected Vas parent = null;
    // 父加载器
    protected ClassLoader parentClassLoader = null;
    // 加载器
    protected Loader loader = null;
    // 容器启动标志位
    protected boolean started = false;

    /**
     * 检索此容器的相关 {@code Loader} 实例。
     * 如果当前容器没有相关 {@code Loader}，它会尝试从父容器获取
     * {@code Loader}。如果在任何级别都未找到加载器，它将返回 {@code null}。
     *
     * @return 与此容器或其父容器相关联的 {@code Loader} 实例，
     * 如果未找到加载器，则返回 {@code null}。
     */
    //</editor-fold>
    //<editor-fold dest="getter & setter">
    public Loader getLoader() {
        if (loader != null) return loader;
        if (parent != null) return parent.getLoader();
        return null;
    }

    /**
     * 设置当前容器的加载器，并建立其与该容器的关系。
     * 如果加载器非空，则在加载器上设置当前容器实例。
     * 如果加载器实现了{@code Lifecycle}并且容器已经启动，
     * 加载器也将被启动。
     *
     * @param loader 要与容器关联的{@code Loader}实例。
     *               可以为null以解除与加载器的关联。
     */
    public synchronized void setLoader(Loader loader) {
        if (this.loader == loader) return;
        this.loader = loader;
        if (loader != null) {
            loader.setVas(this);
        }
        if (started && loader instanceof Lifecycle) {
            try {
                ((Lifecycle) loader).start();
            } catch (LifecycleException e) {
                logger.error("start loader failed", e);
            }
        }
    }

    /**
     * 返回当前容器的关联 {@code Channel} 实例
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * 返回容器名称
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 设定容器名称
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 返回父容器
     */
    @Override
    public Vas getParent() {
        return parent;
    }

    /**
     * 设定父容器
     */
    @Override
    public void setParent(Vas parent) {
        this.parent = parent;
    }

    /**
     * 获取与此容器关联的父类加载器。
     *
     * @return 父 {@code ClassLoader} 实例，如果未定义父类加载器，则返回 {@code ClassLoader.SystemClassLoader}。
     */
    @Override
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null) return parentClassLoader;
        if (parent != null) return parent.getParentClassLoader();
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * 设置此组件的父类加载器。
     *
     * @param classLoader 要设置为父类加载器的 ClassLoader
     */
    @Override
    public void setParentClassLoader(ClassLoader classLoader) {
        ClassLoader old = this.parentClassLoader;
        this.parentClassLoader = classLoader;
    }
    //</editor-fold>
    //<editor-fold desc = "生命周期">

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

    /**
     * 在使用该组件的任何公共方法之前，先调用此方法。
     * 向所有已注册的监听器发送一个类型为
     * START_EVENT 的 LifecycleEvent。
     *
     * @throws LifecycleException 当组件发生严重错误导致不可恢复使用时抛出该错误
     */
    @Override
    public synchronized void start() throws LifecycleException {
        if (started) throw new LifecycleException("container already started");
        started = true;
        lifecycleHelper.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);
        // 激活此组件的下属组件
        if (loader != null && (loader instanceof Lifecycle)) {
            ((Lifecycle) loader).start();
        }
        Mapper[] mappers = findMappers();
        for (Mapper mapper : mappers) {
            if (mapper instanceof Lifecycle) {
                try {
                    ((Lifecycle) mapper).start();
                } catch (LifecycleException e) {
                    logger.error("start mapper failed", e);
                    throw new RuntimeException(e);
                }
            }
        }
        // 激活此容器的容器
        Vas[] children = findChildren();
        for (Vas child : children) {
            if (child instanceof Lifecycle) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    logger.error("start child failed", e);
                }
            }
        }
        if (channel instanceof Lifecycle) ((Lifecycle) channel).start();
        lifecycleHelper.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        lifecycleHelper.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
    }

    /**
     * 终止对组件公开方法的使用。
     * 向所有已注册的监听器发送一个类型为
     * STOP_EVENT 的 LifecycleEvent。
     *
     * @throws LifecycleException 关闭失败时抛出该错误
     */
    @Override
    public synchronized void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("container not started");
        lifecycleHelper.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);
        lifecycleHelper.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        started = false;
        if (channel instanceof Lifecycle) ((Lifecycle) channel).stop();
        // 停止关联的子容器
        Vas[] children = findChildren();
        for (Vas child : children) {
            if (child instanceof Lifecycle) {
                ((Lifecycle) child).stop();
            }
        }
        // 停止关联的子组件
        if (loader != null && (loader instanceof Lifecycle)) {
            ((Lifecycle) loader).stop();
        }
        Mapper[] mappers = findMappers();
        for (Mapper mapper : mappers) {
            if (mapper instanceof Lifecycle) {
                ((Lifecycle) mapper).stop();
            }
        }
        lifecycleHelper.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, null);
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">

    /**
     * 设定此容器的子容器
     */
    @Override
    public void addChild(Vas child) {
        synchronized (children) {
            if (children.containsKey(child.getName())) throw new IllegalArgumentException("child name already exists");
            child.setParent(this);
            if (started && child instanceof Lifecycle) {
                try {
                    ((Lifecycle) child).start();
                } catch (LifecycleException e) {
                    logger.error("start child failed", e);
                    throw new RuntimeException(e);
                }
            }
            children.put(child.getName(), child);
        }
    }

    /**
     * 查找所有与此容器关联的子容器
     */
    @Override
    public Vas[] findChildren() {
        synchronized (children) {
            if (children.isEmpty()) return new Vas[0];
            return children.values().toArray(new Vas[0]);
        }
    }

    /**
     * 根据名称查找此容器的特定子容器
     */
    @Override
    public Vas findChild(String name) {
        synchronized (children) {
            if (!children.containsKey(name)) return null;
            return children.get(name);
        }
    }

    /**
     * 移除此容器的特定子容器
     */
    @Override
    public void removeChild(Vas child) {
        synchronized (children) {
            if (!children.containsKey(child.getName())) return;
            children.remove(child.getName());
            if (started && child instanceof Lifecycle) {
                try {
                    ((Lifecycle) child).stop();
                } catch (LifecycleException e) {
                    logger.error("stop child failed", e);
                    throw new RuntimeException(e);
                }
            }
            child.setParent(null);
        }
    }

    /**
     * 将传入的HTTP请求映射到相应的容器或资源。
     *
     * @param req          要处理的HTTP请求。
     * @param writeRequest 一个标志，指示是否应写入请求详细信息。
     * @return 映射的容器或资源作为Vas对象，如果未找到映射则返回null。
     */
    @Override
    public Vas map(HttpRequest req, boolean writeRequest) {
        Mapper mapper = findMapper(req.getUri());
        return mapper.map(req, writeRequest);
    }

    /**
     * 为此容器添加映射器
     */
    @Override
    public void addMapper(Mapper mapper) {
        synchronized (mappers) {
            mapper.setVas(this);
            mappers.put(this.name, mapper);
        }
        if (mappers.size() == 1) {
            this.mapper = mapper;
        } else this.mapper = null;
    }

    /**
     * 查找并检索与此容器相关的所有映射器。
     *
     * @return 一个 {@code Mapper} 对象的数组，代表与此容器相关的映射器。
     * 如果未找到映射器，则返回一个空数组。
     */
    @Override
    public Mapper[] findMappers() {
        synchronized (mappers) {
            Mapper[] m = new Mapper[mappers.size()];
            return mappers.values().toArray(m);
        }
    }

    /**
     * 查找并返回与给定名称相关的映射器。
     *
     * @param name 要搜索的映射器名称
     * @return 与指定名称相关的映射器，如果未找到则返回 null
     */
    @Override
    public Mapper findMapper(String name) {
        if (mapper != null) return mapper;
        synchronized (mappers) {
            return mappers.get(name);
        }
    }

    //</editor-fold>
    //<editor-fold desc = "Channel相关">
    @Override
    public Checkpoint getBasicCheckpoint() {
        return channel.getBasicCheckpoint();
    }

    @Override
    public void setBasicCheckpoint(Checkpoint checkpoint) {
        channel.setBasicCheckpoint(checkpoint);
    }

    @Override
    public synchronized void addCheckpoint(Checkpoint checkpoint) {
        channel.addCheckpoint(checkpoint);
    }

    @Override
    public Checkpoint[] getCheckpoints() {
        return channel.getCheckpoints();
    }

    @Override
    public synchronized void removeCheckpoint(Checkpoint checkpoint) {
        channel.removeCheckpoint(checkpoint);
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        channel.process(request, response);
    }
    //</editor-fold>
}
