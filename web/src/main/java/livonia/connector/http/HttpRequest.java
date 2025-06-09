package livonia.connector.http;

import livonia.base.Endpoint;
import livonia.log.BaseLogger;

import javax.servlet.*;
import static livonia.base.Const.PunctuationMarks.COMMA;
import static livonia.base.Const.PunctuationMarks.SEMICOLON;
import javax.servlet.http.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;

/**
 * 自定义 HttpRequest 类，实现 HttpServletRequest 接口。
 * 负责解析HTTP请求，并提供访问请求数据的方法。
 */
public class HttpRequest extends BaseLogger implements HttpServletRequest {
    //<editor-fold desc="attr">
    // 存储请求属性
    private final Map<String, Object> attributes = new HashMap<>();
    // sessionId是否来自cookie？
    protected boolean isRequestedSessionIdFromCookie = false;
    // sessionId是否来自URL？
    protected boolean isRequestedSessionIdFromURL = false;
    // 服务器名
    protected String serverName = null;
    // 服务器端口
    protected int serverPort = -1;
    // 与此request绑定的连接器
    private HttpConnector connector;
    // socket
    private Socket socket;
    // HTTP请求方法（例如：GET, POST）
    private String method;
    // 请求URI（例如：/index.html）
    private String uri;
    // 协议版本（例如：HTTP/1.1）
    private String protocol;
    // response;
    private HttpResponse response;
    // 存储HTTP头的映射
    private Map<String, List<String>> headers;
    // 存储请求参数的映射
    private Map<String, List<String>> parameters;
    // 请求体内容（字节数组形式）
    private byte[] body = null;
    //存储远程客户端的IP 地址
    private InetAddress inet;
    // 请求的远程地址和主机名
    private String remoteAddr;
    private String remoteHost;
    // 策略
    private String scheme;
    // 存储请求的 Locale
    private Locale locale;
    // 字符编码
    private String characterEncoding = "UTF-8";
    // 存储请求的 Cookie
    private ArrayList<Cookie> cookies = new ArrayList<>();
    // 客户端在请求中携带的SessionID
    private String requestedSessionId = null;
    // /LLJ/LLJ-home/login 则为/LLJ-home/login
    private String servletPath = null;
    // 在前缀匹配模式下则为login
    private String pathInfo = null;
    // 绑定的servlet端点
    private Endpoint endpoint = null;
    private String contextPath;
    // 流相关
    private InputStream socketInputBuffer;
    // （HttpServletStream）
    private ServletInputStream requestStream;
    private BufferedReader reader;
    private boolean streamUsed = false;
    private boolean readerUsed = false;
    // POST参数是否已解析
    private boolean postParametersParsed = false;


    //</editor-fold>

    //<editor-fold desc="流管理方法">
    /**
     * 设置底层输入流（通常是Socket的输入流）
     * 这个方法在每个新请求开始时被调用
     */
    public void setStream(InputStream inputStream) {
        this.socketInputBuffer = inputStream;
    }

    /**
     * 获取ServletInputStream
     * 延迟创建，支持复用
     */
    @Override
    public ServletInputStream getInputStream() {
        if (readerUsed) {
            throw new IllegalStateException("getReader() has already been called");
        }

        streamUsed = true;

        if (requestStream == null) {
            requestStream = createInputStream();
        }

        return requestStream;
    }

    /**
     * 创建新的输入流（仅在必要时调用）
     */
    protected ServletInputStream createInputStream() {
        return new HttpRequestStream(this);
    }

    /**
     * 设置请求流（由HttpProcessor调用）
     */
    public void setRequestStream(HttpRequestStream stream) {
        this.requestStream = stream;
    }

    /**
     * 获取Reader
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (streamUsed) {
            throw new IllegalStateException("getInputStream() has already been called");
        }
        readerUsed = true;
        if (reader == null) {
            InputStreamReader isr = new InputStreamReader(
                    getStream(), getCharacterEncoding());
            reader = new BufferedReader(isr);
        }
        return reader;
    }

    /**
     * 获取底层的SocketInputBuffer（供其他组件使用）
     */
    public InputStream getStream() {
        return socketInputBuffer;
    }
    //</editor-fold>

