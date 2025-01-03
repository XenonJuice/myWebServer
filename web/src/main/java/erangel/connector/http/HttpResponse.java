package erangel.connector.http;

import erangel.log.BaseLogger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static erangel.connector.Utils.CookieUtils.formatCookie;

/**
 * 自定义的 HttpServletResponse 实现，用于处理 HTTP 响应。
 * 使用自定义的 HttpResponseStream (基于 BufferedOutputStream) 作为输出流。
 */
public class HttpResponse extends BaseLogger implements HttpServletResponse {

    // ======== 状态码和消息 ========
    private int status = SC_OK;
    private String statusMessage = "OK";

    // ======== 响应头部、Cookie ========
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();

    // ======== 内容类型和字符编码 ========
    private String contentType;
    private String characterEncoding = "ISO-8859-1";

    // ======== 缓冲区相关 ========
    private final int bufferSize = 8192;
    private OutputStream clientOutputStream; // 最终输出到客户端的流
    private BufferedOutputStream bufferedOutputStream; // 直接缓冲到clientOutputStream
    private ServletOutputStream servletOutputStream; // 自定义流
    private PrintWriter writer;
    // ======== 提交状态 & 标志位 ========
    private boolean isCommitted = false;
    private boolean writerUsed = false;
    private boolean outputStreamUsed = false;

    /**
     *
     */
    public HttpResponse() {

    }

    // =================== 输出流相关 ===================
    public void setStream(OutputStream outputStream) {
        this.clientOutputStream = outputStream;
        this.bufferedOutputStream = new BufferedOutputStream(clientOutputStream, bufferSize);
        this.servletOutputStream = new HttpResponseStream(this.bufferedOutputStream);
    }

    /**
     * 用指定的字符编码创建/重置 PrintWriter
     */
    private void createWriter(String encoding) throws UnsupportedEncodingException {
        this.writer = new PrintWriter(
                new OutputStreamWriter(this.bufferedOutputStream, encoding));
    }

    /**
     * 回收对象，清理资源
     */
    public void recycle() {
        this.status = SC_OK;
        this.statusMessage = "OK";
        this.headers.clear();
        this.cookies.clear();
        this.contentType = null;
        this.characterEncoding = "ISO-8859-1";
        this.isCommitted = false;
        this.writerUsed = false;
        this.outputStreamUsed = false;
        resetBuffer();
    }

    // =================== 状态码相关 ===================

    @Override
    public void setStatus(int sc) {
        this.status = sc;
        this.statusMessage = getReasonPhrase(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        this.status = sc;
        this.statusMessage = (sm != null ? sm : getReasonPhrase(sc));
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    /**
     * 获取状态码对应的原因短语，若未知则返回 "Unknown Status"
     */
    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case SC_OK -> "OK";
            // 404
            case SC_NOT_FOUND -> "Not Found";
            // 500
            case SC_INTERNAL_SERVER_ERROR -> "Internal Server Error";
            // 400
            case SC_BAD_REQUEST -> "Bad Request";
            // 302
            case SC_FOUND -> "Found";
            default -> "Unknown Status";
        };
    }

    // =================== 响应头相关 ===================

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
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    // =================== 内容类型与编码 ===================

