package livonia.base;

import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;

public interface Vas {
    /**
     * 返回容器名称
     */
    String getName();

    /**
     * 设定容器名称
     */
    void setName(String name);

    /**
     * 返回父容器
     */
    Vas getParent();

    /**
     * 设定父容器
     */
    void setParent(Vas parent);

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

    void process(HttpRequest req, HttpResponse res) throws Exception;

    /**
     * 将传入的HTTP请求映射到相应的容器或资源。
     *
     * @param req          要处理的HTTP请求。
     * @param writeRequest 一个标志，指示是否应写入请求详细信息。
     * @return 映射的容器或资源作为Vas对象，如果未找到映射则返回null。
     */
    Vas map(HttpRequest req, boolean writeRequest);

    /**
     * 为此容器添加映射器
     */
    void addMapper(Mapper mapper);

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

    /**
     * 获取与此容器关联的父类加载器。
     *
     * @return 父 {@code ClassLoader} 实例，如果未定义父类加载器，则返回 {@code null}。
     */
    ClassLoader getParentClassLoader();

    /**
     * 设置此组件的父类加载器。
     *
     * @param classLoader 要设置为父类加载器的 ClassLoader
     */
    void setParentClassLoader(ClassLoader classLoader);


    Loader getLoader();

    void setLoader(Loader loader);


}
