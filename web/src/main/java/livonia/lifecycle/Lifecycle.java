package livonia.lifecycle;

/**
 * Lifecycle 接口表示支持在其生命周期中具有明确定义的一系列事件的组件。
 * 提供了用于管理生命周期监听器，以及启动和停止组件的方法。
 * <p>
 * 实现此接口的组件会触发一系列生命周期事件，例如
 * “before start”、“start”、“after start”、“before stop”、“stop” 和 “after stop”，
 * 用于通知已注册的监听器关于组件状态变化的信息。
 * <p>
 * LifecycleEvent 实例用于封装每个事件的详细信息，包括事件类型和相关数据。
 *
 * @author LILINJIAN
 * @version 2025/02/04
 */
public interface Lifecycle {

    /**
     * 组件的生命周期事件集。
     * 分为启动之前，启动，启动之后，停止之前，停止，停止之之后
     */
    String BEFORE_START_EVENT = "before_start";
    String START_EVENT = "start";
    String AFTER_START_EVENT = "after_start";
    String BEFORE_STOP_EVENT = "before_stop";
    String STOP_EVENT = "stop";
    String AFTER_STOP_EVENT = "after_stop";


    /**
     * 将一个生命周期监听器从组件中移除。
     *
     * @param listener 要移除的生命周期监听器
     */
    void removeLifecycleListener(LifecycleListener listener);

    /**
     * 将一个生命周期监听器添加到这个生命周期中。这个监听器将被通知
     * 生命周期事件，比如启动和停止。
     *
     * @param listener 要添加的生命周期监听器
     */
    void addLifecycleListener(LifecycleListener listener);

    /**
     * 找到所有与此生命周期关联的监听器，如果没有相关监听器
     * 则返回一个长度为0的数组
     */
    LifecycleListener[] findLifecycleListener();

    /**
     * 在使用该组件的任何公共方法之前，先调用此方法。
     * 向所有已注册的监听器发送一个类型为
     * START_EVENT 的 LifecycleEvent。
     *
     * @throws LifecycleException 当组件发生严重错误导致不可恢复使用时抛出该错误
     */
    void start() throws LifecycleException;

    /**
     * 终止对组件公开方法的使用。
     * 向所有已注册的监听器发送一个类型为
     * STOP_EVENT 的 LifecycleEvent。
     *
     * @throws LifecycleException 关闭失败时抛出该错误
     */
    void stop() throws LifecycleException;

}
