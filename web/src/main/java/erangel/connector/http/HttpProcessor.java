package erangel.connector.http;

import erangel.log.BaseLogger;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;

import erangel.connector.http.Const.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static erangel.connector.Utils.CookieUtils.convertToCookieArray;
import static erangel.connector.Utils.CookieUtils.convertToCookieList;

/**
 * HTTP解析器，用于解析HTTP请求。
 *
 * @author LILINJIAN
 * @version $Date: 2024/12/19 15:24
 */
public class HttpProcessor extends BaseLogger implements Runnable {
    //<editor-fold desc = "attr">
    private final HttpRequest request;
    private final HttpResponse response;
    // 从请求中获得的 ServletInputStream
    public ServletInputStream servletInputStream;
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
    // 解析器状态
    private int status = Processor.PROCESSOR_IDLE;
    // 解析器的ID
    private AtomicInteger id;
    // 与此解析器绑定的连接器
    private HttpConnector connector;
    // 代理端口、名 (从绑定的连接器中获取)
    private String proxyName;
    private int proxyPort;
    // 服务器实际端口
    private int serverPort = 0;
    // 长连接标志位
    private boolean keepAlive = false;
    // http11标志位
    private boolean http11 = false;
    // 确认消息标志位
    private boolean ack = false;
    // 线程停止信号
    private boolean stopped = false;

    //</editor-fold>
    public HttpProcessor(HttpConnector connector, AtomicInteger id) throws IOException {
        this.connector = connector;
        this.proxyName = connector.getProxyName();
        this.proxyPort = connector.getProxyPort();
        this.id = id;
        this.request = connector.createRequest();
        this.response = connector.createResponse();
        // this.servletInputStream = request.getInputStream();
        this.characterEncoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
        // assembleRequest(request, method, uri, protocol, headers, parameters);
    }

