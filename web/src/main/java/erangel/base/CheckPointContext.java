package erangel.base;

import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;

/**
 * 一个检查点上下文接口，用于管理HTTP请求和响应中的检查点处理。
 * 由于在该上下文中保存了通道中当前加载到的检查点的位置信息，所以可作为
 * 参数传递到下一个检查点中
 */
public interface CheckPointContext {
    void process(HttpRequest request, HttpResponse response) throws Exception;
}
