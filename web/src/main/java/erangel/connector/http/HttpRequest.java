package erangel.connector.http;

import java.io.*;
import java.security.Principal;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class HttpRequest implements HttpServletRequest {
    // HTTP请求方法（例如：GET, POST）
    private String method;
    // 请求URI（例如：/index.html）
    private String uri;
    // 协议版本（例如：HTTP/1.1）
    private String protocol;
    // 存储HTTP头的映射
    private final Map<String, List<String>> headers = new HashMap<>();
    // 存储请求参数的映射
    private final Map<String, String> parameters = new HashMap<>();
    // 请求体内容
    private String body = null;
    // 用于读取HTTP请求的输入流
    private InputStream inputStream;

    public HttpRequest(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        parseRequest();
    }

    /**
     * 解析HTTP请求
     */
    private void parseRequest() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        if (line == null || line.isEmpty()) {
            throw new IOException("Empty request");
        }

        // 解析请求行（例如：GET /index.html HTTP/1.1）
        String[] requestLine = line.split(" ");
        if (requestLine.length != 3) {
            throw new IOException("invalid request line: " + line);
        }
        method = requestLine[0];
        uri = requestLine[1];
        protocol = requestLine[2];

        // 如果 URI 包含查询字符串，解析参数
        if (uri.contains("?")) {
            String[] uriParts = uri.split("\\?", 2);
            uri = uriParts[0];
            parseParameters(uriParts[1]);
        }

        // 解析请求头
        int contentLength = 0;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int separatorIndex = line.indexOf(": ");
            if (separatorIndex == -1) {
                throw new IOException("Malformed header: " + line);
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 2).trim();
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);

            if ("Content-Length".equalsIgnoreCase(key)) {
                contentLength = Integer.parseInt(value);
            }
        }

        // 解析请求体
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            if (reader.read(bodyChars) != contentLength) {
                throw new IOException("Request body is incomplete");
            }
            body = new String(bodyChars);
        }
    }

    /**
     * 解析查询字符串参数
     */
    private void parseParameters(String queryString) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                parameters.put(keyValue[0], keyValue[1]);
            } else {
                parameters.put(keyValue[0], "");
            }
        }
    }

    /**
     * 回收对象，清理资源
     */
    public void recycle() {
        method = null;
        uri = null;
        protocol = null;
        headers.clear();
        parameters.clear();
        body = null;
        inputStream = null;
    }


    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return "";
    }

    @Override
    public String getPathTranslated() {
        return "";
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getRequestURI() {
        return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getScheme() {
        return "";
    }

    @Override
    public String getServerName() {
        return "";
    }

    @Override
    public int getServerPort() {
        return 0;
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

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Map.of();
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Integer.parseInt(contentLength) : -1;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getQueryString() {
        return parameters.isEmpty() ? null : parameters.toString();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body != null ? body.getBytes() : new byte[0])));
    }

    // ----------------------------- 其余方法未实现，可按需补充 -----------------------------

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public int getIntHeader(String name) {
        return 0;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return "";
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return "";
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String name, Object o) {

    }

    @Override
    public void removeAttribute(String name) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return "";
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }
}
