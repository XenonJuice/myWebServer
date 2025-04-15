package erangel.core;

import erangel.base.*;
import erangel.utils.ServerInfo;

public class DefaultEngine extends VasBase implements Engine {
    //<editor-fold desc = "attr">
    private static final String info = "llj.erangel.core.DefaultEngine";
    private final String mapperClass = "erangel.mapper.EngineMapper";
    private String defaultHostName = "";
    // 包含次Engine的Service
    private Service service = null;
    //</editor-fold>
    //<editor-fold desc = "接口实现">
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
    //<editor-fold desc = "构造器">
    public DefaultEngine() {
        // TODO add a basicCheckPoint!
        // channel.setBasicCheckpoint(XXX);
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
    public void start() throws LifecycleException {
        System.out.println(ServerInfo.getServerInfo());
        super.start();
    }
    //</editor-fold>
}
