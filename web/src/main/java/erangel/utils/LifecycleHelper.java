package erangel.utils;

import erangel.base.Lifecycle;
import erangel.base.LifecycleEvent;
import erangel.base.LifecycleListener;

/**
 * 用于帮助触发并分发 LifecycleEvent 事件给已注册的 LifecycleListener。
 * 它作为一个辅助类，用于管理监听器的注册、移除以及统一地向它们发送事件通知。
 *
 * <p>示例用法：
 * <pre>
 * public class httpConnector implements Lifecycle {
 *     private final LifecycleHelper lifecycleHelper = new LifecycleHelper(this);
 *
 *     // 添加监听器
 *     public void addLifecycleListener(LifecycleListener listener) {
 *         lifecycleHelper.addLifecycleListener(listener);
 *     }
 *
 *     // 触发事件
 *     public void start() {
 *         // ... 业务逻辑
 *         lifecycleHelper.fireLifecycleEvent("START", null);
 *     }
 *
 *     // 移除监听器
 *     public void removeLifecycleListener(LifecycleListener listener) {
 *         lifecycleHelper.removeLifecycleListener(listener);
 *     }
 * }
 * </pre>
 *
 * @author LILINJIAN
 * @version 2025/02/04
 */
public final class LifecycleHelper {

    // 对象锁
    private final Object lock = new Object();

    /**
     * 与当前 LifecycleHelper 关联的 Lifecycle 实例，事件将从此处触发。
     */
    private final Lifecycle lifecycle;

    /**
     * 已注册的 LifecycleListener 列表。
     */
    private  LifecycleListener[] listeners = new LifecycleListener[0];

    /**
     * 使用指定的 Lifecycle 实例构造一个 LifecycleHelper，后者作为事件源。
     *
     * @param lifecycle 事件源 Lifecycle 实例
     */
    public LifecycleHelper(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * 为当前 LifecycleHelper 对象添加一个 LifecycleListener。
     *
     * @param listener 要添加的 LifecycleListener
     */
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (lock) {
            LifecycleListener[] newListeners = new LifecycleListener[listeners.length + 1];
            System.arraycopy(listeners, 0, newListeners, 0, listeners.length);
            newListeners[listeners.length] = listener;
            listeners = newListeners;
        }
    }

    /**
     * 返回与当前 Lifecycle 关联的所有 LifecycleListener。
     *
     * @return 所有已注册的 LifecycleListener，如果没有则返回空数组
     */
    public LifecycleListener[] findLifecycleListeners() {
        return listeners.clone();
    }

    /**
     * 根据给定的事件类型和事件数据，同步调用已注册的每个 LifecycleListener。
     *
     * @param type 事件类型，如 "START"、"STOP" 等
     * @param data 可选的附加数据
     */
    public void fireLifecycleEvent(String type, Object data) {
        LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
        LifecycleListener[] currentListeners;
        synchronized (lock) {
            currentListeners = listeners.clone();
        }
        for (LifecycleListener listener : currentListeners) {
            listener.lifecycleEvent(event);
        }
    }

    /**
     * 从当前 LifecycleHelper 中移除指定的 LifecycleListener。
     *
     * @param listener 要移除的 LifecycleListener
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (lock) {
            if (listener == null || listeners.length == 0) {
                return;
            }
            // 查找要删除的监听器
            int index = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    index = i;
                    break;
                }
            }

            // 如果没找到，直接返回
            if (index < 0) {
                return;
            }

            // 创建新数组并复制
            LifecycleListener[] newListeners = new LifecycleListener[listeners.length - 1];
            // 复制index之前的元素
            if (index > 0) {
                System.arraycopy(listeners, 0, newListeners, 0, index);
            }
            // 复制index之后的元素
            if (index < listeners.length - 1) {
                System.arraycopy(listeners, index + 1, newListeners, index,
                        listeners.length - index - 1);
            }
            // 更新数组引用
            listeners = newListeners;
        }
    }

}
