package erangel.base;

public interface Engine extends Vas {

    /**
     * 返回与此<code>Engine</code>关联的<code>Service</code>
     */
    Service getService();

    /**
     * 设置与此<code>Engine</code>关联的<code>Service</code>
     *
     * @param service 包含此此<code>Engine</code>的<code>Service</code>
     */
    void setService(Service service);

    /**
     * 获取描述信息
     *
     * @return String 表示描述信息
     */
    String getInfo();

    /**
     * 检索与引擎关联的默认主机名。
     *
     * @return 一个 {@code String}，表示默认主机名，
     * 如果未配置默认主机，则返回 {@code null}。
     */
    String getDefaultHostName();

    /**
     * 设置此引擎的默认主机名。默认主机名决定了当没有匹配的特定主机名时，哪个子主机将处理请求。
     *
     * @param name 本引擎使用的默认主机名。它应对应于某个子主机的名称。
     */
    void setDefaultHostName(String name);
}
