package livonia.lifecycle;


import java.util.EventObject;

/**
 * 在实现 Lifecycle 接口的组件上发生的重大事件。本事件可用于通知监听器相关的状态变化，
 *
 * <p>示例用法：
 * <pre>
 *     // 假设有一个实现了Lifecycle接口的组件lifecycleComponent
 *     LifecycleEvent event = new LifecycleEvent(lifecycleComponent, "START");
 *     // 在监听器中捕获该事件后，可以进行相应处理
 * </pre>
 *
 * @author LILINJIAN
 * @version 2025/02/04
 */
public final class LifecycleEvent extends EventObject {

    /**
     * 触发本事件的 Lifecycle 对象。
     */
    private final Lifecycle lifecycle;

    /**
     * 事件类型（必须指定如“START”或“STOP”等）。
     */
    private final String type;

    /**
     * 与事件相关的额外数据（如果有的话）。
     */
    private final Object data;

    /**
     * 创建一个新的 LifecycleEvent 实例。
     *
     * @param lifecycle 触发此事件的 Lifecycle 实例
     * @param type      事件类型（必需）
     */
    public LifecycleEvent(Lifecycle lifecycle, String type) {
        this(lifecycle, type, null);
    }

    /**
     * 创建一个新的 LifecycleEvent 实例，允许带有额外数据。
     *
     * @param lifecycle 触发此事件的 Lifecycle 实例
     * @param type      事件类型（必需）
     * @param data      与该事件相关的额外数据（可以为 null）
     */
    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        super(lifecycle);
        this.lifecycle = lifecycle;
        this.type = type;
        this.data = data;
    }

    /**
     * 获取事件携带的额外数据。
     *
     * @return 事件相关数据，如果没有则返回 null
     */
    public Object getData() {
        return this.data;
    }

    /**
     * 获取触发事件的 Lifecycle 实例。
     *
     * @return 此事件关联的 Lifecycle 对象
     */
    public Lifecycle getLifecycle() {
        return this.lifecycle;
    }

    /**
     * 获取事件类型。
     *
     * @return 事件类型字符串
     */
    public String getType() {
        return this.type;
    }

}