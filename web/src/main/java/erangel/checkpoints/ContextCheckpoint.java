package erangel.checkpoints;

import erangel.base.CheckpointContext;
import erangel.base.Context;
import erangel.base.Endpoint;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.log.BaseLogger;
import erangel.utils.Decoder;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static erangel.base.Const.webApp.META_INF;
import static erangel.base.Const.webApp.WEB_INF;
import static javax.servlet.http.HttpServletResponse.*;

public class ContextCheckpoint extends CheckpointBase {
    //<editor-fold desc = "attr">
    private static final String info = "erangel.checkpoints.ContextCheckpoint";
    private static final Logger logger = BaseLogger.getLogger(ContextCheckpoint.class);
    private static final List<String> deniedPrefix = List.of(
            "/.", WEB_INF, META_INF
    );
    //</editor-fold>
    //<editor-fold desc = "抽象方法实现">
    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception {
        logger.info("ContextCheckpoint : process in {}",getVas().getName());
        // 拒绝对受限资源的直接访问
        String requestURI = Decoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
        String contextPath = request.getContextPath();
        String relativePath = requestURI.substring(contextPath.length()).toUpperCase();
        for (String prefix : deniedPrefix) {
            if (relativePath.equalsIgnoreCase(prefix) ||
                    relativePath.startsWith(prefix)) {
                rejectRequest(request, response, SC_FORBIDDEN);
                return;
            }
        }
        Context innerContext = (Context) getVas();
        // 找到处理该请求的servlet
        Endpoint endpoint;
        try {
            endpoint = (Endpoint) innerContext.map(request, true);
        } catch (Exception e) {
            // 若此处发现请求不合法，拒绝
            rejectRequest(request, response, SC_BAD_REQUEST);
            return;
        }
        // 若找不到对应的servlet，拒绝
        if (endpoint == null) {
            rejectRequest(request, response, SC_NOT_FOUND);
        } else {
            // 正常，则通过servlet处理请求
            response.setContext(innerContext);
            endpoint.process(request, response);
        }
    }

    //</editor-fold>

}
