package erangel.connector.http;

import erangel.log.BaseLogger;
import erangel.net.DefaultServerSocketFactory;
import erangel.net.ServerSocketFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpConnector extends BaseLogger implements Runnable {
    //<editor-fold desc = "attr">
    // 描述信息
    private static final String info = "llj.erangel.connector.http.HttpConnector/1.0";
    // 线程容器
    private final ConcurrentLinkedQueue<HttpProcessor> processors = new ConcurrentLinkedQueue<>();
    // 已创建的解析器线程之容器
    private final CopyOnWriteArrayList<HttpProcessor> created = new CopyOnWriteArrayList<>();
    // 当前解析器数量
    private final AtomicInteger currentProcessors = new AtomicInteger(0);
    // 对象锁
    private final Object lock = new Object();
    // 经过本连接器处理的所有请求的协议名
    private String scheme = "http";
    // 最大连接数
    private int acceptCount = 10;
    // 端口名
    private int port = 8080;
    // 代理端口、名
    private int proxyPort = 0;
    private String proxyName = null;
    // 希望监听特定的网络接口或IP
    private String address = null;
    // DNS反向查询标志位
    private boolean enableLookups = false;
    // 分块传输标识位
    private boolean allowChunking = true;
    // 最大解析器数量
    private int maxProcessors = 100;
    // 最小解析器数量
    private int minProcessors = 20;
    // 超时时间
    private int connectionTimeOut = Const.ConnectorConstants.DEFAULT_CONNECTION_TIMEOUT;
    // 服务器socket工厂
    private ServerSocketFactory factory = null;
    // 所有从TCP连接中接收到的服务器套接字
    private ServerSocket serverSocket = null;
    // 本线程
    private Thread thread = null;
    // 当前线程名称
    private final String threadName = null;
    // 线程停止标志位
    private boolean stopped = false;

    //</editor-fold>
    //<editor-fold desc="getter & setter">
    public int getConnectionTimeOut() {
        return connectionTimeOut;
    }

    public void setConnectionTimeOut(int connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
    }

    public int getAcceptCount() {
        return acceptCount;
    }

    public void setAcceptCount(int acceptCount) {
        this.acceptCount = acceptCount;
    }

    public int getMaxProcessors() {
        return maxProcessors;
    }

    public void setMaxProcessors(int maxProcessors) {
        this.maxProcessors = maxProcessors;
    }

    public int getMinProcessors() {
        return minProcessors;
    }

    public void setMinProcessors(int minProcessors) {
        this.minProcessors = minProcessors;
    }

    public boolean isAllowChunking() {
        return allowChunking;
    }

    public void setAllowChunking(boolean allowChunking) {
        this.allowChunking = allowChunking;
    }

    /**
     * 获取监听HTTP请求的端口号
     */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 设置监听HTTP请求的端口号
     */
    public boolean isEnableLookups() {
        return enableLookups;
    }

    public void setEnableLookups(boolean enableLookups) {
        this.enableLookups = enableLookups;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }


    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ServerSocketFactory getFactory() {
        if (factory == null) {
            synchronized (this) {
                factory = new DefaultServerSocketFactory();
            }
        }
        return (factory);
    }

    public void setFactory(ServerSocketFactory factory) {
        this.factory = factory;
    }

    //</editor-fold>
    //<editor-fold desc="线程相关">
    @Override
    public void run() {
        while (!stopped) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                if (connectionTimeOut > 0) {
                    socket.setSoTimeout(connectionTimeOut);
                }
                socket.setTcpNoDelay(true);
            } catch (SecurityException se) {
                logger.warn("套接字安全异常：{}", se.getMessage());
                continue;
            } catch (IOException e) {
                try {
                    // 重新打开套接字失败时
                    synchronized (lock) {
                        if (!stopped) {
                            serverSocket.close();
                            serverSocket = openSocket();
                        }
                    }
                } catch (IOException reOpen) {
                    logger.warn("重新打开套接字失败，出现IO错误{}", reOpen.getMessage());
                    break;
                }
                continue;
            }
            HttpProcessor processor = createProcessor();
            if (processor == null) {
                try {
                    logger.warn("无解析器可用，拒接本次连接");
                    socket.close();
                } catch (IOException _) {

                }
                continue;
            }
            // TODO 向解析起分配socket
            // processor.attach(socket);
        }
        // 通知线程终结方法已经成功关闭socket
        synchronized (lock) {
            lock.notifyAll();
        }


    }

    void threadStart() {
        logger.info("HttpConnector:后台线程启动");
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    void threadStop() {
        logger.info("HttpConnector:后台线程关闭");
        stopped = true;
        try {
            // 给处理线程5秒钟时间来收拾残局
            lock.wait(5000);
        } catch (InterruptedException _) {

        }
        thread = null;
    }

    void initialize() {
    }

    void start() {
    }

    void stop() {
    }

    //</editor-fold>
    //<editor-fold desc="其他方法">
    private ServerSocket openSocket() throws BindException {
        ServerSocketFactory factory = getFactory();
        // 不限定连接到此服务器的IP地址
        if (address == null) {
            logger.info("HttpConnector:已面向所有地址开启套接字");
            try {
                return (factory.createSocket(port, acceptCount));
            } catch (IOException e) {
                logger.warn("开启套接字时出现错误:{},Port:{}", e.getMessage(), port);
                throw new BindException(e.getMessage() + ":" + port);
            }
        }
        // 面向指定的IP地址开启套接字
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            logger.info("HttpConnector:已面向IP:{}开启套接字", inetAddress);
            try {
                return (factory.createSocket(port, acceptCount, inetAddress));
            } catch (IOException e) {
                logger.warn("HttpConnector:开启套接字时出现错误:{},IP:{},Port:{}", e.getMessage(), address, port);
                throw new BindException(e.getMessage() + ":" + address +
                        ":" + port);
            }
        } catch (Exception e) {
            logger.warn("HttpConnector:不存在与IP:{}相匹配的IP地址，已面向所有IP开启套接字", address);
            try {
                return (factory.createSocket(port, acceptCount));
            } catch (IOException be) {
                logger.warn("开启套接字时出现错误:{},Port:{}", e.getMessage(), port);
                throw new BindException(be.getMessage() + ":" + port);
            }
        }
    }

    public HttpRequest createRequest() {
        HttpRequest request = new HttpRequest();
        request.setConnector(this);
        return request;
    }

    public HttpResponse createResponse() {
        HttpResponse response = new HttpResponse();
        response.setConnector(this);
        return response;
    }

    /**
     * 创建解析器
     **/
    private HttpProcessor createProcessor() {
        HttpProcessor processor = processors.poll();
        if (processor != null) {
            return processor;
        }

        // 创建新processor时检查数量限制
        int current = currentProcessors.get();
        if (maxProcessors > 0 && current >= maxProcessors) {
            return null;
        }

        if (currentProcessors.compareAndSet(current, current + 1)) {
            return newProcessor();
        }

        // 重试从队列获取
        processor = processors.poll();
        return processor;
    }

    private HttpProcessor newProcessor() {
        HttpProcessor processor = null;
        try {
            processor = new HttpProcessor(this, currentProcessors);
        } catch (IOException e) {
            logger.error("解析器创建失败", e);
            return null;
        }
        processor.start();
        // 添加到已创建解析器的列表中
        created.add(processor);
        return processor;
    }

    /**
     * 回收资源
     */
    void recycle(HttpProcessor processor) {
        processors.offer(processor);
    }
    //</editor-fold>
}
