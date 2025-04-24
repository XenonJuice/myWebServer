package erangel.core;

import erangel.base.*;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.lifecycle.Lifecycle;
import erangel.lifecycle.LifecycleException;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import erangel.utils.LifecycleHelper;
import org.slf4j.Logger;

public class DefaultChannel implements Channel, Lifecycle, VasManager {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(DefaultChannel.class);
    protected LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
    protected Vas vas = null;
    protected Checkpoint basicCheckpoint = null;
    // 与该通道绑定的所有检查点的集合
    protected Checkpoint[] checkpoints = new Checkpoint[0];
    protected boolean stared = false;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public DefaultChannel() {
        this(null);
    }

    public DefaultChannel(Vas vas) {
        setVas(vas);
    }

    //</editor-fold>
    //<editor-fold desc = "生命周期">
    @Override
    public synchronized void start() throws LifecycleException {
        if (stared) throw new LifecycleException("DefaultChannel : already started");
        lifecycleHelper.fireLifecycleEvent(Lifecycle.BEFORE_START_EVENT, null);
        stared = true;
        for (Checkpoint cp : checkpoints) {
            if (cp instanceof Lifecycle) ((Lifecycle) cp).start();
        }
        if (basicCheckpoint instanceof Lifecycle) ((Lifecycle) basicCheckpoint).start();
        lifecycleHelper.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        lifecycleHelper.fireLifecycleEvent(Lifecycle.AFTER_START_EVENT, null);
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        if (!stared) throw new LifecycleException("DefaultChannel : not started");
        lifecycleHelper.fireLifecycleEvent(Lifecycle.BEFORE_STOP_EVENT, null);
        lifecycleHelper.fireLifecycleEvent(Lifecycle.STOP_EVENT, null);
        stared = false;
        if (basicCheckpoint != null && basicCheckpoint instanceof Lifecycle) ((Lifecycle) basicCheckpoint).stop();
        for (Checkpoint cp : checkpoints) {
            if (cp instanceof Lifecycle) ((Lifecycle) cp).stop();
        }
        if (basicCheckpoint instanceof Lifecycle) ((Lifecycle) basicCheckpoint).stop();
        lifecycleHelper.fireLifecycleEvent(Lifecycle.AFTER_STOP_EVENT, null);
    }

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

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public Vas getVas() {
        return vas;
    }

    @Override
    public void setVas(Vas vas) {
        this.vas = vas;
    }

    /**
     * 获取与通道相关联的基本检查点。
     *
     * @return 与通道关联的基本 {@code Checkpoint} 实例，如果没有设置则返回 {@code null}。
     */
    @Override
    public Checkpoint getBasicCheckpoint() {
        return this.basicCheckpoint;
    }

    /**
     * 为通道设置基本检查点。
     *
     * @param checkpoint 要设置为基本检查点的检查点。
     */
    @Override
    public void setBasicCheckpoint(Checkpoint checkpoint) {
        Checkpoint oldBasicCheckpoint = this.basicCheckpoint;
        if (oldBasicCheckpoint == checkpoint) return;
        if (oldBasicCheckpoint != null) {
            if (stared && (oldBasicCheckpoint instanceof Lifecycle)) {
                try {
                    ((Lifecycle) oldBasicCheckpoint).stop();
                } catch (LifecycleException e) {
                    logger.error("stop basic checkpoint failed", e);
                }
            }
            if (oldBasicCheckpoint instanceof VasManager) {
                ((VasManager) oldBasicCheckpoint).setVas(null);
            }
        }
        // 当新入检查点不为null时，设置其所属容器
        if (checkpoint != null) {
            if (checkpoint instanceof VasManager) ((VasManager) checkpoint).setVas(vas);
            if (checkpoint instanceof Lifecycle) {
                try {
                    ((Lifecycle) checkpoint).start();
                } catch (LifecycleException e) {
                    logger.error("start basic checkpoint failed", e);
                    return;
                }
            }
            this.basicCheckpoint = checkpoint;
        }
    }

    /**
     * 向通道添加一个检查点。
     *
     * @param checkpoint 要添加的检查点
     */
    @Override
    public void addCheckpoint(Checkpoint checkpoint) {
        if (checkpoint == null) return;
        if (checkpoint instanceof VasManager) ((VasManager) checkpoint).setVas(vas);
        if (stared && (checkpoint instanceof Lifecycle)) {
            try {
                ((Lifecycle) checkpoint).start();
            } catch (LifecycleException e) {
                logger.error("start checkpoint failed", e);
            }
        }
        synchronized (checkpoints) {
            for (Checkpoint cp : checkpoints) {
                if (cp == checkpoint) return;
            }
            Checkpoint[] tmp = new Checkpoint[checkpoints.length + 1];
            System.arraycopy(checkpoints, 0, tmp, 0, checkpoints.length);
            tmp[checkpoints.length] = checkpoint;
            checkpoints = tmp;
        }
    }

    /**
     * 获取与当前通道相关联的所有检查点。
     *
     * @return 一个 {@code Checkpoint} 对象数组，表示所有检查点。
     * 如果没有可用的检查点，则返回一个空数组。
     */
    @Override
    public Checkpoint[] getCheckpoints() {
        if (basicCheckpoint == null) return checkpoints;
        synchronized (checkpoints) {
            Checkpoint[] tmp = new Checkpoint[checkpoints.length + 1];
            tmp[0] = basicCheckpoint;
            System.arraycopy(checkpoints, 0, tmp, 1, checkpoints.length);
            return tmp;
        }
    }

    /**
     * 从通道中移除指定的检查点。
     *
     * @param checkpoint 要移除的检查点。此检查点必须已经与通道相关联。
     */
    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        synchronized (checkpoints) {
            int j = -1;
            for (int i = 0; i < checkpoints.length; i++) {
                if (checkpoints[i] == checkpoint) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                logger.warn("remove checkpoint failed, checkpoint not found");
                return;
            }
            Checkpoint[] tmp = new Checkpoint[checkpoints.length - 1];
            System.arraycopy(checkpoints, 0, tmp, 0, j);
            System.arraycopy(checkpoints, j + 1, tmp, j, checkpoints.length - j - 1);
            checkpoints = tmp;
            if (checkpoint instanceof VasManager) ((VasManager) checkpoint).setVas(null);
            if (stared && (checkpoint instanceof Lifecycle)) {
                try {
                    ((Lifecycle) checkpoint).stop();
                } catch (LifecycleException e) {
                    logger.error("stop checkpoint failed", e);
                }
            }

        }
    }

    /**
     * 通过检查点处理请求和响应
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     */
    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        new InnerCheckPointContext().process(request, response);
    }

    //</editor-fold>
    //<editor-fold desc = "getter && setter">
    //</editor-fold>
    //<editor-fold desc = "内部类">
    public class InnerCheckPointContext implements CheckpointContext {
        protected int checkpointId = 0;

        public void process(HttpRequest request, HttpResponse response) throws Exception {
            int current = checkpointId;
            checkpointId++;
            if (current < checkpoints.length) {
                checkpoints[current].process(request, response, this);
                logger.debug("checkpoint {} processed", checkpoints[current].getInfo());
            } else if (current == checkpoints.length) {
                if (basicCheckpoint != null) {
                    basicCheckpoint.process(request, response, this);
                } else {
                    logger.warn("basic checkpoint not found");
                }
            }

        }
    }
    //</editor-fold>
}
