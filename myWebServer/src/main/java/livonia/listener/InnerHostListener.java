package livonia.listener;

import livonia.base.Const;
import livonia.base.Context;
import livonia.base.Host;
import livonia.lifecycle.LifecycleEvent;
import livonia.lifecycle.LifecycleListener;
import livonia.log.BaseLogger;
import livonia.resource.LocalResource;
import livonia.resource.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import static livonia.base.Const.commonCharacters.EMPTY;
import static livonia.base.Const.commonCharacters.SOLIDUS;
import static livonia.base.Const.confInfo.CORE_DIR;
import static livonia.base.Const.webApp.ROOT;
import static livonia.base.Const.webApp.WEB_XML_PATH;
import static livonia.lifecycle.Lifecycle.START_EVENT;
import static livonia.lifecycle.Lifecycle.STOP_EVENT;

/**
 * 一个<code>Host</code>的生命周期监听器，用于监听启动和关闭<code>Host</code>容器的事件并调用相应的生命周期方法
 */
public final class InnerHostListener implements LifecycleListener, Runnable {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(InnerHostListener.class);
    // context容器监听器
    private final String innerContextListener = "livonia.listener.InnerContextListener";
    // context class
    private final String contextClass = "livonia.core.DefaultContext";
    // 检查周期
    private final int checkCycle = 10000;
    // 所有已部署的webApp
    private final ArrayList<String> deployedApps = new ArrayList<>();
    // webXML更新映射
    private final HashMap<String, Long> webXmlUpdateMapping = new HashMap<>();
    // 线程名
    private String threadName = "InnerHostListener";
    // 监听的Host
    private Host host = null;
    // 线程
    private Thread thread = null;
    // 线程是否完成标志位
    private boolean isThreadDone = false;
    //</editor-fold>
    //<editor-fold desc = "接口实现">

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        host = (Host) event.getLifecycle();
        if (event.getType().equals(START_EVENT)) start();
        if (event.getType().equals(STOP_EVENT)) stop();
    }
    //</editor-fold>

    //<editor-fold desc = "被生命周期触发的方法">
    private void start() {
        logger.info("InnerHostListener : host start");
        deployApps();
        threadStart();
    }

    private void stop() {
        threadStop();
        undeployApps();

    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">
    // 返回程序启动时的工作区参数
    private Path appBase() {
        String appBase = host.getAppBase();
        Path path = Paths.get(appBase);
        if (!path.isAbsolute()) {
            String base = System.getProperty(CORE_DIR);
            path = Paths.get(base, appBase);
        }
        return path;
    }

    // 部署webApp
    private void deployApps() {
        logger.debug("InnerHostListener ：try to deploy apps");
        // 获取Host的应用基础目录
        Path appBase = appBase();
        logger.debug("InnerHostListener : appBase is {}", appBase);
        
        // 检查appBase目录是否存在且为目录
        if (!appBase.toFile().exists() || !appBase.toFile().isDirectory()) {
            return;
        }
        
        // 获取appBase目录下的所有文件和子目录
        String[] files = appBase.toFile().list();

        if (files != null) {
            // 遍历每个文件/目录
            for (String file : files) {
                // 跳过META-INF目录（这是特殊的元数据目录）
                if (file.equalsIgnoreCase(
                        (Const.webApp.META_INF)
                                .substring(1))) {
                    continue;
                }
                // 跳过WEB-INF目录（这是特殊的Web应用配置目录）
                if (file.equalsIgnoreCase(
                        (Const.webApp.WEB_INF)
                                .substring(1))) {
                    continue;
                }
                
                // 如果该目录已经在deployedApps列表中，说明已经处理过，跳过
                if (deployedApps.contains(file)) continue;
                
                // 构建该文件的完整路径
                Path path = Paths.get(appBase.toString(), file);
                
                // 只处理目录（Web应用必须是目录）
                if (path.toFile().isDirectory()) {
                    // 检查是否有WEB-INF目录
                    Path webInfoPath = Paths.get(path.toString(), Const.webApp.WEB_INF);
                    if (!webInfoPath.toFile().exists() || !webInfoPath.toFile().isDirectory()) {
                        logger.debug("Skipping {} - no WEB-INF directory", file);
                        continue; 
                    }
                    
                    // 检查该物理目录是否已经被部署
                    boolean alreadyDeployed = false;
                    // 获取所有已部署的Context路径
                    String[] contextPaths = ((Host.Deployer) host).findDeployedApps();
                    // 遍历每个已部署的Context
                    for (String cp : contextPaths) {
                        // 获取Context对象
                        Context ctx = ((Host.Deployer) host).findDeployedApp(cp);
                        if (ctx != null) {
                            // 获取该Context的basePath（物理路径）
                            String basePath = ctx.getBasePath();
                            // 比较basePath是否指向当前扫描的目录
                            // 需要处理三种情况：
                            // 1. basePath直接等于目录名（相对路径）："simpleApp1"
                            // 2. basePath以/结尾的绝对路径："/path/to/simpleApp1/"
                            // 3. basePath以\结尾的绝对路径（Windows）："C:\path\to\simpleApp1\"
                            if (basePath != null && (basePath.equals(file) || 
                                basePath.endsWith("/" + file) || 
                                basePath.endsWith("\\" + file))) {
                                logger.debug("Directory {} already deployed as context path {}", file, cp);
                                alreadyDeployed = true;
                                break;
                            }
                        }
                    }
                    
                    // 如果该目录已经被某个Context使用，跳过部署
                    if (alreadyDeployed) {
                        // 仍然添加到deployedApps列表，避免后续重复检查
                        deployedApps.add(file);
                        continue;
                    }
                    
                    // 构建Context路径（URL路径）
                    // 规则：目录名前加"/"，如simpleApp1 -> /simpleApp1
                    String contextPath = SOLIDUS + file;
                    
                    // 特殊处理：如果目录名是ROOT，则Context路径为空（根应用）
                    if (file.equalsIgnoreCase(ROOT)) {
                        contextPath = EMPTY;
                    }
                    
                    // 检查该Context路径是否已存在
                    if (host.findChild(contextPath) != null) {
                        logger.debug("Context path {} already exists", contextPath);
                        // 添加到deployedApps，避免重复处理
                        deployedApps.add(file);
                        continue;
                    }
                    
                    // 所有检查通过，开始部署
                    logger.info("deploying app : {}", file);
                    // 先添加到deployedApps，标记为已处理
                    deployedApps.add(file);
                    
                    try {
                        // 构建部署URI
                        URI deploymentUri = path.toUri();
                        // 调用Host的install方法进行实际部署
                        ((Host.Deployer) host).install(contextPath, deploymentUri.toURL());
                    } catch (Exception e) {
                        logger.error("deploying app error", e);
                    }
                }
            }
        } else {
            logger.error("appBase {} is not a directory", appBase);
        }

    }

    // 移除部署的webApp
    private void undeployApps() {
        logger.info("InnerHostListener ：try to undeploy apps");
        String[] contextPaths = ((Host.Deployer) host).findDeployedApps();
        for (String cp : contextPaths) {
            logger.info("undeploying app : {}", cp);
            try {
                ((Host.Deployer) host).remove(cp);
            } catch (IOException e) {
                logger.error("undeploying app error", e);
            }
        }
    }

    // 检查每个webApp的webXML是否有变化
    private void checkWebXmlUpdate() {
        Host.Deployer deployer = (Host.Deployer) host;
        // 找到所有已部署的webApp
        String[] contextNames = deployer.findDeployedApps();
        // 遍历所有webApp
        for (String contextName : contextNames) {
            Context context = deployer.findDeployedApp(contextName);
            try {
                ResourceManager resource = context.getResources();
                // 若这里没有获取到资源管理，则说明context中发生异常，暂且略过
                if (resource == null) continue;
                LocalResource webXml = resource.getResource(WEB_XML_PATH);
                // 获取最后修改时间
                long lastModified = webXml.getLastModified();
                // 获取之前已经被存入更新映射的同一文件的最后修改时间
                Long lastUpdate = webXmlUpdateMapping.get(contextName);
                // 如果之前没存.则说明这是一个刚加入的webAPP
                if (lastUpdate == null) {
                    webXmlUpdateMapping.put(contextName, lastModified);
                    // 或者发现时间不一致，这说明已经更新了
                } else if (lastUpdate != lastModified) {
                    logger.debug(":webApp ：{} webXml has been modified ，try to reload", contextName);
                    webXmlUpdateMapping.put(contextName, lastModified);
                    context.reload();
                }
            } catch (RuntimeException e) {
                logger.error("checkWebXmlUpdate error", e);
            }
        }
    }

    //</editor-fold>
    //<editor-fold desc = "线程相关">
    @Override
    public void run() {
        logger.debug("InnerHostListener : thread running");
        while (!isThreadDone) {
            threadSleep();
            deployApps();
            checkWebXmlUpdate();
        }
        logger.debug("InnerHostListener : thread stopping");
    }

    private void threadStart() {
        logger.debug("InnerHostListener : thread start");
        isThreadDone = false;
        threadName = "InnerHostListener[" + host.getName() + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void threadStop() {
        if (thread == null) return;
        logger.debug("InnerHostListener : thread stop");
        isThreadDone = true;
        // 退出当前的阻塞状态
        thread.interrupt();
        // 继续
        try {
            thread.join();
        } catch (InterruptedException _) {
        }
        // 销毁
        thread = null;
    }

    private void threadSleep() {
        try {
            Thread.sleep(checkCycle);
        } catch (InterruptedException _) {
        }
    }
    //</editor-fold>
}
