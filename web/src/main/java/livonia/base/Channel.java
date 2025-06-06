package livonia.base;

import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;

/**
 * 表示一个在系统中管理检查点的通道。
 * 一个通道可以设置、检索、添加或移除检查点。
 */
public interface Channel {
    /**
     * 获取与通道相关联的基本检查点。
     *
     * @return 与通道关联的基本 {@code Checkpoint} 实例，如果没有设置则返回 {@code null}。
     */
    Checkpoint getBasicCheckpoint();

    /**
     * 为通道设置基本检查点。
     *
     * @param checkpoint 要设置为基本检查点的检查点。
     */
    void setBasicCheckpoint(Checkpoint checkpoint);

    /**
     * 向通道添加一个检查点。
     *
     * @param checkpoint 要添加的检查点
     */
    void addCheckpoint(Checkpoint checkpoint);

    /**
     * 获取与当前通道相关联的所有检查点。
     *
     * @return 一个 {@code Checkpoint} 对象数组，表示所有检查点。
     * 如果没有可用的检查点，则返回一个空数组。
     */
    Checkpoint[] getCheckpoints();

    /**
     * 从通道中移除指定的检查点。
     *
     * @param checkpoint 要移除的检查点。此检查点必须已经与通道相关联。
     */
    void removeCheckpoint(Checkpoint checkpoint);

    /**
     * 通过检查点处理请求和响应
     */
    void process(HttpRequest request, HttpResponse response) throws Exception;
}
