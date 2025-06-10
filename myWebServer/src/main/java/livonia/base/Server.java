package livonia.base;

import livonia.lifecycle.LifecycleException;

public interface Server {

    /**
     * 将<code>Service</code>添加到<code>Server</code>实例中
     *
     * @param service 要被添加的<code>Service</code>实例
     */
    void addService(Service service);

    /**
     * 查找并返回指定名称的<code>Service</code>。
     *
     * @param name 要搜索的<code>Service</code>名称
     * @return 匹配指定名称的 {@code Service} 实例，
     * 如果未找到<code>Service</code>，则返回 {@code null}
     */
    Service findService(String name);

    /**
     * 查找并返回该<code>Server</code>持有的所有<code>Service</code>。
     *
     * @return 一个 {@code Service} 对象数组，代表与服务器相关的服务。
     * 如果未找到任何<code>Service</code>，则返回一个空数组。
     */
    Service[] findServices();

    /**
     * 从容器中按名称移除<code>Service</code>。
     *
     * @param name 要移除的<code>Service</code>名称；必须是非空且非空字符串。
     */
    void removeService(String name);

    /**
     * 初始化
     */
    void initialize() throws LifecycleException;

    /**
     * 取得用于关闭<code>Server</code>的端口号。
     *
     * @return 接受shutdown指令的端口号。
     */
    int getShutdownPort();

    /**
     * 设置用于关闭<code>Server</code>的端口号。
     *
     * @param port 接受shutdown指令的端口号。
     */
    void setShutdownPort(int port);

    /**
     * 获取<code>Server</code>的关闭命令
     *
     * @return shutdown指令
     */
    String getShutdownCommand();

    /**
     * 设置<code>Server</code>的关闭命令。
     */
    void setShutdownCommand(String shutdownCommand);

    /**
     * 等待shutdown指令
     */
    void waitForShutdown();

    /**
     * 取得描述信息。
     *
     * @return 描述信息。
     */
    String getInfo();
}
