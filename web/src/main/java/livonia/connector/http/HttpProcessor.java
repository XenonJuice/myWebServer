package livonia.connector.http;

import livonia.base.Const.*;
import livonia.lifecycle.Lifecycle;
import livonia.lifecycle.LifecycleException;
import livonia.lifecycle.LifecycleListener;
import livonia.log.BaseLogger;
import livonia.utils.LifecycleHelper;
import livonia.utils.ServerInfo;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;

import static livonia.base.Const.CharPunctuationMarks.CR;
import static livonia.base.Const.CharPunctuationMarks.LF;
import static livonia.base.Const.HttpProtocol.HTTP_0_9;
import static livonia.utils.CookieUtils.convertToCookieArray;
import static livonia.utils.CookieUtils.convertToCookieList;

/**
 * HTTP解析器 - 用于解析HTTP请求并生成响应。
 * <p>
 * 主要流程：
 * <pre>
 * └── {@link #process(Socket)} - 处理传入 Socket 的完整流程：
 *      1. 初始化请求与响应
 *          └── - 初始化请求与响应对象
 *      2. 请求解析
 *          └── {@link #parseRequestAndConnection(Socket)} - 解析请求与连接部分：
 *              ├── {@link #parseRequest(HttpRequest)} - 解析请求行
 *              ├── {@link #parseConnection(Socket)} - 解析连接属性（协议、超时）
 *              ├── {@link #parseHeaders(Map)} - 解析请求头：
 *                  │   ├── {@link #parseCookies(String)} - 解析 Cookies
 *                  │   └── 其他 Header 处理逻辑（Host, Accept-Language 等）
 *              └── {@link #parseParameters(String)} - 解析 URL 参数
 *      3. 请求组装
 *          └── {@link #assembleRequest} - 组装最终的请求对象
 *      4. 处理具体业务逻辑并生成响应数据
 *      5. 资源清理
 *          └── {@link #recycle()} - 清理内存与 Socket 相关资源
 * </pre>
 * <p>
 * 用途：
 * <p>- 用于解析 HTTP 请求、提取请求数据、并返回业务响应。
 *
 * @author LILINJIAN
 * @version 2024/12/19
 */
public class HttpProcessor extends BaseLogger implements Runnable, Lifecycle {
    //<editor-fold desc = "attr">
    private final HttpRequest request;
    private final HttpResponse response;
    // 与此解析器绑定的连接器
    private final HttpConnector connector;
    // 代理端口、名 (从绑定的连接器中获取)
    private final String proxyName;
    private final int proxyPort;
    // 对象锁
    private final Object lock = new Object();
    private final Logger logger = BaseLogger.getLogger(this.getClass());
    // 缓冲区大小
    private final int bufferSize = 8192;
    // 从请求中获得的字符编码
    public String characterEncoding;
    // 存储HTTP头的映射
    public Map<String, List<String>> headers = new HashMap<>();
    // 存储Cookie的映射
    public Map<String, String> cookies = new HashMap<>();
    // 存储请求参数的映射
    public Map<String, List<String>> parameters = new HashMap<>();
    public String method;
    public String fullUri;
    public String protocol;
    public String uri;
    // 生命周期助手
    protected LifecycleHelper helper = new LifecycleHelper(this);
    // 是否可以发送响应标志位
    boolean finishResponse = true;
    // 服务器实际端口
    private int serverPort = 0;
    // 线程停止信号
    private boolean stopped = false;
    // 是否有可用的socket
    private boolean hasSocket = false;
    // 解析器状态
    private int status = Processor.PROCESSOR_IDLE;
    // 长连接标志位
    private boolean keepAlive = false;
    // http11标志位
    private boolean http11 = false;
    // 确认消息标志位
    private boolean ack = false;
    // 当前解析器持有的socket
    private Socket socket = null;
    // 当前线程
    private Thread thread = null;
    // 当前线程名
    private String threadName = null;
    // 线程启动标志位
    private boolean started = false;
    // 复用的流对象
    private HttpRequestStream requestStream;
    private HttpResponseStream responseStream;
    // 统计信息
    private long totalRequestsProcessed = 0;
    private long totalBytesRead = 0;
    private long totalBytesWritten = 0;

