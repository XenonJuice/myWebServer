package erangel.connector.http;

import erangel.connector.http.Const.Header;
import erangel.connector.http.Const.PunctuationMarks;
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

    //<editor-fold desc="attr">
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    private HttpConnector connector;
    private final int bufferSize = 8192;
    private int status = SC_OK;
    private String statusMessage = "OK";
    private String contentType;
    private long contentLength = -1;
    private String characterEncoding = "UTF-8";
    private HttpRequest request;
    private OutputStream clientOutputStream; // 最终输出到客户端的流
    private HttpResponseStream servletOutputStream; // 自定义流
    private PrintWriter writer;
    private boolean isCommitted = false;
    private boolean writerUsed = false;
    private boolean outputStreamUsed = false;
    private boolean allowChunking = false;

    /**
     * 用指定的字符编码创建/重置 PrintWriter
     */
    private void createWriter(String encoding) throws UnsupportedEncodingException {
        HttpResponseStream newStream = (HttpResponseStream) createOutputStream();
        this.writer = new PrintWriter(
                new OutputStreamWriter(newStream, encoding));
        this.servletOutputStream = newStream;
    }

    @Override
    public void setStatus(int sc, String sm) {
        this.status = sc;
        this.statusMessage = (sm != null ? sm : getReasonPhrase(sc));
    }
    //</editor-fold>
    //<editor-fold desc="状态码相关">
    // =================== 状态码相关 ===================

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
        this.statusMessage = getReasonPhrase(sc);
    }

    /**
     * 获取状态码对应的原因短语，若未知则返回 "Unknown Status"
     */
    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            // 100
            case SC_CONTINUE -> "Continue";
            // 200
            case SC_OK -> "OK";
            // 404
            case SC_NOT_FOUND -> "Not Found";
            // 500
            case SC_INTERNAL_SERVER_ERROR -> "Internal Server Error";
            // 400
            case SC_BAD_REQUEST -> "Bad Request";
            // 302
            case SC_FOUND -> "Found";
            // 408
            case SC_REQUEST_TIMEOUT -> "TimeOut";

            default -> "Unknown Status";
        };
    }


    //</editor-fold>
    //<editor-fold desc="响应头相关">

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

    /**
     * 从 headers 中移除整个 key（以及它对应的所有值）。
     *
     * @param key 要移除的 key
     */
    public void removeHeader(String key) {
        headers.remove(key);
    }

    /**
     * 从 headers 中移除指定 key 的某个 value。
     * 如果移除该 value 后对应的 List 为空，则移除整个 key。
     *
     * @param key   要操作的 key
     * @param value 要移除的 value
     */
    public void removeHeader(String key, String value) {
        if (headers.containsKey(key)) {
            List<String> values = headers.get(key);
            values.remove(value); // 尝试移除该值
            if (values.isEmpty()) { // 如果对应的值列表为空，则移除整个 key
                headers.remove(key);
            }
        }
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

    @Override
    public String getContentType() {
        return this.contentType;
    }

    //</editor-fold>
    //<editor-fold desc="内容类型与编码">

    @Override
    public void setContentType(String type) {
        // 1. 如果已经提交响应，就按照 Servlet 规范的通常做法：禁止再修改内容类型
        if (isCommitted) {
            throw new IllegalStateException("Response has already been committed; cannot change content type.");
        }
        // 2. 如果传入 null，就相当于去掉 Content-Type 头
        if (type == null) {
            this.contentType = null;
            setHeader(Header.CONTENT_TYPE, PunctuationMarks.EMPTY);
            return;
        }

        // 3. 设置 contentType 到本地字段，并更新到头部
        this.contentType = type;
        setHeader(Header.CONTENT_TYPE, type);

        // 4. 自动解析 charset=
        //    例如 "text/html; charset=UTF-8; boundary=xxx"
        //    先转小写方便找 "charset="
        String lowerType = type.toLowerCase(Locale.ROOT);
        int idx = lowerType.indexOf("charset=");
        if (idx != -1) {
            // 4.1 charsetPart 拿到 "UTF-8; boundary=xxx" 这样的子串
            String charsetPart = type.substring(idx + 8).trim();

            // 如果还有其他属性，比如 `; boundary=someBoundary`，就需要截掉
            int semicolonIndex = charsetPart.indexOf(PunctuationMarks.SEMICOLON);
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
    public String getCharacterEncoding() {
        return this.characterEncoding;
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
    public ServletOutputStream getOutputStream() {
        if (writerUsed) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }
        outputStreamUsed = true;
        if (this.servletOutputStream == null) {
            this.servletOutputStream = (HttpResponseStream) createOutputStream();
        }
        return this.servletOutputStream;
    }

    //</editor-fold>
    //<editor-fold desc="输出流/Writer 获取">

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStreamUsed) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }
        writerUsed = true;
        if (writer != null) return this.writer;
        createWriter(this.characterEncoding);
        return this.writer;
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (isCommitted) {
            throw new IllegalStateException("Cannot add cookie after response has been committed.");
        }
        if (cookie != null) {
            this.cookies.add(cookie);
        }
    }

    //</editor-fold>
    //<editor-fold desc = "发送错误与重定向">
    // 确认消息
    public void sendAck() throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send error after response has been committed.");
        }
        setStatus(100, null);
    }

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
        String errorPage = "<html><head><title>Error</title></head><body>"
                + "<h1>HTTP Error " + sc + " - " + msg + "</h1>"
                + "</body></html>";
        byte[] errorBytes = errorPage.getBytes(getCharacterEncoding());
        setContentType("text/html; charset=UTF-8");
        setContentLength(errorBytes.length);
        getWriter().write(errorPage);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (isCommitted) {
            throw new IllegalStateException("Cannot send redirect after response has been committed.");
        }
        setStatus(SC_FOUND, getReasonPhrase(SC_FOUND));
        setHeader(Header.LOCATION, location);
        // 清空已有的响应体
        resetBuffer();
        String redirectPage = "<html><head><title>Redirect</title></head><body>"
                + "<h1>Redirecting to <a href=\"" + location + "\">" + location + "</a></h1>"
                + "</body></html>";
        byte[] reBytes = redirectPage.getBytes(getCharacterEncoding());
        setContentLength(reBytes.length);
        setContentType("text/html; charset=UTF-8");
        getWriter().write(redirectPage);
    }

    @Override
    public void flushBuffer() throws IOException {
        writeStatusLineAndHeaders();
        if (writerUsed) writer.flush();
        if (outputStreamUsed) servletOutputStream.flush();

    }
    //</editor-fold>
    //<editor-fold desc="提交/刷新缓冲">

    /**
     * 写出状态行、响应头和 Cookie；必须保证先于响应正文
     */
    protected void writeStatusLineAndHeaders() throws IOException {
        if (isCommitted) return;
        StringBuilder sb = new StringBuilder();
        sb.append(request.getProtocol()).append(PunctuationMarks.SPACE)
                .append(status)
                .append(PunctuationMarks.SPACE)
                .append(statusMessage)
                .append(PunctuationMarks.CRLF);
        // 常用的 Date 和 Server
        // sb.append(Header.DATE + PunctuationMarks.COLON_SPACE).append(formatDate(System.currentTimeMillis())).append(PunctuationMarks.CRLF);
        // TODO 改为固定时间便与测试
        sb.append(Header.DATE + PunctuationMarks.COLON_SPACE).append("Fri, 17 Jan 2025 07:06:03 GMT").append(PunctuationMarks.CRLF);
        sb.append(Header.SERVER + PunctuationMarks.COLON_SPACE + "CustomJavaServer" + PunctuationMarks.CRLF);
        // 其他头部
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            for (String val : entry.getValue()) {
                sb.append(name).append(PunctuationMarks.COLON_SPACE).append(val).append(PunctuationMarks.CRLF);
            }
        }

        // 写入 Cookie
        for (Cookie cookie : cookies) {
            sb.append(Header.SET_COOKIE + PunctuationMarks.COLON_SPACE).append(formatCookie(cookie)).append(PunctuationMarks.CRLF);
        }
        // 空行，结束头部
        sb.append(PunctuationMarks.CRLF);
        // 单独声明一个writer防止与响应体正文用的writer冲突
        OutputStreamWriter out;
        try {
            out = new OutputStreamWriter(getStream(), getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            out = new OutputStreamWriter(getStream());
        }
        PrintWriter outputWriter = new PrintWriter(out);
        outputWriter.write(sb.toString());
        outputWriter.flush();
        isCommitted = true;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    //</editor-fold>
    //<editor-fold desc="缓冲区相关">

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
        this.servletOutputStream = new HttpResponseStream(this);
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
        this.servletOutputStream = new HttpResponseStream(this);
        headers.clear();
        cookies.clear();
        contentType = null;
        contentLength = -1;
        characterEncoding = "UTF-8";
        writerUsed = false;
        outputStreamUsed = false;
        try {
            createWriter(this.characterEncoding);
        } catch (UnsupportedEncodingException e) {
            // ignore
        }

    }

    public long getContentLength() {
        return contentLength;
    }

    //</editor-fold>
    //<editor-fold desc="其他头部设置">

    @Override
    public void setContentLength(int len) {
        setIntHeader(Header.CONTENT_LENGTH, len);
        this.contentLength = Long.parseLong(Integer.toString(len));
    }

    @Override
    public void setContentLengthLong(long len) {
        setHeader(Header.CONTENT_LENGTH, Long.toString(len));
        this.contentLength = Long.parseLong(Long.toString(len));
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

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }
    //</editor-fold>
    //<editor-fold desc="Locale 相关（示例）">
    // =================== Locale 相关（示例） ===================

    @Override
    public void setLocale(Locale loc) {

    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    //</editor-fold>
    //<editor-fold desc="仅供调试/测试">

    public Map<String, List<String>> getHeadersMap() {
        return this.headers;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    @Deprecated
    @Override
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    //</editor-fold>
    //<editor-fold desc="URL 编码相关（简单实现）">

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

    //</editor-fold>
    //<editor-fold desc="其他方法">

    /**
     * 回收对象，清理资源
     */
    public void recycle() {
        this.status = SC_OK;
        this.statusMessage = "OK";
        this.headers.clear();
        this.cookies.clear();
        this.contentType = null;
        this.characterEncoding = "UTF-8";
        this.isCommitted = false;
        this.writerUsed = false;
        this.outputStreamUsed = false;
        this.allowChunking = false;
        this.request = null;
        resetBuffer();
    }

    public HttpConnector getConnector() {
        return connector;
    }

    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public void finishResponse() throws IOException {
        if (getStatus() < SC_BAD_REQUEST) {
            if ((getContentLength() == -1)
                    && getStatus() >= 200
                    && getStatus() != SC_NO_CONTENT
                    && getStatus() != SC_NOT_MODIFIED) {
                setContentLength(0);
            }
        } else {
            setHeader(Header.CONNECTION, Header.CLOSE);
        }

        if (!isCommitted && clientOutputStream == null
                && writer == null && getStatus() >= SC_BAD_REQUEST
                && contentType == null && !(servletOutputStream.getByteCount() > 0)) {
            setContentType("text/html; charset=UTF-8");
            String errorPage = "<html><head><title>Error</title></head><body>"
                    + "<h1>HTTP Error " + " - " + getStatusMessage() + "</h1>"
                    + "</body></html>";
            byte[] errorBytes = errorPage.getBytes(getCharacterEncoding());
            setContentLength(errorBytes.length);
            resetBuffer();
            getWriter().write(errorPage);
        }
        flushBuffer();
        if (servletOutputStream != null) servletOutputStream.close();
        if (writer != null) writer.close();
    }


    //</editor-fold>
    //<editor-fold desc="输出流相关">
    public OutputStream getStream() {
        return clientOutputStream;
    }

    public void setStream(OutputStream outputStream) {
        this.clientOutputStream = outputStream;
    }

    public ServletOutputStream createOutputStream() {
        return new HttpResponseStream(this);
    }

    //</editor-fold>
    //<editor-fold desc="chunk?">
    public boolean isAllowChunking() {
        return allowChunking;
    }

    public void setAllowChunking(boolean allowChunking) {
        this.allowChunking = allowChunking;
    }

    public boolean isCloseConnection() {
        String connectionValue = getHeader(Header.CONNECTION);
        return (connectionValue != null && connectionValue.equals(Header.CLOSE));
    }
    //</editor-fold>
}
