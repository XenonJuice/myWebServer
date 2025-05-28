package erangel.checkpoints;

import erangel.base.Checkpoint;
import erangel.base.CheckpointContext;
import erangel.base.Vas;
import erangel.base.VasManager;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.log.BaseLogger;
import org.slf4j.Logger;

import static javax.servlet.http.HttpServletResponse.*;

public abstract class CheckpointBase implements Checkpoint, VasManager {
    //<editor-fold desc = "attr">
    private static final Logger logger = BaseLogger.getLogger(CheckpointBase.class);
    protected Vas vas = null;

    //</editor-fold>
    //<editor-fold desc = "一次实现">
    @Override
    public Vas getVas() {
        return vas;
    }

    @Override
    public void setVas(Vas vas) {
        this.vas = vas;
    }

    public abstract void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception;
    //</editor-fold>

    //<editor-fold desc = "拒绝请求">
    protected void rejectRequest(HttpRequest request, HttpResponse response, int type) throws Exception {
        try {
            switch (type) {
                case 404:
                    response.sendError(SC_NOT_FOUND, request.getRequestURI());
                    break;
                case 405:
                    response.sendError(SC_METHOD_NOT_ALLOWED, request.getMethod());
                    break;
                case 403:
                    response.sendError(SC_FORBIDDEN, request.getRequestURI());
                    break;
                case 400:
                    response.sendError(SC_BAD_REQUEST, request.getRequestURI());
                    break;
                case 500:
                    response.sendError(SC_INTERNAL_SERVER_ERROR, request.getRequestURI());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("rejectRequest error", e);
        }
    }
    //</editor-fold>
}
