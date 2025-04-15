package erangel.mapper;

import erangel.base.Context;
import erangel.base.Mapper;
import erangel.base.Vas;
import erangel.connector.http.HttpRequest;
import erangel.core.DefaultHost;
import erangel.utils.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static erangel.base.Const.commonCharacters.BLANK;

public class HostMapper implements Mapper {
    //<editor-fold desc = "attr">
    // 关联的Host
    private DefaultHost host = null;
    // logger
    private static final Logger logger = LoggerFactory.getLogger(HostMapper.class);

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public Vas getVas() {
        return host;
    }

    @Override
    public void setVas(Vas vas) {
        host = (DefaultHost) vas;
    }

    @Override
    public Vas map(HttpRequest request, boolean writeRequest) {
        String uri = (Decoder.decode(request.getRequestURI(), StandardCharsets.UTF_8));
        logger.info("HostMapper : uri: '{}'", uri);
        Context context = host.map(uri);
        if (writeRequest) {
            if (context != null) {
                request.setContextPath(context.getPath());
            } else {
                request.setContextPath(BLANK);
            }
        }
        return context;
    }
    //</editor-fold>
}
