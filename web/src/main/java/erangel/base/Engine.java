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
}
