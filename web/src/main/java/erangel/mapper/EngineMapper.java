package erangel.mapper;

import erangel.base.Mapper;
import erangel.base.Vas;
import erangel.connector.http.HttpRequest;
import erangel.core.DefaultEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineMapper implements Mapper {
    //<editor-fold desc = "attr">
    // 关联的Engine实例
    private DefaultEngine engine = null;
    // logger
    private static final Logger logger = LoggerFactory.getLogger(EngineMapper.class);

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public Vas getVas() {
        return engine;
    }

    @Override
    public void setVas(Vas vas) {
        this.engine = (DefaultEngine) vas;
    }

    @Override
    public Vas map(HttpRequest request, boolean writeRequest) {
        String virtualHostName = request.getServerName();
        if (virtualHostName == null || virtualHostName.isEmpty()) {
            logger.warn("virtual host name is null or empty");
            virtualHostName = engine.getDefaultHostName();
            if (writeRequest) request.setServerName(virtualHostName);
        }
        if (virtualHostName == null || virtualHostName.isEmpty()) return null;
        virtualHostName = virtualHostName.toLowerCase();
        logger.info("EngineMapper : virtual host name: '{}'", virtualHostName);
        return engine.findChild(virtualHostName);
    }
    //</editor-fold>

}
