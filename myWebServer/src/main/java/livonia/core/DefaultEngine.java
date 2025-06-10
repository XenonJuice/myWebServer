package livonia.core;

import livonia.base.*;
import livonia.checkpoints.EngineCheckpoint;
import livonia.lifecycle.LifecycleException;
import livonia.utils.ServerInfo;

public class DefaultEngine extends VasBase implements Engine {
    //<editor-fold desc = "attr">
    private static final String info = "llj.livonia.core.DefaultEngine";
    private String defaultEngineMapper = "livonia.mapper.EngineMapper";
    private String defaultHostName = "";
    // 包含次Engine的Service
    private Service service = null;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public DefaultEngine() {
        channel.setBasicCheckpoint(new EngineCheckpoint());
    }

    public String getMapper() {
        return defaultEngineMapper;
    }
    //</editor-fold>
    //<editor-fold desc = "接口实现">

    //</editor-fold>
    //<editor-fold desc = "映射器">
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
        this.defaultEngineMapper = mapper.getClass().getName();
    }

    @Override
    public String getDefaultHostName() {
        return defaultHostName;
    }

    @Override
    public void setDefaultHostName(String name) {
        this.defaultHostName = name;
    }

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public void setService(Service service) {
        this.service = service;
    }

    @Override
    public String getInfo() {
        return info;
    }

    //</editor-fold>
    //<editor-fold desc = "重写抽象类的部分方法">
    /*
     * 由于Engine的特殊性，其子容器必须是host，且不能再有上层的父容器了
     */
    @Override
    public void addChild(Vas child) {
        if (!(child instanceof Host)) throw new IllegalArgumentException("child must be a Host!");
        super.addChild(child);
    }

    @Override
    public void setParent(Vas parent) {
        throw new UnsupportedOperationException("Engine can not have parent!");
    }


    //</editor-fold>
    //<editor-fold desc = "生命周期相关">
    @Override
    public synchronized void start() throws LifecycleException {
        System.out.println(ServerInfo.getServerInfo());
        Mapper mapper = setMapper(defaultEngineMapper);
        mapper.setVas(this);
        super.start();
    }
    //</editor-fold>
}
