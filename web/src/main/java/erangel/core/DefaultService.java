package erangel.core;

import erangel.base.*;
import erangel.connector.http.HttpConnector;
import erangel.lifecycle.Lifecycle;
import erangel.lifecycle.LifecycleException;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import erangel.utils.LifecycleHelper;
import org.slf4j.Logger;

public class DefaultService implements Service, Lifecycle {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(DefaultService.class);
    // 描述信息
    private static final String info = "erangel.core.DefaultService";
    // 与此Service关联的容器
    private Vas vas = null;
    // 是否完成了初始化
    private boolean initialized = false;
    // 是否开始了
    private boolean started = false;
    // 此Service名
    private String name = "";
    // 生命周期助手
    private LifecycleHelper helper = new LifecycleHelper(this);
    // 持有此Service的Server
    private Server server = null;
    // 此Service持有的连接器集合
    private HttpConnector[] connectors = null;

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public Vas getVas() {
        return vas;
    }

    @Override
    public void setVas(Vas vas) {
        Vas old = this.vas;
        ((Engine) old).setService(null);
        this.vas = vas;
        ((Engine) vas).setService(this);
        if (started) {
            try {
                logger.debug("Engine changed, restarting");
                ((Lifecycle) vas).start();
            } catch (LifecycleException e) {
                logger.error("start Engine failed", e);
            }
        }
        synchronized (connectors) {
            for (HttpConnector connector : connectors) connector.setVas(vas);
        }
        if (started) {
            try {
                logger.debug("Engine changed, shutdown the old Engine");
                ((Lifecycle) old).stop();
            } catch (LifecycleException e) {
                logger.error("stop Vas failed", e);
            }
        }
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public void addConnector(Connector connector) {
        synchronized (connectors) {
            if (connectors.length == 0) {
                connectors = new HttpConnector[1];
                connectors[0] = (HttpConnector) connector;
            } else {
                ((HttpConnector) connector).setVas(vas);
                ((HttpConnector) connector).setService(this);
                HttpConnector[] tmp = new HttpConnector[connectors.length + 1];
                System.arraycopy(connectors, 0, tmp, 0, connectors.length);
                tmp[connectors.length] = (HttpConnector) connector;
                connectors = tmp;
            }

            // 初始化连接器
            if (initialized) {
                try {
                    logger.info("DefaultService addConnector : initialize connector");
                    ((HttpConnector) connector).initialize();
                } catch (LifecycleException e) {
                    logger.error("initialize connector failed", e);
                    e.printStackTrace(System.err);
                }
            }

            // 启动连接器
            if (started) {
                try {
                    logger.info("DefaultService addConnector : start connector");
                    ((HttpConnector) connector).start();
                } catch (LifecycleException e) {
                    logger.error("start connector failed", e);
                }
            }
        }
    }

    @Override
    public Connector[] findConnectors() {
        return connectors;
    }

    @Override
    public void removeConnector(Connector connector) {
        synchronized (connectors) {
            int j = -9;
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] == connector) {
                    j = i;
                    break;
                }
            }
            if (j < 0) return;
            if (started) {
                try {
                    logger.info("DefaultService removeConnector : stop connector");
                    connectors[j].stop();
                } catch (LifecycleException e) {
                    logger.error("stop connector failed", e);
                }
            }
            connectors[j].setVas(null);
            connectors[j].setService(null);
            HttpConnector[] tmp = new HttpConnector[connectors.length - 1];
            System.arraycopy(connectors, 0, tmp, 0, j);
            System.arraycopy(connectors, j + 1, tmp, j, connectors.length - j - 1);
            connectors = tmp;
        }
    }
    //</editor-fold>
    //<editor-fold desc = "生命周期相关">


    @Override
    public void initialize() throws LifecycleException {
        if (initialized) throw new LifecycleException("DefaultService : already initialized");
        initialized = true;
        synchronized (connectors) {
            for (HttpConnector connector : connectors) {
                try {
                    logger.debug("DefaultService initialize : initialize connector");
                    connector.initialize();
                } catch (LifecycleException e) {
                    logger.error("initialize connector failed", e);
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        helper.removeLifecycleListener(listener);
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        helper.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListener() {
        return helper.findLifecycleListeners();
    }

    @Override
    public void start() throws LifecycleException {
        if (started) throw new LifecycleException("DefaultService : already started");
        helper.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);
        // 这里在控制台输出
        System.out.println("DefaultService : " + name + "is starting...");
        helper.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;

        // 启动Engine
        if (vas != null) {
            synchronized (vas) {
                try {
                    logger.info("DefaultService start : start Engine");
                    ((Lifecycle) vas).start();
                } catch (LifecycleException e) {
                    logger.error("start Engine failed", e);
                }
            }
        }

        // 启动连接器
        synchronized (connectors) {
            for (HttpConnector connector : connectors) {
                try {
                    logger.info("DefaultService start : start connector");
                    connector.start();
                } catch (LifecycleException e) {
                    logger.error("start connector failed", e);
                }
            }
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("DefaultService : not started");
        helper.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);
        helper.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        // 这里在控制台输出
        System.out.println("DefaultService : " + name + "is stopping...");
        started = false;

        // 关闭连接器
        synchronized (connectors) {
            for (HttpConnector connector : connectors) {
                try {
                    logger.info("DefaultService stop : stop connector");
                    connector.stop();
                } catch (LifecycleException e) {
                    logger.error("stop connector failed", e);
                }
            }
        }

        // 关闭Engine
        if (vas != null) {
            synchronized (vas) {
                try {
                    logger.info("DefaultService stop : stop Engine");
                    ((Lifecycle) vas).stop();
                } catch (LifecycleException e) {
                    logger.error("stop Engine failed", e);
                }
            }
        }
        helper.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, null);
    }
    //</editor-fold>
}
