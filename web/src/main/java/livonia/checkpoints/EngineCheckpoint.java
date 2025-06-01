package livonia.checkpoints;

import livonia.base.CheckpointContext;
import livonia.base.Host;
import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;
import livonia.core.DefaultEngine;

import static livonia.base.Const.HttpProtocol.HTTP_1_1;

public class EngineCheckpoint extends CheckpointBase {
    //<editor-fold desc = "attr">
    private static final String info = "livonia.checkpoints.EngineCheckpoint";

    //</editor-fold>
    //<editor-fold desc = "抽象方法实现">
    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception {
        DefaultEngine engine = (DefaultEngine) getVas();

        // HTTP1.1时拒绝不包含hostHeader的请求
        if (HTTP_1_1.equals(request.getProtocol()) && request.getServerName() == null) {
            rejectRequest(request, response, 400);
            return;
        }
        // 找不到host时也拒绝请求
        Host host = (Host) engine.map(request, true);
        if (host == null) {
            rejectRequest(request, response, 404);
            return;
        }
        engine.process(request, response);
    }
    //</editor-fold>

}
