package erangel.listener;

import erangel.base.Engine;
import erangel.lifecycle.LifecycleEvent;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import org.slf4j.Logger;

import static erangel.lifecycle.Lifecycle.START_EVENT;
import static erangel.lifecycle.Lifecycle.STOP_EVENT;

/**
 * 一个<code>Engine</code>的生命周期监听器，用于监听启动和关闭<code>Engine</code>容器的事件并调用相应的生命周期方法
 */
public final class InnerEngineListener implements LifecycleListener {
    //<editor-fold desc = "attr">
    // 监听的Engine
    private Engine engine = null;
    private static final Logger logger = BaseLogger.getLogger(InnerEngineListener.class);

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        engine = (Engine) event.getLifecycle();
        if (event.getType().equals(START_EVENT)) start();
        else if (event.getType().equals(STOP_EVENT)) stop();
    }
    //</editor-fold>

    //<editor-fold desc = "生命周期相关">
    private void start() {
        logger.info("start engine");
    }

    private void stop() {
        logger.info("stop engine");
    }
    //</editor-fold>
}
