package erangel.connector.http;

import erangel.log.BaseLogger;
import erangel.net.DefaultServerSocketFactory;
import erangel.net.ServerSocketFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpConnector extends BaseLogger implements Runnable {
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
    // 希望监听特定的网络接口或IP
    private String address = null;
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

    /**
     * 获取监听HTTP请求的端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置监听HTTP请求的端口号
     */
    public void setPort(int port) {
        this.port = port;
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

    // =================== 线程相关 ===================
    @Override
    public void run() {

    }

    void threadStart() {
    }

    void threadStop() {
    }

    void initialize() {
    }

    void start() {
    }

    void stop() {
    }

    // =================== 其他方法 ===================
    private ServerSocket openSocket() {
        return null;
    }

    public HttpRequest createRequest() {
        // TODO 将请求与连接器绑定
        return new HttpRequest();
    }

    public HttpResponse createResponse() {
        // TODO 将响应与连接器绑定
        return new HttpResponse();
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
            logger.error("解析器创建失败",e);
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
}
