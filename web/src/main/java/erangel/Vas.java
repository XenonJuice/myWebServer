package erangel;

import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

import javax.naming.directory.DirContext;
import java.beans.PropertyChangeListener;

public interface Vas {
    /**
     * 返回容器名称
     */
    String getName();

    /**
     * 设定容器名称
     */
    String setName();

    /**
     * 返回父容器
     */
    Vas getParent();

    /**
     * 设定父容器
     */
    Vas setParent(Vas parent);

    /**
     * 返回与此容器关联的资源，如果没有则返回父容器的关联资源
     * 如果还没有则返回null
     */
    DirContext getResources();

    /**
     * 为此容器设定关联资源
     */
    void setResources(DirContext resources);

    /**
     * 设定此容器的子容器
     */
    void addChild(Vas child);

    /**
     * 查找所有与此容器关联的子容器
     */
    Vas[] findChildren();

    /**
     * 根据名称查找此容器的特定子容器
     */
    Vas findChild(String name);

    /**
     * 移除此容器的特定子容器
     */
    void removeChild(Vas child);

    void invoke(HttpRequest req, HttpResponse res);

    /**
     * 将传入的HTTP请求映射到相应的容器或资源。
     *
     * @param req          要处理的HTTP请求。
     * @param writeRequest 一个标志，指示是否应写入请求详细信息。
     * @return 映射的容器或资源作为Vas对象，如果未找到映射则返回null。
     */
    Vas map(HttpRequest req, boolean writeRequest);
    // FIXME 增删容器事件监听器，

    /**
     * 为此容器添加映射器
     */
    void addMapper(Mapper mapper);

    /**
     * 向组件添加属性变化监听器。
     * 监听器将在实施对象的属性发生任何变化时被通知。
     *
     * @param listener 要添加的 PropertyChangeListener；它监听
     *                 属性变化事件。
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * 从组件中移除 PropertyChangeListener。此监听器将不再接收组件中绑定属性更改的通知。
     *
     * @param listener 要移除的 PropertyChangeListener
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * 查找并检索与此容器相关的所有映射器。
     *
     * @return 一个 {@code Mapper} 对象的数组，代表与此容器相关的映射器。
     * 如果未找到映射器，则返回一个空数组。
     */
    Mapper[] findMappers();

    /**
     * 查找并返回与给定名称相关的映射器。
     *
     * @param name 要搜索的映射器名称
     * @return 与指定名称相关的映射器，如果未找到则返回 null
     */
    Mapper findMapper(String name);

}
