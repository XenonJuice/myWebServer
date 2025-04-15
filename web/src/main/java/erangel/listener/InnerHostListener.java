package erangel.listener;

import erangel.base.*;
import erangel.lifecycle.LifecycleEvent;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import erangel.resource.LocalResource;
import erangel.resource.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static erangel.base.Const.commonCharacters.EMPTY;
import static erangel.base.Const.commonCharacters.SOLIDUS;
import static erangel.base.Const.webApp.ROOT;
import static erangel.lifecycle.Lifecycle.START_EVENT;
import static erangel.lifecycle.Lifecycle.STOP_EVENT;

/**
 * 一个<code>Host</code>的生命周期监听器，用于监听启动和关闭<code>Host</code>容器的事件并调用相应的生命周期方法
 */
public final class InnerHostListener implements LifecycleListener, Runnable {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(InnerHostListener.class);
    // context容器监听器
    private final String innerContextListener = "erangel.listener.InnerContextListener";
    // context class
    private final String contextClass = "erangel.core.DefaultContext";
    // 检查周期
    private final int checkCycle = 100000;
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
            String base = System.getProperty("simpleWebService.appBase");
            path = Paths.get(base, appBase);
        }
        return path;
    }

    // 部署webApp
    private void deployApps() {
        logger.info("InnerHostListener ：try to deploy apps");
        Path appBase = appBase();
        if (!appBase.toFile().exists() || !appBase.toFile().isDirectory()) {
            return;
        }
        String[] files = appBase.toFile().list();

        if (files != null) {
            for (String file : files) {
                if (file.equalsIgnoreCase(
                        (Const.webApp.META_INF)
                                .substring(1))) {
                    continue;
                }
                if (file.equalsIgnoreCase(
                        (Const.webApp.WEB_INF)
                                .substring(1))) {
                    continue;
                }
                if (deployedApps.contains(file)) continue;
                Path path = Paths.get(appBase.toString(), file);
                if (path.toFile().isDirectory()) {
                    deployedApps.add(file);
                    Path webInfoPath = Paths.get(path.toString(), Const.webApp.WEB_INF);
                    if (!webInfoPath.toFile().exists() || !webInfoPath.toFile().isDirectory()) {
                        return;
                    }
                    // 拼接contextPath
                    String contextPath = SOLIDUS + file;
                    // 判断是否为更路径
                    if (file.equalsIgnoreCase(ROOT)) {
                        contextPath = EMPTY;
                    }
                    // 若已部署则跳过
                    if (host.findChild(contextPath) != null) continue;
                    logger.info("deploying app : {}", file);
                    try {
                        String canonicalPath = path.toFile().getCanonicalPath();
                        URI deploymentUri = path.toUri();
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
                Map<String, List<LocalResource>> configMap = resource.getConfigMap();
                // 从资源管理中拿到webXML引用
                LocalResource webXml = configMap.get(Const.webApp.WEB_XML).getFirst();
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
