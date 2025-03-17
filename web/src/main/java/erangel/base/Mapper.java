package erangel.base;

import erangel.connector.http.HttpRequest;

public interface Mapper {
    /**
     * 检索与此对象关联的 Vas 实例。
     *
     * @return 与之关联的 Vas 实例，如果没有关联实例则返回 null。
     */
    Vas getVas();

    /**
     * 为此映射器设置指定的 Vas 实例。
     *
     * @param vas 要设置的 Vas 实例
     */
    void setVas(Vas vas);

    /**
     * 将传入的 HTTP 请求映射到相应的容器或资源。
     *
     * @param request      要处理的 HTTP 请求。
     * @param writeRequest 一个标志，指示是否应写入请求详情。
     * @return 对应的映射容器或资源作为 Vas 对象，如果未找到任何映射则返回 null。
     */
    Vas map(HttpRequest request, boolean writeRequest);
}
