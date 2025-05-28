package erangel.listener;

import erangel.XMLParse.MiniDigester;
import erangel.XMLParse.WebRuleSet;
import erangel.base.Channel;
import erangel.base.Checkpoint;
import erangel.base.Context;
import erangel.base.Vas;
import erangel.core.VasBase;
import erangel.filter.FilterDef;
import erangel.filter.FilterMap;
import erangel.lifecycle.Lifecycle;
import erangel.lifecycle.LifecycleEvent;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;

import static erangel.base.Const.webApp.WEB_XML_PATH;

/**
 * 一个<code>Context</code>的生命周期监听器，用于监听启动和关闭<code>Context</code>容器的事件并调用相应的生命周期方法
 */
public final class InnerContextListener implements LifecycleListener {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(InnerContextListener.class);
    private Context context = null;
    private final MiniDigester digester = createDigester();
    private boolean noProblem = false;

    //</editor-fold
    //<editor-fold desc = "接口实现">
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        context = (Context) event.getLifecycle();
        if (event.getType().equals(Lifecycle.START_EVENT)) {
            start();
        }
        if (event.getType().equals(Lifecycle.STOP_EVENT)) {
            stop();
        }
    }

    //</editor-fold>
    //<editor-fold desc = "启动组件的生命周期">
    private synchronized void start() {
        logger.info("InnerContextListener : context start");
        context.setConfigured(false);
        noProblem = true;
        try {
            loadWebXml();
        } catch (IOException e) {
            logger.error("load web.xml error", e);
            noProblem = false;
        }
        // 输出一下绑定的检查点列表
        Channel channel = ((VasBase) context).getChannel();
        if (channel != null) {
            Checkpoint[] checkpoints = channel.getCheckpoints();
            if (checkpoints != null) {
                logger.debug("current checkpoint info start :");
                for (Checkpoint c : checkpoints) {
                    logger.debug(c.getInfo());
                }
                logger.debug("current checkpoint info end :");
            }
        }
        context.setConfigured(noProblem);

    }

    private synchronized void stop() {
        logger.info("InnerContextListener : context stop");
        //
        Vas[] children = context.findChildren();
        for (Vas c : children) {
            context.removeChild(c);
        }
        //
        String[] applicationListeners = context.findApplicationListeners();
        for (String l : applicationListeners) {
            context.removeApplicationListener(l);
        }
        //
        FilterDef[] filterDefs = context.findFilterDefs();
        for (FilterDef filterDef : filterDefs) {
            context.removeFilterDef(filterDef);
        }
        //
        FilterMap[] filterMaps = context.findFilterMaps();
        for (FilterMap filterMap : filterMaps) {
            context.removeFilterMap(filterMap);
        }
        //
        String[] servletMappings = context.findServletMappings();
        for (String s : servletMappings) {
            context.removeServletMapping(s);
        }
        noProblem = true;
    }

    //</editor-fold>
    //<editor-fold desc = "加载web.xml配置文件">
    private void loadWebXml() throws IOException {
        InputStream in = null;
        ServletContext servletContext;
        try {
            servletContext = context.getServletContext();
            in = servletContext.getResourceAsStream(WEB_XML_PATH);
            if (in == null) {
                logger.error("web.xml not found");
                return;
            }
            synchronized (digester) {
                try {
                    new WebRuleSet().addRuleInstances(digester);
                    in = servletContext.getResourceAsStream(WEB_XML_PATH);
                    digester.clear();
                    // 这里要把上下文（也就是<web-app>）先放进去
                    digester.push(context);
                    digester.parse(in);
                } catch (Exception e) {
                    logger.error("load web.xml error", e);
                    noProblem = false;
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (Exception e) {
                        logger.error("An IOException happened when closing web.xml ：", e);
                    }

                }
            }
        } catch (Exception e) {
            logger.error("load web.xml error", e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    //</editor-fold>
    //<editor-fold desc = "创建webXml解析器材">
    private MiniDigester createDigester() {
        MiniDigester mini = new MiniDigester();
        mini.setNamespaceAware(true);
        return mini;
    }
    //</editor-fold>
}