    // ===================解析相关 ===================

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
    private String readLine(ServletInputStream in, String charset) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;
        boolean lastWasCR = false;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                lastWasCR = true;
            } else if (c == '\n' && lastWasCR) {
                break; // 读取到完整行
            } else {
                if (lastWasCR) {
                    // \r 后没有紧跟 \n，将 \r 也写入缓冲
                    buffer.write('\r');
                    lastWasCR = false;
                }
                buffer.write(c);
            }
        }

        // 如果没读到任何数据且已到流末尾，返回null表示无更多行可读
        if (c == -1 && buffer.size() == 0 && !lastWasCR) {
            return null;
        }

        return buffer.toString(charset);
    }

    /**
     * 解析HTTP请求
     */
    private void parseRequest() throws  IOException,ServletException {
        // 1. 读取并解析请求行
        String requestLine = readLine(servletInputStream, characterEncoding);
        status = Processor.PROCESSOR_ACTIVE;
        if (requestLine == null || requestLine.isEmpty()) {
            throw new ServletException("Empty request");
        }

        String[] requestLineParts = requestLine.split(" ", 3);
        if (requestLineParts.length == 2) {
            // HTTP/0.9
            method = requestLineParts[0];
            fullUri = requestLineParts[1];
            protocol = HttpProtocol.HTTP_0_9;  // 设置默认协议版本
        } else if (requestLineParts.length == 3) {
            // HTTP/1.0
            method = requestLineParts[0];
            fullUri = requestLineParts[1];
            protocol = requestLineParts[2];
        } else {
            throw new ServletException("Invalid request line: " + requestLine);
        }
        if(protocol.equals(HttpProtocol.HTTP_1_1)){
            http11 = true;
            ack = true;
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

        if(!this.protocol.startsWith(HttpProtocol.HTTP_0_9.substring(0,6))) {

        // 2. 解析请求头
        int contentLength = 0;
        String contentType = null;

        String headerLine;
        while ((headerLine = readLine(servletInputStream, characterEncoding)) != null && !headerLine.isEmpty()) {
            int separatorIndex = headerLine.indexOf(": ");
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
                String[] typeParts = value.split(";");
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

        // 3. 根据Content-Length从流中按字节读取请求体
        if (contentLength > 0) {
            logger.info("开始读取请求体, 长度: {}", contentLength);
            byte[] body = new byte[contentLength];
            int bytesRead = 0;
            while (bytesRead < contentLength) {
                int readCount = servletInputStream.read(body, bytesRead, contentLength - bytesRead);
                if (readCount == -1) {
                    break;
                }
                bytesRead += readCount;
            }
            if (bytesRead != contentLength) {
                logger.warn("请求体不完整: 已读 {} 字节, 期望 {} 字节", bytesRead, contentLength);
                throw new ServletException("Request body is incomplete");
            }
            logger.info("成功读取请求体");

            // 如果Content-Type是application/x-www-form-urlencoded，则对body进行解码并解析参数
            if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                String bodyStr = new String(body, characterEncoding);
                parseParameters(bodyStr);
                logger.info("解析后的请求体参数: {}", parameters);
            }
        }
        }
        assembleRequest(request, method, uri, protocol, headers, parameters);
        logger.info("HTTP请求解析完成");
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
            logger.info("参数: {} = {}", key, value);
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
                logger.info("解析Cookie: {} = {}", key, value);
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
    //TODO
    private void parseHeaders(Map<String, List<String>> headers) {
        if (headers.isEmpty()) return;
        // 设置确认反馈
        if (headers.containsKey(Ack.EXCEPT_ACKNOWLEDGEMENT)) {
            ack = true;
        }
        // 设置connection属性
        if(headers.containsKey(Header.CONNECTION)){
            List<String> connectionHeaders = headers.get(Header.CONNECTION);
            if (connectionHeaders != null && !connectionHeaders.isEmpty()) {
                String connection = connectionHeaders.get(0);
                if (connection != null && !connection.isEmpty()) {
                    if (connection.equalsIgnoreCase(Header.CLOSE)) {
                        keepAlive = false;
                    } else if (connection.equalsIgnoreCase(Header.KEEP_ALIVE)) {
                        keepAlive = true;
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
                String acceptLanguage = acceptLanguageHeaders.get(0); // 只取第一个值
                if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                    String[] locales = acceptLanguage.split(",");
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
                String host = hostHeaders.get(0);
                if (host != null && !host.isEmpty()) {
                    int colonIndex = host.indexOf(':'); // 检查是否包含冒号
                    if (colonIndex < 0) {
                        if (connector.getScheme().equals("http")) {
                                request.setServerPort(80);
                            } else if (connector.getScheme().equals("https")) {
                                request.setServerPort(443);
                        }
                        request.setServerName(host.trim());
                    } else {
                        // 分离主机名和端口号
                        String serverName = host.substring(0, colonIndex).trim();
                        String portString = host.substring(colonIndex + 1).trim();

                        int port = 80; // 默认端口
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

    // =================== 线程相关 ===================
    @Override
    public void run() {

    }

    void threadStart() {
    }

    void threadStop() {
    }

    void start() {
    }

    void stop() {
    }

    // 清理内存
    void recycle (){
        this.keepAlive = false;
        this.ack = false;
        this.http11 = false;
        this.protocol = null;
        this.headers = null;
        this.method = null;
        this.uri = null;
        this.fullUri = null;
        request.recycle();
        response.recycle();
    }

    // =================== 解析请求，生成响应===================
    private void process(Socket socket) {
        boolean noProblem = true;
        boolean finishResponse = true;
        // InputStream input = null;
        OutputStream output = null;
        try {
            servletInputStream = new HttpRequestStream(socket.getInputStream());
        } catch (IOException e) {
            noProblem = false;
            logger.error("处理请求时发生IO错误:input", e);
        }

        // TODO 具体的处理逻辑
        keepAlive = true;
        while (!stopped && noProblem && keepAlive) {
            finishResponse = true;
            try {
                request.setStream(servletInputStream);
                request.setResponse(response);
                output = socket.getOutputStream();
                response.setStream(output);
                response.setRequest(request);
            } catch (IOException e) {
                noProblem = false;
                logger.error("处理请求时发生IO错误:output", e);
            }

            // IO无异常，继续解析
            try {
                if (noProblem) {
                    parseConnection(socket);
                    parseRequest();
                    if(http11){
                        if(ack) response.sendAck();
                        if(connector.isAllowChunking()) response.setAllowChunking(true);
                    }
                }
            } catch (EOFException e){
                // 客户端或者服务器socket断开
                logger.warn("client or server socket shutdown!");
                noProblem = false;
                finishResponse = false;
            } catch (ServletException e){
                // request内容不合法
                logger.info("request内容不合法: {}",e.getMessage());
                try {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                } catch (IOException ex) {
                    logger.warn("发送错误响应时失败: {}", ex.getMessage());
                }
            } catch (InterruptedIOException e) {
                // 请求超时
                noProblem = false;
                try {
                    response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                } catch (IOException ee) {
                    logger.warn("发送错误响应时失败: {}", ee.getMessage());
                }
            } catch (IOException e) {
                // 服务器读取请求时发生异常
                logger.error("处理请求时发生IO错误:parseRequest", e);
                noProblem = false;
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ee) {
                    logger.warn("发送错误响应时失败: {}", ee.getMessage());
                }

            }  catch (Exception e) {
                // 其他异常
                logger.error("未知异常", e);
                noProblem = false;
                try {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } catch (IOException ee) {
                    logger.warn("发送错误响应时失败: {}", ee.getMessage());
                }
            }

        }

        // TODO servlet容器加载
        // container.invoke(request,response)

    }
}
