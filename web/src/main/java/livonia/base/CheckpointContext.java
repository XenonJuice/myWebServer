package livonia.base;

import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;

/**
 * 一个检查点上下文接口，用于管理HTTP请求和响应中的检查点处理。
 * 由于在该上下文中保存了通道中当前加载到的检查点的位置信息，所以可作为
 * 参数传递到下一个检查点中
 */
public interface CheckpointContext {
    void process(HttpRequest request, HttpResponse response) throws Exception;
}
