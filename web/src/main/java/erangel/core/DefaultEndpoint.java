package erangel.core;

import erangel.base.Endpoint;
import erangel.log.BaseLogger;
import org.slf4j.Logger;

import javax.servlet.*;
import java.util.Enumeration;
import java.util.HashMap;

public final class DefaultEndpoint extends VasBase implements Endpoint, ServletConfig {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(DefaultEndpoint.class);
    // 初始化后的servlet实例
    private Servlet sInstance = null;
    // 初始化param
    private HashMap<String,String> initParams = new HashMap<>();
    // 正在卸载servlet标志位
    private boolean isUnloading = false;
    // 可用性：OL可用 INT MAXVALUE不可用
    private long available = 0L;
    //</editor-fold>
    //<editor-fold desc = "getter & setter">
    public long getAvailable() {
        return available;
    }
    public void setAvailable(long available) {
        long bignumber = System.currentTimeMillis();
        if(available > bignumber) this.available = available;
        else this.available = 0L;
        this.available = available;
    }
    //</editor-fold>
    //<editor-fold desc = "构造器">
    public DefaultEndpoint(){
        // TODO:设置一个默认检查点
        channel.setBasicCheckpoint(null);
    }
    //</editor-fold>
    //<editor-fold desc = "实现或重写父类，接口">
    @Override
    public void load() throws ServletException {

    }

    @Override
    public void unload() throws ServletException {

    }

    @Override
    public void salloc() throws ServletException {

    }

    @Override
    public void sfree() throws ServletException {

    }

    @Override
    public String findInitParam(String name) {
        return "";
    }

    @Override
    public String[] findInitParams() {
        return new String[0];
    }

    @Override
    public void removeInitParam(String name) {

    }

    @Override
    public boolean isUnavaliable() {
        if(available == 0L) return false;
        if(available < System.currentTimeMillis()) {
            available = 0L;
            return false;
        }
        return true;
    }

    @Override
    public void unavaliable(UnavailableException unavailable) {
        logger.warn("mark endpoint : {} as unavailable cause : {}",getName(),unavailable.getMessage());
        if(unavailable.isPermanent()) setAvailable(Long.MAX_VALUE);
        else {
            int timeout = unavailable.getUnavailableSeconds();
            if (timeout<0) timeout = 1;
            setAvailable(System.currentTimeMillis()+timeout*1000L);
        }
    }

    @Override
    public String getServletName() {
        return "";
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        return "";
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }
    //</editor-fold>


    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
