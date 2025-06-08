package livonia.core;

import livonia.base.Context;
import livonia.base.Host;
import livonia.base.Mapper;
import livonia.base.Vas;
import livonia.checkpoints.HostCheckpoint;
import livonia.lifecycle.Lifecycle;
import livonia.lifecycle.LifecycleException;
import livonia.lifecycle.LifecycleListener;

import java.io.IOException;
import java.net.URL;

import static livonia.base.Const.commonCharacters.SOLIDUS;

public class DefaultHost extends VasBase implements Host, Host.Deployer {
    // context对象
    private final String defaultContextClass = "livonia.core.DefaultContext";
    // 部署器
    private final Deployer deployer = new HostDeployer(this);
    // 映射器对象
    private String defaultHostMapper = "livonia.mapper.HostMapper";
    //<editor-fold desc = "attr">
    // 程序根目录
    private String appBase = "";
    // 是否自动部署
    private boolean autoDeploy = true;
    // context容器监听器
    private String innerContextListener = "livonia.listener.InnerContextListener";
    // 工作区
    private String workDir = "";

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public DefaultHost() {
        channel.setBasicCheckpoint(new HostCheckpoint());
    }

    //</editor-fold>
    //<editor-fold desc = "getter && setter">

    public String getMapper() {
        return defaultHostMapper;
    }