    //</editor-fold>
    //<editor-fold desc = "constructor">
    public HttpProcessor(HttpConnector connector, int id) {
        this.connector = connector;
        this.proxyName = connector.getProxyName();
        this.proxyPort = connector.getProxyPort();
        this.serverPort = connector.getPort();
        this.request = connector.createRequest();
        this.response = connector.createResponse();
        this.characterEncoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
        this.threadName = "HttpProcessor[" + connector.getPort() + "][" + id + "]";
    }

    //</editor-fold>
    //<editor-fold desc = "解析相关">
    private void parseRequestAndConnection(Socket socket) throws IOException, ServletException {
        parseConnection(socket);
        parseRequest(request);
        if (http11) {
            if (ack) response.sendAck();
            if (connector.isAllowChunking()) response.setAllowChunking(true);
        }
    }

    /**
     * 解析连接
     */
    private void parseConnection(Socket socket) {
        logger.info("解析连接: IP : {} port : {}",
                socket.getInetAddress(), connector.getPort());
        request.setInet(socket.getInetAddress());
        if (proxyPort != 0) request.setServerPort(proxyPort);
        else request.setServerPort(serverPort);
        request.setSocket(socket);
    }

    /**
     * 使用字节读取一行数据，直到遇到 \r\n 为止。
     * 如果到达流的末尾返回 null。
     */
    private String readLine(InputStream in, String charset) throws IOException {
        byte[] buffer = new byte[1024]; // 每次最多读取1024个字节
        StringBuilder lineBuilder = new StringBuilder(); // 用于存储结果
        boolean lastWasCR = false; // 标识上一个字节是否是 CR

        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                byte b = buffer[i];
                if (b == CR) { // 检测到 CR
                    lastWasCR = true;
                } else if (b == LF) { // 检测到 LF
                    if (lastWasCR) {
                        // 如果前一个是 CR，现在是 LF，说明一行结束
                        return lineBuilder.toString(); // 返回当前行
                    }
                } else {
                    // 如果前一位是CR，但现在不是LF，则将CR加入结果
                    if (lastWasCR) {
                        lineBuilder.append(CR);
                        lastWasCR = false; // 重置状态
                    }
                    // 将当前字节加入结果
                    lineBuilder.append((char) b);
                }
            }
        }

        // 流结束，处理最后一行
        if (!lineBuilder.isEmpty() || lastWasCR) {
            return lineBuilder.toString();
        }

        // 没有读取到任何数据时，返回 null 表示流结束
        return null;
    }

    /**
     * 解析HTTP请求
     */
    private void parseRequest(HttpRequest request) throws IOException, ServletException {
        InputStream input = request.getInputStream();
        // 1. 读取并解析请求行
        String requestLine = readLine(input, characterEncoding);
        status = Processor.PROCESSOR_ACTIVE;
        if (requestLine == null || requestLine.isEmpty()) {
            throw new ServletException("Empty request");
        }

        String[] requestLineParts = requestLine.split(PunctuationMarks.SPACE, 3);
        if (requestLineParts.length == 2) {
            // HTTP/0.9
            method = requestLineParts[0];
            fullUri = requestLineParts[1];
            protocol = HTTP_0_9;  // 设置默认协议版本
        } else if (requestLineParts.length == 3) {
            // HTTP/1.0
            method = requestLineParts[0];
            fullUri = requestLineParts[1];
            protocol = requestLineParts[2];
        } else {
            throw new ServletException("Invalid request line: " + requestLine);
        }
        if (protocol.equals(HttpProtocol.HTTP_1_1)) {
            http11 = true;
        }

        logger.info("请求方法: {}", method);
        logger.info("完整URI: {}", fullUri);
        logger.info("协议版本: {}", protocol);

        // 解析URI和查询字符串
        if (fullUri.contains("?")) {
            String[] uriParts = fullUri.split("\\?", 2);
            uri = uriParts[0];
            parseParameters(uriParts[1]);
            logger.info("解析后的URI: {}", uri);
            logger.info("解析后的查询参数: {}", parameters);
        } else {
            uri = fullUri;
        }

        if (!this.protocol.startsWith(HTTP_0_9.substring(0, 6))) {
            // 2. 解析请求头
            int contentLength = 0;
            String contentType = null;

            String headerLine;
            while ((headerLine = readLine(input, characterEncoding)) != null && !headerLine.isEmpty()) {
                int separatorIndex = headerLine.indexOf(PunctuationMarks.COLON_SPACE);
                if (separatorIndex == -1) {
                    logger.info("格式错误的头部: {}", headerLine);
                    continue;
                }
                String key = headerLine.substring(0, separatorIndex).trim();
                String value = headerLine.substring(separatorIndex + 2).trim();
                headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                logger.info("头部: {} = {}", key, value);

                if (Header.CONTENT_LENGTH.equalsIgnoreCase(key)) {
                    try {
                        contentLength = Integer.parseInt(value);
                        logger.info("Content-Length: {}", contentLength);
                    } catch (NumberFormatException e) {
                        throw new ServletException("Invalid Content-Length value: " + value);
                    }
                }

                if (Header.CONTENT_TYPE.equalsIgnoreCase(key)) {
                    contentType = value;
                    String[] typeParts = value.split(PunctuationMarks.SEMICOLON);
                    if (typeParts.length > 1) {
                        String charsetPart = typeParts[1].trim();
                        if (charsetPart.startsWith("charset=")) {
                            String charset = charsetPart.substring("charset=".length());
                            response.setCharacterEncoding(charset);
                            this.characterEncoding = charset;
                        }
                    }
                }
                // 解析Cookies
                if (Header.COOKIE.equalsIgnoreCase(key)) {
                    parseCookies(value);
                }
            }
        }
        assembleRequest(request, method, uri, protocol, headers, parameters);
        logger.debug("HTTP请求解析完成");
    }

    /**
     * 解析查询字符串参数
     */
    private void parseParameters(String queryString) throws UnsupportedEncodingException {
        logger.info("解析参数: {}", queryString);
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], characterEncoding);
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], characterEncoding) : "";
            parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            logger.debug("参数: {} = {}", key, value);
        }
    }


    /**
     * 解析Cookie字符串
     */
    private void parseCookies(String cookieHeader) {
        String[] cookiePairs = cookieHeader.split("; ");
        for (String cookie : cookiePairs) {
            String[] keyValue = cookie.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                cookies.put(key, value);
                logger.debug("解析Cookie: {} = {}", key, value);
            }

        }
    }

    /**
     * 组装请求对象
     */
    private void assembleRequest(HttpRequest request, String method, String uri, String protocol, Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        parseHeaders(headers);
        request.setMethod(method);
        request.setUri(uri);
        request.setProtocol(protocol);
        request.setHeaders(headers);
        request.setParameters(parameters);
        request.setCookies(convertToCookieList(cookies));
    }

    /**
     * 解析请求头
     */
    private void parseHeaders(Map<String, List<String>> headers) {
        if (headers.isEmpty()) return;
        // 设置确认反馈
        if (headers.containsKey(Ack.EXCEPT_ACKNOWLEDGEMENT)) {
            ack = true;
        }
        // 设置connection属性
        if (headers.containsKey(Header.CONNECTION)) {
            List<String> connectionHeaders = headers.get(Header.CONNECTION);
            if (connectionHeaders != null && !connectionHeaders.isEmpty()) {
                String connection = connectionHeaders.getFirst();
                if (connection != null && !connection.isEmpty()) {
                    if (connection.equalsIgnoreCase(Header.CLOSE)) {
                        keepAlive = false;
                        response.addHeader(Header.CONNECTION, Header.CLOSE); // FIXME may cause conflict
                    } else if (connection.equalsIgnoreCase(Header.KEEP_ALIVE)) {
                        keepAlive = true;
                        response.addHeader(Header.CONNECTION, Header.KEEP_ALIVE);// FIXME may cause conflict
                    }
                }
            }
        }

        // 为request设置权限
        if (headers.containsKey(Header.AUTHORIZATION)) {
            //TODO
            System.out.println("权限未实现");
            // request.setAuthorization();
        }
        // 为request设置语言
        if (headers.containsKey(Header.ACCEPT_LANGUAGE)) {
            Locale highestPriorityLocale = null;
            List<String> acceptLanguageHeaders = headers.get(Header.ACCEPT_LANGUAGE);
            if (acceptLanguageHeaders != null && !acceptLanguageHeaders.isEmpty()) {
                String acceptLanguage = acceptLanguageHeaders.getFirst(); // 只取第一个值
                if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                    String[] locales = acceptLanguage.split(PunctuationMarks.COMMA);
                    double highestWeight = -1.0;

                    for (String localeEntry : locales) {
                        String[] parts = localeEntry.trim().split(PunctuationMarks.COMMA);
                        String languageTag = parts[0].trim();
                        double weight = 1.0; // 默认权重

                        // 解析权重（如果存在）
                        if (parts.length > 1 && parts[1].trim().startsWith("q=")) {
                            try {
                                weight = Double.parseDouble(parts[1].trim().substring(2));
                            } catch (NumberFormatException e) {
                                // 忽略异常，使用默认权重
                            }
                        }
                        // 如果当前权重更高，更新最高优先语言
                        if (weight > highestWeight) {
                            highestWeight = weight;
                            highestPriorityLocale = Locale.forLanguageTag(languageTag);
                        }
                    }
                    // 设置语言
                    if (highestPriorityLocale != null) {
                        request.setLocale(highestPriorityLocale);
                    } else {
                        request.setLocale(Locale.getDefault());
                    }
                }
            }
        }
        // 为request设置sessionId使用来源
        if (headers.containsKey(Header.COOKIE)) {
            List<String> cookieHeaders = headers.get(Header.COOKIE);
            if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
                Cookie[] cookies = convertToCookieArray(cookieHeaders);
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(Header.SESSION_ID)) {
                        if (!request.isRequestedSessionIdFromCookie()) {
                            String value = cookie.getValue();
                            logger.info("设置sessionId: {}", value);
                            request.setRequestedSessionId(cookie.getValue());
                            request.setRequestedSessionIdFromCookie(true);
                            request.setRequestedSessionIdFromURL(false);
                        }
                    }
                }
            }
        }
        // 为request设置host
        if (headers.containsKey(Header.HOST)) {
            List<String> hostHeaders = headers.get(Header.HOST);
            if (hostHeaders != null && !hostHeaders.isEmpty()) {
                String host = hostHeaders.getFirst();
                if (host != null && !host.isEmpty()) {
                    if (host.startsWith("[")) { // 针对IPv6处理
                        // 检查是否是IPv6格式 [地址]:端口
                        int closingBracketIndex = host.indexOf(']');
                        if (closingBracketIndex < 0) {
                            throw new IllegalArgumentException("Invalid IPv6 address in Host header: " + host);
                        }

                        String serverName = host.substring(1, closingBracketIndex).trim(); // 提取IPv6地址
                        int port = getPort(closingBracketIndex, host);

                        // 设置主机名和端口号
                        request.setServerName(serverName);
                        request.setServerPort(port);

                    } else { // 针对IPv4或主机名处理
                        int colonIndex = host.indexOf(':'); // 检查是否包含冒号
                        if (colonIndex < 0) {
                            request.setServerName(Objects.requireNonNullElse(proxyName, host.trim()));
                            request.setServerPort(getDefaultPort(connector.getScheme()));
                        } else {
                            // 分离主机名和端口号
                            String serverName = host.substring(0, colonIndex).trim();
                            String portString = host.substring(colonIndex + 1).trim();
                            int port = getDefaultPort(connector.getScheme()); // 默认端口
                            try {
                                port = Integer.parseInt(portString); // 尝试解析端口号
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Invalid port number in Host header: " + host);
                            }

                            // 设置主机名和端口号
                            request.setServerName(serverName);
                            request.setServerPort(port);
                        }
                    }
                }
            }
        }
    }

    private int getPort(int closingBracketIndex, String host) {
        String portString = null;
        if (closingBracketIndex + 1 < host.length() && host.charAt(closingBracketIndex + 1) == ':') {
            portString = host.substring(closingBracketIndex + 2).trim(); // 提取端口号
        }

        int port = getDefaultPort(connector.getScheme()); // 默认端口
        if (portString != null) {
            try {
                port = Integer.parseInt(portString); // 尝试解析端口号
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number in Host header: " + host);
            }
        }
        return port;
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">

    /**
     * 获取默认端口号
     */
    private int getDefaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return 80; // 默认HTTP端口
    }

    // 清理内存
    private void recycle() {
        // 重置协议相关标志
        this.http11 = false;
        this.ack = false;
        this.protocol = null;
        // 清空集合
        this.headers.clear();
        this.cookies.clear();
        this.parameters.clear();
        // 重置请求信息
        this.method = null;
        this.uri = null;
        this.fullUri = null;
        // 重置其他标志
        this.finishResponse = true;
        this.characterEncoding = "UTF-8";
        // 回收请求和响应对象
        request.recycle();
        response.recycle();
        /*
         不重置keepAlive，它应该在每个连接开始时设置
         不回收流对象，它们会被复用
        */
    }

    public void recycleByConnector(){
        // 清理所有资源
        this.requestStream = null;
        this.responseStream = null;
        this.totalRequestsProcessed = 0;
        this.totalBytesRead = 0;
        this.totalBytesWritten = 0;
    }

    private void closeInputStream(InputStream input) {
        try {
            if (input.available() > 0) {
                long skip = input.skip(input.available());
                logger.info("已跳过字节数 ：{}", skip);
            }
        } catch (IOException e) {
            logger.error("closeInputStream", e);
        }
    }

    //</editor-fold>
    //<editor-fold desc = "线程相关">
    protected synchronized void receiveSocket(Socket socket) {
        // 当前解析器已持有一个socket时，等待
        while (hasSocket) {
            try {
                wait();
            } catch (InterruptedException _) {
            }
        }
        // 当前不持有socket时
        this.socket = socket;
        hasSocket = true;
        // 唤醒waitSocket()
        notifyAll();
        if (socket == null) {
            logger.info("收到停止信号，socket为null");
        } else {
            logger.info("已分配到一个请求,来自：{}", socket.getRemoteSocketAddress());
        }
    }

    private synchronized Socket waitSocket() {
        // 当前解析器不持有一个socket时，等待
        while (!hasSocket) {
            try {
                wait();
            } catch (InterruptedException _) {
            }
        }
        Socket socket = this.socket;
        hasSocket = false;
        /* notifyAll();
         * 这里是对关键操作的详细解释：
         * 1. HttpConnector通过receiveSocket()向Processor传递socket，与此同时，生命周期函数可能被触发，
         *    HttpConnector所在线程会再次调用processor.stop()，间接导致receiveSocket(null)进入等待状态。
         * 2. 在首次的receiveSocket(socket)之后，notifyAll()会唤醒在processor线程上阻塞的waitSocket()，
         *    并且释放锁，但是在 1. 中receiveSocket(null)在此时也被唤醒，此时有两条线程试图获取processor
         *    的对象锁，分别是connector线程和processor线程，如果这时分配null的connector线程先于从阻塞在
         *    waitSocket()的processor的线程获得了对象锁，会紧接着再次进入receiveSocket()中
         * 3. 由于前一次进入receiveSocket()导致boolean hasSocket被修改为true，receiveSocket(null)会
         *    阻塞在wait()，并且释放锁
         * 4. 这时唯一处于就绪状态的processor线程调用waitSocket()，继续处理第一次connector线程向processor
         *    中分配的socket，将hasSocket修改为false，同时唤醒等待状态中的分配null的线程。
         * 5. 正常进行之后的线程停止流程...
         */
        notifyAll();
        logger.info("正在等待请求");
        return socket;
    }

    @Override
    public void run() {
        while (!stopped) {
            Socket socket = waitSocket();
            // 当socket为null时，说明生命周期方法stop被调用
            if (socket == null) {
                continue;
            }
            try {
                process(socket);
            } catch (Throwable e) {
                logger.error("<=>", e);
            } finally {
                connector.recycle(this);
            }
        }

        synchronized (lock) {
            lock.notifyAll();
        }

    }

    void threadStart() {
        logger.info("HttpProcessor:后台线程启动");
        thread = new Thread(this, threadName);
        // HttpProcessor作为socket解析器，可设置为守护线程
        thread.setDaemon(true);
        thread.start();
    }

    void threadStop() {
        logger.info("HttpProcessor:后台线程关闭");
        stopped = true;
        receiveSocket(null);
        // 在当前处理器所在线程工作时，等待五秒收拾残局
        if (status == Processor.PROCESSOR_ACTIVE) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException _) {
                    // ignore
                }
            }
            thread = null;

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

    public void start() throws LifecycleException {
        if (started) {
            logger.warn("HttpProcessor:already started,ignore this start request");
            throw new LifecycleException("HttpProcessor:already started,ignore this start request");
        }
        helper.fireLifecycleEvent(Lifecycle.START_EVENT, null);
        started = true;
        threadStart();
    }

    public void stop() throws LifecycleException {
        if (!started) {
            logger.warn("HttpProcessor:not started,ignore this stop request");
            throw new LifecycleException("HttpProcessor:not started,ignore this stop request");
        }
        if (thread != null) {
            threadStop();
        }
    }

    //</editor-fold>
    //<editor-fold desc = "process">
    public void process(Socket socket) {
        boolean ok = true;
        boolean finishResponse = true;
        InputStream socketInputStream= null;
        OutputStream output = null;

        // 初始化keepAlive为true，允许连接复用
        keepAlive = true;
        try {
            // 获取输入流
            socketInputStream = new SocketInputBuffer(socketInputStream, bufferSize);
        } catch (Exception e) {
            logger.error("Socket初始化失败", e);
            ok = false;
        }
        while (!stopped && ok && keepAlive) {
            try {
                // 设置请求和响应的流
                request.setStream(socketInputStream);
                request.setResponse(response);
                // 获取输出流
                output = socket.getOutputStream();
                response.setStream(output);
                response.setRequest(request);
                // 设置默认响应头
                response.setHeader("Server", ServerInfo.getServerInfo());
            } catch (Exception e) {
                ok = false;
                logger.error("获取，设置流失败", e);
            }

            // IO无异常，继续解析
            try {
                parseRequestAndConnection(socket);
                // 对于HTTP/1.0，默认关闭连接除非明确指定keep-alive
                if (!http11 && !headers.containsKey(Header.CONNECTION)) {
                    keepAlive = false;
                }
            } catch (EOFException e) {
                // 客户端关闭连接
                logger.info("客户端关闭连接");
                ok = false;
                finishResponse = false;
            } catch (ServletException e) {
                logger.error("请求解析失败", e);
                response.setError();
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                } catch (IOException ex) {
                    logger.error("发送错误响应失败", ex);
                }
                ok = false;
            } catch (InterruptedIOException e) {
                logger.error("请求超时");
                ok = false;
                try {
                    response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                } catch (IOException ex) {
                    logger.error("发送超时响应失败", ex);
                }
            } catch (IOException e) {
                logger.error("IO错误", e);
                ok = false;
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ex) {
                    logger.error("发送错误响应失败", ex);
                }
            } catch (Exception e) {
                logger.error("未知错误", e);
                ok = false;
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ex) {
                    logger.error("发送错误响应失败", ex);
                }
            }


            if (http11 && keepAlive) {
                response.setHeader("Connection", "keep-alive");
            }
            // 创建或复用请求流
            if (requestStream == null) {
                requestStream = new HttpRequestStream(request, response);
            } else {
                requestStream.recycle();
                requestStream.setupForNewRequest(request);
            }
            request.setRequestStream(requestStream);

            // 创建或复用响应流
            if (responseStream == null) {
                responseStream = new HttpResponseStream(response, bufferSize);
            } else {
                responseStream.recycle();
            }
            response.setResponseStream(responseStream);

            // 处理请求
            if (ok) {
                try {
                    connector.getVas().process(request, response);
                } catch (Exception e) {
                    logger.error("请求处理失败", e);
                    ok = false;
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ex) {
                        logger.error("发送错误响应失败", ex);
                    }
                }
            }

            // 完成响应
            if (finishResponse) {
                try {
                    response.finishResponse();
                } catch (IOException e) {
                    logger.error("完成响应失败", e);
                    ok = false;
                } catch (Throwable e) {
                    logger.error("完成响应时发生严重错误", e);
                    ok = false;
                }

                try {
                    request.finishRequest();
                } catch (IOException e) {
                    logger.error("完成请求失败", e);
                    ok = false;
                } catch (Throwable e) {
                    logger.error("完成请求时发生严重错误", e);
                    ok = false;
                }

                try {
                    if (output != null) {
                        output.flush();
                    }
                } catch (IOException e) {
                    logger.error("刷新输出流失败", e);
                    ok = false;
                }
            }

            // 检查连接是否应该关闭
            if ("close".equalsIgnoreCase(response.getHeader("Connection"))) {
                keepAlive = false;
            }

            // 对于错误响应，通常关闭连接
            if (!ok || response.getStatus() >= 400) {
                keepAlive = false;
            }

            // 更新统计信息
            if (requestStream != null) {
                totalBytesRead += requestStream.getBytesRead();
            }
            if (responseStream != null) {
                totalBytesWritten += responseStream.getTotalBytesWritten();
            }
            totalRequestsProcessed++;

            // 回收资源准备下一个请求
            recycle();

            status = Processor.PROCESSOR_IDLE;
        }

        try {
            closeInputStream(socket.getInputStream());
            socket.close();
        } catch (IOException e) {
            logger.error("关闭socket失败", e);
        }
        socket = null;

        logger.info("处理器统计 - 请求数: {}, 读取字节: {}, 写入字节: {}",
                totalRequestsProcessed, totalBytesRead, totalBytesWritten);
    }

    //</editor-fold>
    //</editor-fold>

}



