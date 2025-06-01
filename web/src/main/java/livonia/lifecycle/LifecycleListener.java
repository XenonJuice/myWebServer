package livonia.lifecycle;

/**
 * 定义了一个监听器接口，
 * 用于对实现 Lifecycle 接口的组件在运行过程中发生的重要事件
 * （例如「组件启动」和「组件停止」）进行处理。
 *
 * <p>示例用法：
 * <pre>
 * public class MyLifecycleListener implements LifecycleListener {
 *     &#064;Override
 *     public void lifecycleEvent(LifecycleEvent event) {
 *         // 根据具体事件类型执行相应逻辑
 *         System.out.println("事件类型: " + event.getType());
 *     }
 * }
 *
 * // 在组件中添加该监听器以监听生命周期事件
 * Lifecycle httpConnector = new HttpConnector();
 * httpConnector.addLifecycleListener(new MyLifecycleListener());
 * </pre>
 *
 * <p>实现者可以根据事件类型（如“START”或“STOP”）执行相应操作。
 *
 * @author LILINJIAN
 * @version 2025/02/04
 */
public interface LifecycleListener {

    /**
     * 当某个 LifecycleEvent 事件发生时，会调用此方法进行处理。
     *
     * @param event 已发生的 LifecycleEvent 事件
     */
    void lifecycleEvent(LifecycleEvent event);

}