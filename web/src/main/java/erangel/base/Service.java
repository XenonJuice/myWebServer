package erangel.base;

import erangel.lifecycle.LifecycleException;

/**
 * 包含连接器的容器
 */
public interface Service {

    /**
     * 返回一个负责处理与该<code>Service</code>相关联的所有<code>Connector</code>所接收到请求的容器（也就是<code>Engine</code>）
     *
     * @return 一个<code>Engine</code>实例
     * @see Engine
     */
    Vas getVas();

    /**
     * 设置一个负责处理与该<code>Service</code>相关联的所有<code>Connector</code>所接收到请求的容器（也就是<code>Engine</code>）
     *
     * @param vas 一个容器，也就是<code>Engine</code>
     * @see Engine
     */
    void setVas(Vas vas);

    /**
     * 获取描述信息
     *
     * @return String 表示描述信息
     */
    String getInfo();

    /**
     * 获取<code>Service</code>的名称
     *
     * @return String 表示<code>Service</code>名
     */
    String getName();

    /**
     * 设置<code>Service</code>的名称
     *
     * @param name <code>Service</code>的名称
     */
    void setName(String name);

    /**
     * 获取关联的<code>Server</code>
     *
     * @return <code>Server</code> 整个服务器实例对象
     * @see Server
     */
    Server getServer();


    /**
     * 设置关联的<code>Server</code>
     *
     * @param server 一个服务器实例对象
     */
    void setServer(Server server);

    /**
     * 添加一个新的连接器到连接器的集合中，并将其与该<code>Service</code>器关联。
     *
     * @param connector 要添加的连接器
     */
    void addConnector(Connector connector);

    /**
     * 找到并返回与此<code>Service</code>相关的连接器集合。
     */
    Connector[] findConnectors();


    /**
     * 从该<code>Service</code>中移除指定的连接器
     */
    void removeConnector(Connector connector);


    /**
     * 初始化连接器
     *
     * @throws LifecycleException 如果在初始化过程中出现问题
     */
    void initialize() throws LifecycleException;
}