    //<editor-fold desc="getter & setter">
    public HttpConnector getConnector() {
        return connector;
    }

    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }

    public InetAddress getInet() {
        return inet;
    }

    public void setInet(InetAddress inet) {
        this.inet = inet;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies.toArray(new Cookie[0]);
    }

    public void setCookies(ArrayList<Cookie> cookies) {
        this.cookies = cookies;
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) return -1;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return (values != null && !values.isEmpty()) ? values.getFirst() : null;
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = headers.get(name);
        return Collections.enumeration(values != null ? values : Collections.emptyList());
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public Map<String, List<String>> getHeaderMap() {
        return headers;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getPathInfo() {
        // 简单实现，根据需求调整
        return null;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    @Override
    public String getPathTranslated() {
        // 简单实现，根据需求调整
        return null;
    }

    @Override
    public String getContextPath() {
        return this.contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public String getQueryString() {
        // 将参数映射转换为查询字符串
        if (parameters.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                if (!sb.isEmpty()) sb.append("&");
                sb.append(key).append("=").append(value);
            }
        }
        return sb.toString();
    }

    @Override
    public String getRemoteUser() {
        // 未实现认证逻辑
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        // 未实现认证逻辑
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        // 未实现认证逻辑
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }

    public void setRequestedSessionId(String requestedSessionId) {
        this.requestedSessionId = requestedSessionId;
    }

    @Override
    public String getRequestURI() {
        return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        // 未实现完整URL构建逻辑
        return new StringBuffer(uri);
    }

    @Override
    public String getServletPath() {
        return servletPath != null ? servletPath : "";
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    @Override
    public HttpSession getSession(boolean create) {
        // 未实现会话管理
        return null;
    }

    @Override
    public HttpSession getSession() {
        // 未实现会话管理
        return null;
    }

    @Override
    public String changeSessionId() {
        // 未实现会话管理
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        // 未实现会话管理
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return isRequestedSessionIdFromCookie;
    }

    public void setRequestedSessionIdFromCookie(boolean flag) {

        this.isRequestedSessionIdFromCookie = flag;

    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return isRequestedSessionIdFromURL;
    }

    public void setRequestedSessionIdFromURL(boolean flag) {

        this.isRequestedSessionIdFromURL = flag;

    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws ServletException {
        // 未实现认证逻辑
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        // 未实现认证逻辑
    }

    @Override
    public void logout() throws ServletException {
        // 未实现认证逻辑
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        // 未实现多部分请求解析
        return Collections.emptyList();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        // 未实现多部分请求解析
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // 未实现升级协议（如WebSocket）
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        if (env == null || env.isEmpty()) {
            throw new UnsupportedEncodingException("Character encoding is null or empty");
        }
        this.characterEncoding = env;
    }

    @Override
    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Integer.parseInt(contentLength) : -1;
    }

    public void setContentLength(int len) {
        headers.put("Content-Length", Collections.singletonList(String.valueOf(len)));
    }

    @Override
    public long getContentLengthLong() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Long.parseLong(contentLength) : -1L;
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public String getParameter(String name) {
        parseParams();
        List<String> values = parameters.get(name);
        return (values != null && !values.isEmpty()) ? values.getFirst() : null;
    }

    public void setParameters(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        parseParams();
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        parseParams();
        List<String> values = parameters.get(name);
        if (values == null || values.isEmpty()) return null;
        return values.toArray(new String[0]);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        parseParams();
        Map<String, String[]> map = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
        return map;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String name) {
        this.serverName = name;

    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int port) {
        this.serverPort = port;
    }

    @Override
    public String getRemoteAddr() {
        // ip地址的字符串表示
        return inet.getHostAddress();
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        if (connector.isEnableLookups()) return inet.getHostName();
        return getRemoteAddr();
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        String acceptLanguage = getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return Collections.enumeration(Collections.singletonList(Locale.getDefault()));
        }
        String[] locales = acceptLanguage.split(COMMA);
        List<Locale> localeList = new ArrayList<>();
        for (String loc : locales) {
            String[] parts = loc.trim().split(SEMICOLON);
            try {
                localeList.add(Locale.forLanguageTag(parts[0]));
            } catch (Exception e) {
                // 忽略无效的Locale
            }
        }
        if (localeList.isEmpty()) {
            localeList.add(Locale.getDefault());
        }
        return Collections.enumeration(localeList);
    }

    @Override
    public boolean isSecure() {
        // 简单实现，根据实际情况调整
        return "https".equalsIgnoreCase(getScheme());
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // 未实现请求转发逻辑
        return null;
    }

    @Override
    @Deprecated
    public String getRealPath(String path) {
        // 已弃用方法，返回null
        return null;
    }

    @Override
    public int getRemotePort() {
        // 简单实现，根据实际情况调整
        return 0;
    }

    @Override
    public String getLocalName() {
        // 简单实现，根据实际情况调整
        return "localhost";
    }

    @Override
    public String getLocalAddr() {
        // 简单实现，根据实际情况调整
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        // 简单实现，根据实际情况调整
        return 80;
    }

    @Override
    public ServletContext getServletContext() {
        // 未实现ServletContext相关逻辑
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        // 未实现异步处理逻辑
        throw new IllegalStateException("Async not supported.");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        // 未实现异步处理逻辑
        throw new IllegalStateException("Async not supported.");
    }

    @Override
    public boolean isAsyncStarted() {
        // 未实现异步处理逻辑
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        // 未实现异步处理逻辑
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        // 未实现异步处理逻辑
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    //</editor-fold>
    //<editor-fold desc="其他方法">

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    /**
     * 回收对象，清理资源
     */
    public void recycle() {
        // 清空请求基本信息
        method = null;
        uri = null;
        protocol = null;
        contextPath = null;
        servletPath = null;
        pathInfo = null;
        // 清空集合
        attributes.clear();
        if (headers != null) {
            headers.clear();
        }
        if (parameters != null) {
            parameters.clear();
        }
        cookies.clear();
        // 重置流状态
        streamUsed = false;
        readerUsed = false;
        postParametersParsed = false;
        // 清空Reader（下次需要时重新创建）
        reader = null;
        // 重置会话信息
        requestedSessionId = null;
        isRequestedSessionIdFromCookie = false;
        isRequestedSessionIdFromURL = false;
        // 重置服务器信息
        remoteAddr = null;
        remoteHost = null;
        // 重置编码和区域设置
        characterEncoding = "UTF-8";
        locale = null;
        // 清空其他引用
        response = null;
        endpoint = null;
        socketInputBuffer = null;
    }

    public void finishRequest() throws IOException {
        if (socketInputBuffer != null) {
            if (this.requestStream != null) this.requestStream.close();
            if (this.reader != null) reader.close();
        } else {
            logger.warn("finishRequest失败，socket的输出流为null");
        }
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * 解析POST请求参数（懒加载）
     */
    private void parseParams() {
        // 如果已经解析过，直接返回
        if (postParametersParsed) {
            return;
        }

        // 只处理POST请求
        if (!"POST".equalsIgnoreCase(method)) {
            return;
        }
        
        // 检查流是否已被使用
        if (streamUsed || readerUsed) {
            return;
        }
        
        // 检查Content-Type
        String contentType = getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            return;
        }
        
        // 获取Content-Length
        int contentLength = getContentLength();
        if (contentLength <= 0) {
            return;  // 不支持 chunked 编码，直接跳过
        }
        
        try {
            // 读取请求体
            String queryString = getQueryString(contentLength);
            parseParametersInternal(queryString);
            
        } catch (IOException e) {
            logger.error("解析POST参数失败", e);
        }

        // 标记为已尝试解析
        postParametersParsed = true;
    }

    private String getQueryString(int contentLength) throws IOException {
        byte[] buffer = new byte[contentLength];
        ServletInputStream inputStream = getInputStream();
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int read = inputStream.read(buffer, bytesRead, contentLength - bytesRead);
            if (read == -1) {
                break;
            }
            bytesRead += read;
        }

        // 解析参数
        return new String(buffer, 0, bytesRead, characterEncoding);
    }

    /**
     * 内部方法：解析参数字符串（URL编码格式）
     * 复用 HttpProcessor 中的逻辑
     */
    private void parseParametersInternal(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] keyValue = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(keyValue[0], characterEncoding);
                String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], characterEncoding) : "";
                parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } catch (UnsupportedEncodingException e) {
                logger.error("解码参数失败: {}", pair, e);
            }
        }
    }
    //</editor-fold>
}
