package erangel.connector.http;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HttpResponse implements HttpServletResponse {

    // 状态码和消息
    private int status = SC_OK;
    private String statusMessage = "OK";
    // 状态行
    private StringBuilder statusLine;
    // 响应头部
    private Map<String, List<String>> headers = new HashMap<>();
    // 用于格式化 Cookie 的 StringBuilder
    private final StringBuilder cookieBuilder = new StringBuilder();
    // Cookie 列表
    private List<Cookie> cookies = new ArrayList<>();

    // 内容类型和字符编码
    private String contentType;
    private String characterEncoding = "ISO-8859-1";

    // 输出流和缓冲区
    // Servlet通过 ServletOutputStream 或 PrintWriter 向响应写入数据时，
    // 实际上是将数据写入到这个缓冲区ByteArrayOutputStream中。
    private final ByteArrayOutputStream outputStreamBuffer = new ByteArrayOutputStream();
    private final ServletOutputStream outputStream;
    private PrintWriter writer;

    // 是否已提交？
    private boolean isCommitted = false;
    // 缓冲区大小
    private int bufferSize = 8192;

    // 构造函数
    public HttpResponse() throws UnsupportedEncodingException {
        this.outputStream = new ServletOutputStream() {
            @Override
            //异步相关
            public boolean isReady() {
                return true;
            }

            @Override
            // 暂时为空
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                outputStreamBuffer.write(b);
            }
        };
        this.writer = new PrintWriter(new OutputStreamWriter(outputStreamBuffer, this.characterEncoding));
    }

    /**
     * 回收对象，清理资源
     */
    void recycle() {
        this.status = SC_OK;
        this.statusMessage = "OK";
        this.statusLine.setLength(0);
        this.statusLine.trimToSize();
        this.headers.clear();
        this.cookies.clear();
        this.contentType = null;
        this.characterEncoding = "ISO-8859-1";
        this.outputStreamBuffer.reset();
    }

    // 设置状态码
    @Override
    public void setStatus(int sc) {
        this.status = sc;
        this.statusMessage = getReasonPhrase(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        this.status = sc;
        this.statusMessage = sm;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    // 获取状态码对应的原因短语
    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case SC_OK -> "OK";
            case SC_NOT_FOUND -> "Not Found";
            case SC_INTERNAL_SERVER_ERROR -> "Internal Server Error";
            // 添加其他状态码
            default -> "";
        };
    }

    // 设置头部信息
    @Override
    public void setHeader(String name, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        headers.put(name, values);
    }

    @Override
    public void addHeader(String name, String value) {
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return (values != null && !values.isEmpty()) ? values.getFirst() : null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    // 设置内容类型
    @Override
    public void setContentType(String type) {
        this.contentType = type;
        setHeader("Content-Type", type);
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    // 设置字符编码
    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    // 获取输出流
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    // 获取 PrintWriter
    @Override
    public PrintWriter getWriter() throws IOException {
        return this.writer;
    }

    // 添加 Cookie
    @Override
    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    // 发送错误
    @Override
    public void sendError(int sc) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send error after response has been committed");
        }
        setStatus(sc);
        // 清空缓冲区
        outputStreamBuffer.reset();
        writer = new PrintWriter(new OutputStreamWriter(outputStreamBuffer, this.characterEncoding));

    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        sendError(sc);
        this.statusMessage = msg;
        writer.write(msg);
        writer.write("<html><head><title>Error</title></head><body>");
        writer.write("<h1>HTTP Error " + sc + " - " + msg + "</h1>");
        writer.write("</body></html>");
        writer.flush();
        flushBuffer();
    }

    // 发送重定向
    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send redirect after response has been committed");
        }
        setStatus(SC_FOUND);
        setHeader("Location", location);
        // 通常重定向不需要响应体，但可以根据需要添加
        flushBuffer();
    }

    // 缓冲区管理
    @Override
    public void flushBuffer() throws IOException {
        if (!isCommitted) {
            //TODO
            // 在这里可以将响应头部和状态行写入实际的输出流
            // 尚未涉及实际的网络传输，具体实现取决于服务器架构
            isCommitted = true;
            writeStatusLineAndHeaders();
        }
        writer.flush();
        outputStream.flush();
    }

    // 响应行和头部写入缓冲区
    private void writeStatusLineAndHeaders() throws IOException {
        statusLine.append("HTTP/1.1 ").append(status).append(" ").append(statusMessage).append("\r\n");
        headers.forEach((name, values) -> {
            values.forEach(value -> statusLine.append(name).append(": ").append(value).append("\r\n"));
        });
        for (Cookie cookie : cookies) {
            statusLine.append("Set-Cookie: ").append(formatCookie(cookie)).append("\r\n");
        }
        statusLine.append("\r\n");
        outputStreamBuffer.write(statusLine.toString().getBytes(characterEncoding));
    }

    // 格式化 Cookie
    private String formatCookie(Cookie cookie) {
        cookieBuilder.append(cookie.getName()).append("=").append(cookie.getValue());
        if (cookie.getMaxAge() >= 0) cookieBuilder.append("; Max-Age=").append(cookie.getMaxAge());
        if (cookie.getPath() != null) cookieBuilder.append("; Path=").append(cookie.getPath());
        if (cookie.getDomain() != null) cookieBuilder.append("; Domain=").append(cookie.getDomain());
        if (cookie.getSecure()) cookieBuilder.append("; Secure");
        if (cookie.isHttpOnly()) cookieBuilder.append("; HttpOnly");
        return cookieBuilder.toString();
    }

    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    @Override
    public void setBufferSize(int size) {
        if (isCommitted) {
            throw new IllegalStateException("Cannot set buffer size after response has been committed");
        }
        this.bufferSize = size;
    }

    @Override
    public boolean isCommitted() {
        return this.isCommitted;
    }

    @Override
    public void resetBuffer() {
        if (isCommitted) {
            throw new IllegalStateException("Cannot reset buffer after response has been committed");
        }
        outputStreamBuffer.reset();
    }

    @Override
    public void reset() {
        if (isCommitted) {
            throw new IllegalStateException("Cannot reset after response has been committed");
        }
        status = SC_OK;
        statusMessage = "OK";
        headers.clear();
        cookies.clear();
        contentType = null;
        characterEncoding = "ISO-8859-1";
        outputStreamBuffer.reset();
    }

    // 设置内容长度
    @Override
    public void setContentLength(int len) {
        setIntHeader("Content-Length", len);
    }

    @Override
    public void setContentLengthLong(long len) {
        setHeader("Content-Length", Long.toString(len));
    }

    // 设置日期头部
    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, formatDate(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, formatDate(date));
    }

    // 设置整数头部
    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    // 日期格式化
    private String formatDate(long date) {
        // 格式化为 HTTP 日期格式，例如：Sun, 06 Nov 1994 08:49:37 GMT
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(date));
    }

    // 设置是否使用字符编码
    @Override
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    /**
     *
     */
    public byte[] getResponseData() throws IOException {
        flushBuffer(); // 确保所有数据都已写入缓冲区
        return outputStreamBuffer.toByteArray();
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public Map<String, List<String>> getHeadersMap() {
        return this.headers;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }


    // HttpServletResponse 接口中被弃用的方法
    @Deprecated
    @Override
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Deprecated
    @Override
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

    // 实现 URL 编码方法
    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }
}
