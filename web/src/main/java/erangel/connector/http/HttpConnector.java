package erangel.connector.http;

import erangel.net.DefaultServerSocketFactory;
import erangel.net.ServerSocketFactory;

import java.net.ServerSocket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpConnector implements Runnable {
    // 描述信息
    private static final String info = "llj.erangel.connector.http.HttpConnector/1.0";
    // 线程容器
    private final ConcurrentLinkedQueue<HttpProcessor> processors = new ConcurrentLinkedQueue<>();
    // 已创建的解析器线程之容器
    private final CopyOnWriteArrayList<HttpProcessor> created = new CopyOnWriteArrayList<>();
    // 当前解析器数量
    private final AtomicInteger currentProcessors = new AtomicInteger(0);
    // 经过本连接器处理的所有请求的协议名
    private String scheme = "http";
    // 最大连接数
    private int acceptCount = 10;
    // 端口名
    private int port = 8080;
    // 代理端口、名
    private int proxyPort = 0;
    private String proxyName = null;
    // 最大解析器数量
    private int maxProcessors = 100;
    // 最小解析器数量
    private int minProcessors = 20;
    // 服务器socket工厂
    private ServerSocketFactory factory = null;
    // 所有从TCP连接中接收到的服务器套接字
    private ServerSocket serverSocket = null;
    // 本线程
    private Thread thread = null;
    // 当前线程名称
    private String threadName = null;
    // 线程停止标志位
    private boolean stopped = false;

    // =================== getter & setter ===================
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

    // =================== 线程相关 ===================
    @Override
    public void run() {

    }

    void threadStart() {
    }

    void threadStop() {
    }

    void initalize() {
    }

    void start() {
    }

    void stop() {
    }

    private ServerSocket openSocket() {
        return null;
    }

}