    //<editor-fold desc = "映射器">
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
        this.defaultHostMapper = mapper.getClass().getName();
    }
    //</editor-fold>

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public boolean isAutoDeploy() {
        return autoDeploy;
    }

    public void setAutoDeploy(boolean autoDeploy) {
        this.autoDeploy = autoDeploy;
    }

    public String getInnerContextListener() {
        return innerContextListener;
    }

    public void setInnerContextListener(String innerContextListener) {
        this.innerContextListener = innerContextListener;
    }

    //</editor-fold>
    //<editor-fold desc = "生命周期">
    @Override
    public void start() throws LifecycleException {
        logger.debug("Starting host:{}", name);
        Mapper mapper =setMapper(defaultHostMapper);
        mapper.setVas(this);
        super.start();
    }

    @Override
    public void stop() throws LifecycleException {
        logger.debug("Stopping host:{}", name);
        super.stop();
    }

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public Context map(String uri) {
        logger.debug("Mapping uri:{}", uri);
        if (uri == null) return null;
        Context context;
        String mappedUri = uri;
        while (true) {
            context = (Context) findChild(mappedUri);
            if (context != null) break;
            int index = mappedUri.lastIndexOf(SOLIDUS);
            if (index == -1) break;
            mappedUri = mappedUri.substring(0, index);
        }
        return context;
    }

    @Override
    public String getAppBase() {
        return appBase;
    }

    @Override
    public void setAppBase(String appBase) {
        this.appBase = appBase;
    }

    @Override
    public void install(String contextPath, URL webApp) throws Exception {
        deployer.install(contextPath, webApp);
    }

    @Override
    public Context findDeployedApp(String contextPath) {
        return deployer.findDeployedApp(contextPath);
    }

    @Override
    public String[] findDeployedApps() {
        return deployer.findDeployedApps();
    }

    @Override
    public void remove(String contextPath) throws IOException {
        deployer.remove(contextPath);
    }


    public void start(String contextPath) throws IOException {
        deployer.start(contextPath);
    }


    public void stop(String contextPath) throws IOException {
        deployer.stop(contextPath);
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">
    public void addChild(Vas child) {
        if (!(child instanceof Context)) {
            throw new IllegalArgumentException("child must be a context");
        }
        super.addChild(child);
    }

    private void beforeInstall(String contextPath, URL webApp) {
        if (contextPath == null || webApp == null) throw new IllegalArgumentException(
                "contextPath and webApp must not be null"
        );
        if (!contextPath.isEmpty() && !contextPath.startsWith(SOLIDUS)) throw new IllegalArgumentException(
                "contextPath must start with /"
        );
        if (findDeployedApp(contextPath) != null) throw new IllegalArgumentException(
                "contextPath has been deployed"
        );
    }

    //</editor-fold>
    //<editor-fold desc = "内部类">
    public class HostDeployer implements Deployer {
        private final DefaultHost host;

        public HostDeployer(DefaultHost host) {
            this.host = host;
        }

        @Override
        public void install(String contextPath, URL webApp) throws Exception {
            beforeInstall(contextPath, webApp);
            logger.debug("Installing web app:{} from :{}", webApp, contextPath);
            String webAppUrl = webApp.toString();
            String basePath = null;
            logger.debug("webAppUrl:{}", webAppUrl);
            if (webAppUrl.startsWith("file:")) {
                basePath = webAppUrl.substring(5);
            } else if (webAppUrl.startsWith("file://")) {
                basePath = webAppUrl.substring(7);
            } else {
                throw new IllegalArgumentException("webAppUrl must start with file:");
            }

            try {
                // 创建 Context对象
                Class<? extends Context> typedContextClass = Class.forName(defaultContextClass).asSubclass(Context.class);
                Context contextInstance = typedContextClass.getDeclaredConstructor().newInstance();
                contextInstance.setPath(contextPath);
                contextInstance.setBasePath(basePath);

                // 创建 InnerContextListener对象
                Class<?> listener = Class.forName(innerContextListener);
                LifecycleListener listenerInstance = (LifecycleListener) listener.getDeclaredConstructor().newInstance();
                // 设置监听器
                ((Lifecycle) contextInstance).addLifecycleListener(listenerInstance);
                // 将当前Context作为子容器注入当前关联的Host
                host.addChild(contextInstance);
            } catch (Exception e) {
                logger.error("Deploy web app error", e);
                throw new Exception("Deploy web app error", e);
            }
        }

        @Override
        public Context findDeployedApp(String contextPath) {
            return (Context) host.findChild(contextPath);
        }

        @Override
        public String[] findDeployedApps() {
            Vas[] children = host.findChildren();
            String[] deployedApps = new String[children.length];
            for (int i = 0; i < children.length; i++) {
                deployedApps[i] = children[i].getName();
            }
            return deployedApps;
        }

        @Override
        public void remove(String contextPath) throws IOException {
            if (contextPath == null)
                throw new IllegalArgumentException("host : contextPath must not be null");
            if (!(contextPath.isEmpty() || contextPath.startsWith(SOLIDUS)))
                throw new IllegalArgumentException("host : contextPath must start with /");
            // 找到要移除的webAPP
            Context deployedApp = findDeployedApp(contextPath);
            if (deployedApp == null) throw new IllegalArgumentException("host : contextPath has not been deployed");
            logger.debug("Removing web app:{}", contextPath);
            host.removeChild(deployedApp);
        }


        @Override
        public void start(String contextPath) throws IOException {
            if (contextPath == null)
                throw new IllegalArgumentException("host : contextPath must not be null");
            if (!(contextPath.isEmpty() || contextPath.startsWith(SOLIDUS)))
                throw new IllegalArgumentException("host : contextPath must start with /");
            Context deployedApp = findDeployedApp(contextPath);
            if (deployedApp == null) throw new IllegalArgumentException("host : contextPath has not been deployed");
            logger.debug("Starting web app:{}", contextPath);
            if (deployedApp instanceof Lifecycle) {
                try {
                    ((Lifecycle) deployedApp).start();
                } catch (LifecycleException e) {
                    logger.error("start web app failed", e);
                    throw new IOException("start web app failed", e);
                }
            } else {
                logger.warn("web app:{} is not Lifecycle", contextPath);
            }

        }

        @Override
        public void stop(String contextPath) throws IOException {
            if (contextPath == null)
                throw new IllegalArgumentException("host : contextPath must not be null");
            if (!(contextPath.isEmpty() || contextPath.startsWith(SOLIDUS)))
                throw new IllegalArgumentException("host : contextPath must start with /");
            Context deployedApp = findDeployedApp(contextPath);
            if (deployedApp == null) throw new IllegalArgumentException("host : contextPath has not been deployed");
            logger.debug("Stopping web app:{}", contextPath);
            if (deployedApp instanceof Lifecycle) {
                try {
                    ((Lifecycle) deployedApp).stop();
                } catch (LifecycleException e) {
                    logger.error("stop web app failed", e);
                    throw new IOException("stop web app failed", e);
                }
            }
        }
    }
    //</editor-fold>
}