    @Override
    public void setContentType(String type) {
        // 1. 如果已经提交响应，就按照 Servlet 规范的通常做法：禁止再修改内容类型
        if (isCommitted) {
            throw new IllegalStateException("Response has already been committed; cannot change content type.");
        }
        // 2. 如果传入 null，就相当于去掉 Content-Type 头
        if (type == null) {
            this.contentType = null;
            setHeader("Content-Type", "");
            return;
        }

        // 3. 设置 contentType 到本地字段，并更新到头部
        this.contentType = type;
        setHeader("Content-Type", type);

        // 4. 自动解析 charset=
        //    例如 "text/html; charset=UTF-8; boundary=xxx"
        //    先转小写方便找 "charset="
        String lowerType = type.toLowerCase(Locale.ROOT);
        int idx = lowerType.indexOf("charset=");
        if (idx != -1) {
            // 4.1 charsetPart 拿到 "UTF-8; boundary=xxx" 这样的子串
            String charsetPart = type.substring(idx + 8).trim();

            // 如果还有其他属性，比如 `; boundary=someBoundary`，就需要截掉
            int semicolonIndex = charsetPart.indexOf(';');
            if (semicolonIndex != -1) {
                charsetPart = charsetPart.substring(0, semicolonIndex).trim();
            }

            // 4.2 如果不为空，就更新 characterEncoding
            if (!charsetPart.isEmpty()) {
                this.characterEncoding = charsetPart;
                try {
                    // 4.3 如果此前已经产生过 PrintWriter，需要重建，以应用新的编码
                    createWriter(this.characterEncoding);
                } catch (UnsupportedEncodingException e) {
                    logger.error("Unsupported encoding: {}", charsetPart);
                }
            }
        }
    }


    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if (isCommitted) {
            // 一旦提交，不可改编码（符合 Servlet 规范）
            return;
        }
        this.characterEncoding = charset;
        try {
            createWriter(this.characterEncoding);
        } catch (UnsupportedEncodingException e) {
            // ignore or fallback
        }
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    // =================== 输出流/Writer 获取 ===================

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writerUsed) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }
        outputStreamUsed = true;
        return this.servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStreamUsed) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }
        writerUsed = true;
        return this.writer;
    }

    // =================== Cookie 相关 ===================

    @Override
    public void addCookie(Cookie cookie) {
        if (isCommitted) {
            throw new IllegalStateException("Cannot add cookie after response has been committed.");
        }
        if (cookie != null) {
            this.cookies.add(cookie);
        }
    }

    // =================== 发送错误与重定向 ===================

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, getReasonPhrase(sc));
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send error after response has been committed.");
        }
        setStatus(sc, msg);
        // 清空已有的响应体
        resetBuffer();
        setContentType("text/html; charset=UTF-8");

        String errorPage = "<html><head><title>Error</title></head><body>"
                + "<h1>HTTP Error " + sc + " - " + msg + "</h1>"
                + "</body></html>";
        getWriter().write(errorPage);
        flushBuffer(); // 提交
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send redirect after response has been committed.");
        }
        setStatus(SC_FOUND, getReasonPhrase(SC_FOUND));
        setHeader("Location", location);
        // 清空已有的响应体
        resetBuffer();
        setContentType("text/html; charset=UTF-8");

        String redirectPage = "<html><head><title>Redirect</title></head><body>"
                + "<h1>Redirecting to <a href=\"" + location + "\">" + location + "</a></h1>"
                + "</body></html>";
        getWriter().write(redirectPage);
        flushBuffer(); // 提交
    }

    // =================== 提交/刷新缓冲 ===================

    @Override
    public void flushBuffer() throws IOException {
        if (isCommitted) {
            return;
        }
        // 1) 先把状态行和响应头发送到客户端
        writeStatusLineAndHeaders();

        // 2) 再将正文（内存缓冲）刷新到客户端
        //    先flush Writer -> flush BufferedOutputStream
        writer.flush();
        servletOutputStream.flush();
        clientOutputStream.flush();
        isCommitted = true;
    }

    /**
     * 写出状态行、响应头和 Cookie；必须保证先于响应正文
     */
    private void writeStatusLineAndHeaders() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ")
                .append(status)
                .append(" ")
                .append(statusMessage)
                .append("\r\n");

        // Content-Type
        if (this.contentType != null) {
            sb.append("Content-Type: ").append(this.contentType).append("\r\n");
        }

        // 其他头部
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            for (String val : entry.getValue()) {
                sb.append(name).append(": ").append(val).append("\r\n");
            }
        }

        // 写入 Cookie
        for (Cookie cookie : cookies) {
            sb.append("Set-Cookie: ").append(formatCookie(cookie)).append("\r\n");
        }

        // 常用的 Date 和 Server
        sb.append("Date: ").append(formatDate(System.currentTimeMillis())).append("\r\n");
        sb.append("Server: CustomJavaServer/1.0\r\n");

        // 空行，结束头部
        sb.append("\r\n");

        servletOutputStream.write(sb.toString().getBytes(this.characterEncoding));
        servletOutputStream.flush();
    }

    // =================== 缓冲区相关 ===================

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void setBufferSize(int size) {
        if (isCommitted) {
            throw new IllegalStateException("Cannot set buffer size after response has been committed.");
        }
        // 不作实现
    }

    @Override
    public boolean isCommitted() {
        return this.isCommitted;
    }

    /**
     * resetBuffer() 应该清空尚未提交的内容，
     */
    @Override
    public void resetBuffer() {
        if (isCommitted) {
            throw new IllegalStateException("Cannot reset buffer after response has been committed.");
        }
        try {
            // 必须重建 writer，否则已经写过的数据无法被重置
            createWriter(this.characterEncoding);
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
    }

    /**
     * reset() 在尚未提交的情况下，清空状态行、头部、cookies，以及响应内容
     */
    @Override
    public void reset() {
        if (isCommitted) {
            throw new IllegalStateException("Cannot reset after response has been committed.");
        }
        status = SC_OK;
        statusMessage = "OK";
        headers.clear();
        cookies.clear();
        contentType = null;
        characterEncoding = "ISO-8859-1";
        writerUsed = false;
        outputStreamUsed = false;
        // internalBuffer.reset();
        try {
            createWriter(this.characterEncoding);
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
    }

    // =================== 其他头部设置 ===================

    @Override
    public void setContentLength(int len) {
        setIntHeader("Content-Length", len);
    }

    @Override
    public void setContentLengthLong(long len) {
        setHeader("Content-Length", Long.toString(len));
    }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, formatDate(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, formatDate(date));
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    private String formatDate(long date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US
        );
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date(date));
    }

    // =================== Locale 相关（示例） ===================

    @Override
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    // =================== 仅供调试/测试 ===================

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public Map<String, List<String>> getHeadersMap() {
        return this.headers;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    // =================== URL 编码相关（简单实现） ===================

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

    @Override
    public String encodeURL(String url) {
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }
}
