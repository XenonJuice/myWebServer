package livonia.checkpoints;

import livonia.base.CheckpointContext;
import livonia.base.Context;
import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpResponse;
import livonia.core.DefaultHost;
import livonia.log.BaseLogger;
import livonia.utils.Decoder;
import org.slf4j.Logger;

public class HostCheckpoint extends CheckpointBase {
    //<editor-fold desc = "attr">
    private static final String info = "livonia.base.lifecycle.HostCheckpoint";
    private static final Logger logger = BaseLogger.getLogger(HostCheckpoint.class);

    //</editor-fold>
    //<editor-fold desc = "抽象方法实现">
    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception {
        logger.info("HostCheckpoint : process in {}", getVas().getName());
        DefaultHost host = (DefaultHost) getVas();
        Context innerContext = (Context) host.map(request, true);
        if (innerContext == null) {
            rejectRequest(request, response, 404);
            logger.error("HostCheckpoint : process : context : {} not found",
                    Decoder.decode(request.getRequestURI()));
            return;
        }
        // 切换到map到的context的上下文线程的类加载器
        Thread.currentThread().setContextClassLoader(innerContext.getLoader().getClassLoader());
        innerContext.process(request, response);
    }
    //</editor-fold>

}
