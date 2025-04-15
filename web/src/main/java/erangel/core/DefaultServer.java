package erangel.core;


import erangel.base.Server;
import erangel.base.Service;
import erangel.lifecycle.Lifecycle;
import erangel.lifecycle.LifecycleException;
import erangel.lifecycle.LifecycleListener;
import erangel.log.BaseLogger;
import erangel.utils.LifecycleHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DefaultServer implements Server, Lifecycle {
    //<editor-fold desc = "attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(DefaultServer.class);
    // info
    private static final String info = "erangel.core.DefaultServer";
    // 本地回环地址
    private final static String LOOPBACK_ADDRESS = "127.0.0.1";
    // 生命周期助手
    private LifecycleHelper helper = new LifecycleHelper(this);
    // 关闭端口
    private int shutdownPort = 114514;
    // 此Server持有的Service
    private Service[] services = new Service[0];
    // 关闭指令
    private String shutdownCommand = "SHUTDOWN";
    // 是否完成了初始化
    private boolean initialized = false;
    // 是否开始了
    private boolean started = false;

    //</editor-fold>
    //<editor-fold desc = "生命周期相关">
    @Override
    public void initialize() throws LifecycleException {
        if (initialized) throw new LifecycleException("DefaultServer : already initialized");
        initialized =true;
        // 初始化Service
        synchronized (services) {
            for (Service service : services) {
                try {
                    logger.info("DefaultServer initialize : initialize service");
                    service.initialize();
                } catch (LifecycleException e) {
                    logger.error("initialize service failed", e);
                    e.printStackTrace(System.err);
                }
            }
        }

    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        helper.removeLifecycleListener(listener);
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        helper.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListener() {
        return helper.findLifecycleListeners();
    }

    @Override
    public void start() throws LifecycleException {
        if (started) throw new LifecycleException("DefaultServer : already started");
        helper.fireLifecycleEvent(BEFORE_START_EVENT, null);
        helper.fireLifecycleEvent(START_EVENT, null);
        started = true;
        // 启动Service
        synchronized (services) {
            for (Service service : services) {
                try {
                    logger.info("DefaultServer start : start service");
                    ((Lifecycle) service).start();
                } catch (LifecycleException e) {
                    logger.error("start service failed", e);
                }
            }
        }
        helper.fireLifecycleEvent(AFTER_START_EVENT, null);

    }

    @Override
    public void stop() throws LifecycleException {
        if (!started) throw new LifecycleException("DefaultServer : not started");
        helper.fireLifecycleEvent(BEFORE_START_EVENT, null);
        helper.fireLifecycleEvent(STOP_EVENT,null);
        started =false;
        // 停止Service
        synchronized (services) {
            for (Service service : services) {
                try {
                    logger.info("DefaultServer stop : stop service");
                    ((Lifecycle) service).stop();
                } catch (LifecycleException e) {
                    logger.error("stop service failed", e);
                }
            }
        }
        helper.fireLifecycleEvent(AFTER_STOP_EVENT, null);
    }

    //</editor-fold>
    //<editor-fold desc = "接口实现">
    @Override
    public void addService(Service service) {
        service.setServer(this);
        synchronized (services) {
            Service[] tmp = new Service[services.length + 1];
            System.arraycopy(services, 0, tmp, 0, services.length);
            tmp[services.length] = service;
            services = tmp;

            // 启动Service
            if (initialized) {
                try {
                    logger.info("DefaultServer addService : initialize service");
                    service.initialize();
                } catch (LifecycleException e) {
                    logger.error("initialize service failed", e);
                    e.printStackTrace(System.err);
                }
            }

            // 开始运行Service
            if (started && service instanceof Lifecycle) {
                try {
                    logger.info("DefaultServer addService : start service");
                    ((Lifecycle) service).start();
                } catch (LifecycleException e) {
                    logger.error("start service failed", e);
                }
            }
        }
    }

    @Override
    public Service findService(String name) {
        if (name == null) return null;
        synchronized (services) {
            for (Service service : services) {
                if (name.equals(service.getName())) return service;
            }
            return null;
        }
    }

    @Override
    public Service[] findServices() {
        return services;
    }

    @Override
    public void removeService(String name) {
        synchronized (services) {
            int j = -9;
            for (int i = 0; i < services.length; i++) {
                if (services[i].getName().equals(name)) {
                    j = i;
                    break;
                }
            }
            if (j < 0) return;
            if (services[j] instanceof Lifecycle) {
                try {
                    logger.info("DefaultServer removeService : stop service");
                    ((Lifecycle) services[j]).stop();
                } catch (LifecycleException e) {
                    logger.error("stop service failed", e);
                }
            }

            services[j].setServer(null);
            Service[] tmp = new Service[services.length - 1];
            System.arraycopy(services, 0, tmp, 0, j);
            System.arraycopy(services, j + 1, tmp, j, services.length - j - 1);
            services = tmp;
        }

    }


    @Override
    public int getShutdownPort() {
        return this.shutdownPort;
    }

    @Override
    public void setShutdownPort(int port) {
        if (port < 0) throw new IllegalArgumentException("port must be positive!");
        this.shutdownPort = port;
    }

    @Override
    public String getShutdownCommand() {
        return shutdownCommand;
    }

    @Override
    public void setShutdownCommand(String shutdownCommand) {
        this.shutdownCommand = shutdownCommand;
    }

    @Override
    public void waitForShutdown() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(shutdownPort, 1, InetAddress.getByName(LOOPBACK_ADDRESS));
        } catch (IOException e) {
            System.err.println("DefaultServer waitForShutdown : " +
                    "can not create shutdown server socket at port [" + shutdownPort
                    + "] with address [" + LOOPBACK_ADDRESS + "]");
            e.printStackTrace();
            System.exit(1);
        }

        // 从socket中读取关闭指令
        while (true) {
            Socket socket = null;
            InputStream inputStream = null;

            try {
                socket = serverSocket.accept();
                // 十秒内无数据的话就跳过
                socket.setSoTimeout(10000);
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                System.err.println(
                        "DefaultServer waitForShutdown went wrong when accept socket : "
                                + e.getMessage());
                logger.error("waitForShutdown went wrong when accept socket ", e);
                System.exit(1);
            }


            StringBuilder shutdownCommand0 = new StringBuilder();
            // 稍微留出一些空间
            int maxLen = Math.max(512, shutdownCommand.length() + 32);
            int data;
            try {
                while ((maxLen--) > 0 && (data = inputStream.read()) >= 32) {
                    shutdownCommand0.append((char) data);
                }
            } catch (IOException e) {
                System.err.println(
                        "DefaultServer waitForShutdown went wrong when read data from socket : "
                                + e.getMessage());
                logger.error("waitForShutdown went wrong when read data from socket ", e);
                System.exit(1);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("waitForShutdown went wrong when close inputStream ", e);
                }
            }

            // 接受完关闭指令后 关闭socket 这里是关闭某客户端的链接，其实就是自己
            try{
                socket.close();
            } catch (IOException e) {
                logger.error("waitForShutdown went wrong when close socket ", e);
            }

            // 处理一下收到的字符串
            String shutdownCommand1 = shutdownCommand0.toString().trim();
            if (shutdownCommand1.equals(shutdownCommand)) {
                logger.info("DefaultServer waitForShutdown : shutdown command received");
                break;
            } else {
                System.err.println("DefaultServer waitForShutdown : shutdown command ["+shutdownCommand1+"] is not correct, please try again");
            }
        }

        // 关闭服务器套接字 这里是关闭整个服务器监听端口
        try{
            serverSocket.close();
        } catch (IOException e){
            logger.error("waitForShutdown went wrong when close server socket ", e);
        }
    }

    @Override
    public String getInfo() {
        return info;
    }
    //</editor-fold>

}
