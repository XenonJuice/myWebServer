package erangel.connector.http;

import erangel.net.DefaultServerSocketFactory;
import erangel.net.ServerSocketFactory;

import java.net.ServerSocket;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpConnector implements Runnable {
    // 描述信息
    private static final String info = "llj.erangel.connector.http.HttpConnector/1.0";
    // 经过本连接器处理的所有请求的协议名
    private String scheme = "http";
    // 最大连接数
    private int acceptCount = 10;
    // 端口名
    private int port = 8080;
    // 最大解析器数量
    private int maxProcessors = 100;
    // 最小解析器数量
    private int minProcessors = 20;
    // 线程容器
    private final ConcurrentLinkedQueue<HttpProcessor> processors = new ConcurrentLinkedQueue<>();
    // 已创建的解析器线程之容器
    private final CopyOnWriteArrayList<HttpProcessor> created = new CopyOnWriteArrayList<>();
    // 当前解析器数量
    private final AtomicInteger currentProcessors = new AtomicInteger(0);
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



    @Override
    public void run() {

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

    private ServerSocket openSocket() {
        return null;
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
}
