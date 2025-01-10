package erangel.connector.http;

import erangel.log.BaseLogger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
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
        this.servletInputStream = request.getInputStream();
        this.characterEncoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
        parseRequest();
        assembleRequest(request, method, uri, protocol, headers, parameters);
    }

    // ===================解析相关 ===================

    /**
     * 解析连接
     */
    private void parseConnection(Socket socket) {
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
    private void parseRequest() throws IOException {
        // 1. 读取并解析请求行
        String requestLine = readLine(servletInputStream, characterEncoding);
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request");
        }

        String[] requestLineParts = requestLine.split(" ", 3);
        if (requestLineParts.length != 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }
        method = requestLineParts[0];
        fullUri = requestLineParts[1];
        protocol = requestLineParts[2];

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

            if ("Content-Length".equalsIgnoreCase(key)) {
                try {
                    contentLength = Integer.parseInt(value);
                    logger.info("Content-Length: {}", contentLength);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid Content-Length value: " + value);
                }
            }

            if ("Content-Type".equalsIgnoreCase(key)) {
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
            if ("Cookie".equalsIgnoreCase(key)) {
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
                throw new IOException("Request body is incomplete");
            }
            logger.info("成功读取请求体");

            // 如果Content-Type是application/x-www-form-urlencoded，则对body进行解码并解析参数
            if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                String bodyStr = new String(body, characterEncoding);
                parseParameters(bodyStr);
                logger.info("解析后的请求体参数: {}", parameters);
            }
        }

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
        // 为request设置权限
        if (headers.containsKey("authorization")) {
            //System.out.println("权限未实现");
            // request.setAuthorization();
        }
        // 为request设置语言
        if (headers.containsKey("Accept-Language")) {
            Locale highestPriorityLocale = null;
            List<String> acceptLanguageHeaders = headers.get("Accept-Language");
            if (acceptLanguageHeaders != null && !acceptLanguageHeaders.isEmpty()) {
                String acceptLanguage = acceptLanguageHeaders.get(0); // 假设只取第一个值
                if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                    String[] locales = acceptLanguage.split(",");
                    double highestWeight = -1.0;

                    for (String localeEntry : locales) {
                        String[] parts = localeEntry.trim().split(";");
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
        if (headers.containsKey("Cookie")) {
            List<String> cookieHeaders = headers.get("Cookie");
            if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
                Cookie[] cookies = convertToCookieArray(cookieHeaders);
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("JSESSIONID")) {
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
        if (headers.containsKey("Host")) {
            List<String> hostHeaders = headers.get("Host");
            if (hostHeaders != null && !hostHeaders.isEmpty()) {
                String host = hostHeaders.get(0);
                if (host != null && !host.isEmpty()) {
                    int colonIndex = host.indexOf(':'); // 检查是否包含冒号
                    if (colonIndex < 0) {
                        // 没有冒号，只设置主机名
                        request.setServerName(host.trim());
                        request.setServerPort(80); // 默认端口
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

    // =================== 解析请求，生成响应===================
    private void process(Socket socket) {
        boolean noProblem = true;
        boolean finishResponse = true;
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new HttpRequestStream(socket.getInputStream());
        } catch (IOException e) {
            noProblem = false;
            logger.error("处理请求时发生IO错误:input", e);
        }
        // TODO 具体的处理逻辑
        keepAlive = true;
        while (!stopped && noProblem && keepAlive) {
            try {
                request.setStream(input);
                request.setResponse(response);
                output = socket.getOutputStream();
                response.setStream(output);
            } catch (IOException e) {
                noProblem = false;
                logger.error("处理请求时发生IO错误:output", e);
            }


        }

    }
}
