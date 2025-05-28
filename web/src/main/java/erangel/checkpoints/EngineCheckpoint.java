package erangel.checkpoints;

import erangel.base.CheckpointContext;
import erangel.base.Host;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.core.DefaultEngine;

import static erangel.base.Const.HttpProtocol.HTTP_1_1;

public class EngineCheckpoint extends CheckpointBase {
    //<editor-fold desc = "attr">
    private static final String info = "erangel.checkpoints.EngineCheckpoint";

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
