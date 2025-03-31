package erangel.listener;

import erangel.base.*;
import erangel.core.DefaultEndpoint;
import erangel.log.BaseLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;

import static erangel.base.Const.webApp.WEB_XML_PATH;

/**
 * 一个Context的生命周期监听器，用于监听启动和关闭Context容器的事件并调用相应的生命周期方法
 */
public class InnerContextListener implements LifecycleListener {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(InnerContextListener.class);
    private Context context = null;
    //</editor-fold
    //<editor-fold desc = "接口实现">
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        context = (Context) event.getLifecycle();
        if (event.getType().equals(Lifecycle.START_EVENT)){
            start();
        }
        if (event.getType().equals(Lifecycle.STOP_EVENT)){
            stop();
        }
    }
    //</editor-fold>
    //<editor-fold desc = "启动组件的生命周期">
    private synchronized void start() {
        logger.info("InnerContextListener : context start");

    }
    private synchronized void stop() {

    }
    //</editor-fold>
    //<editor-fold desc = "加载web.xml配置文件">
    private void loadWebXml() throws IOException {
        InputStream in = null;
        ServletContext servletContext;
        try{
            servletContext = context.getServletContext();
            in = servletContext.getResourceAsStream(WEB_XML_PATH);
            if (in == null) {
                logger.error("web.xml not found");
                return;
            }
        } catch (Exception e){
            logger.error("load web.xml error", e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
