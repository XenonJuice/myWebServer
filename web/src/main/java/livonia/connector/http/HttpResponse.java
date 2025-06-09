package livonia.connector.http;

import livonia.base.Const;
import livonia.base.Context;
import livonia.log.BaseLogger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static livonia.base.Const.Header;
import static livonia.base.Const.PunctuationMarks.COLON_SPACE;
import static livonia.base.Const.PunctuationMarks.CRLF;
import static livonia.utils.CookieUtils.formatCookie;

/**
 * 自定义的 HttpServletResponse 实现，用于处理 HTTP 响应
 *
 * @author LILINJIAN
 * @version 2025/06/04
 */
public class HttpResponse extends BaseLogger implements HttpServletResponse {

    // 日期格式化器（线程安全的单例）
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf;
    });
    //<editor-fold desc="属性">
    // 响应头和Cookie
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final List<Cookie> cookies = new ArrayList<>();
    // 连接器和上下文
    private HttpConnector connector;
    private Context context;
    private HttpRequest request;
    // 状态信息
    private int status = SC_OK;
    private String statusMessage = "OK";
    private boolean error = false;
    // 内容相关
    private String contentType;
    private long contentLength = -1;
    private String characterEncoding = "UTF-8";
    // 流相关 - 改进的流管理
    private OutputStream clientOutputStream;
    private HttpResponseStream responseStream;
    private PrintWriter writer;
    // 状态标志
    private boolean committed = false;
    private boolean writerUsed = false;
    private boolean outputStreamUsed = false;
    private boolean suspended = false;
    // Chunking相关
    private boolean allowChunking = false;
    // 缓冲区大小
    private int bufferSize = 8192;
    //</editor-fold>

    //<editor-fold desc="构造器">
    public HttpResponse() {
        // 默认构造器
    }
    //</editor-fold>

    //<editor-fold desc="流管理方法">

    /**
     * 获取底层输出流
     */
    public OutputStream getStream() {
        return clientOutputStream;
    }

    /**
     * 设置底层输出流（通常是Socket的输出流）
     */
    public void setStream(OutputStream outputStream) {
        this.clientOutputStream = outputStream;
    }

    /**
     * 设置响应流（由HttpProcessor调用）
     */
    public void setResponseStream(HttpResponseStream stream) {
        this.responseStream = stream;
    }

    /**
     * 获取ServletOutputStream
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writerUsed) {
            throw new IllegalStateException("getWriter() has already been called");
        }

        outputStreamUsed = true;

        if (responseStream == null) {
            responseStream = createOutputStream();
        }

        return responseStream;
    }

    /**
     * 创建新的输出流（仅在必要时调用）
     */
    protected HttpResponseStream createOutputStream() {
        return new HttpResponseStream(this, bufferSize);
    }

    /**
     * 发送响应头
     */
    public void sendHeaders() throws IOException {
        if (committed) {
            return;
        }

        // 准备状态行和响应头
        StringBuilder sb = new StringBuilder(256);

        // 状态行
        sb.append(request.getProtocol()).append(' ')
                .append(status).append(' ')
                .append(statusMessage)
                .append(CRLF);

        // 日期头
        sb.append(Header.DATE).append(COLON_SPACE)
                .append(formatDate(System.currentTimeMillis()))
                .append(CRLF);

        // 服务器头
        sb.append(Header.SERVER).append(COLON_SPACE)
                .append("Livonia/1.0")
                .append(CRLF);

        // 其他响应头
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            for (String value : entry.getValue()) {
                sb.append(name).append(COLON_SPACE)
                        .append(value).append(CRLF);
            }
        }

        // Cookie头
        for (Cookie cookie : cookies) {
            sb.append(Header.SET_COOKIE).append(COLON_SPACE)
                    .append(formatCookie(cookie))
                    .append(CRLF);
        }

        // 空行结束头部
        sb.append(CRLF);

        // 写入头部
        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        clientOutputStream.write(headerBytes);
        clientOutputStream.flush();

        committed = true;
    }
    //</editor-fold>

    //<editor-fold desc="响应完成和回收">

    /**
     * 完成响应
     */
    public void finishResponse() throws IOException {
        // 如果还没有发送响应头，现在发送
        if (!committed) {
            // 先刷新Writer确保所有数据都写入到底层流
            if (writer != null) {
                writer.flush();
            }

            // 设置默认的Content-Length
            if (contentLength == -1 && !isChunking()) {
                if (responseStream != null) {
                    // 先刷新流，确保所有数据都写入
                    responseStream.flush();
                    // 计算写入的长度
                    contentLength = responseStream.getTotalBytesWritten();
                } else {
                    contentLength = 0;
                }
                setContentLengthLong(contentLength);
                logger.warn("自动计算Content-Length: {} bytes (totalWritten={}, buffered={})",
                        contentLength,
                        responseStream.getTotalBytesWritten(),
                        responseStream.getBufferedDataSize());
            }

            sendHeaders();
        }

        // 刷新并关闭流
        if (writer != null) {
            writer.flush();
            writer.close();
        } else if (responseStream != null) {
            responseStream.flush();
            responseStream.close();
        }

        // 确保底层流被刷新
        if (clientOutputStream != null) {
            clientOutputStream.flush();
        }
    }

    /**
     * 回收对象，清理资源（为下一个响应做准备）
     */
    public void recycle() {
        // 重置状态
        status = SC_OK;
        statusMessage = "OK";
        error = false;

        // 清空集合
        headers.clear();
        cookies.clear();

        // 重置内容相关
        contentType = null;
        contentLength = -1;
        characterEncoding = "UTF-8";

        // 重置流状态
        committed = false;
        writerUsed = false;
        outputStreamUsed = false;
        suspended = false;

        // 清空Writer（下次需要时重新创建）
        writer = null;

        // 重置其他
        allowChunking = false;
        request = null;

        // 不要清空connector和clientOutputStream，它们可能被复用
    }
    //</editor-fold>

    //<editor-fold desc="状态码相关">
    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(int sc) {
        if (committed) {
            return;
        }
        this.status = sc;
        this.statusMessage = getReasonPhrase(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        if (committed) {
            return;
        }
        this.status = sc;
        this.statusMessage = (sm != null ? sm : getReasonPhrase(sc));
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public boolean isError() {
        return error;
    }

    public void setError() {
        error = true;
    }

    /**
     * 获取状态码对应的原因短语
     */
    private String getReasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 100 -> "Continue";
            case 101 -> "Switching Protocols";
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 206 -> "Partial Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 303 -> "See Other";
            case 304 -> "Not Modified";
            case 307 -> "Temporary Redirect";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 411 -> "Length Required";
            case 412 -> "Precondition Failed";
            case 413 -> "Request Entity Too Large";
            case 414 -> "Request-URI Too Long";
            case 415 -> "Unsupported Media Type";
            case 416 -> "Requested Range Not Satisfiable";
            case 417 -> "Expectation Failed";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            case 505 -> "HTTP Version Not Supported";
            default -> "Unknown";
        };
    }
    //</editor-fold>

    //<editor-fold desc="响应头相关">
    @Override
    public void setHeader(String name, String value) {
        if (committed) {
            return;
        }

        // 直接设置头部
        List<String> values = new ArrayList<>();
        values.add(value);
        headers.put(name, values);

        // 根据头部名称进行处理
        String header = name.toLowerCase();
        if (header.equals(Const.Header.CONTENT_LENGTH.toLowerCase())) {
            long contentLength = -1;
            try {
                contentLength = Long.parseLong(value);
            } catch (NumberFormatException e) {
                //
            }
            if (contentLength >= 0) {
                this.contentLength = contentLength;
            }
        } else if (header.equals(Header.CONTENT_TYPE.toLowerCase())) {
            setContentType(value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (committed) {
            return;
        }

        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        String nameLowerCase = name.toLowerCase();
        if (Header.CONTENT_TYPE.equalsIgnoreCase(nameLowerCase) && contentType == null) {
            setContentType(value);
        }
    }

    public void removeHeader(String name) {
        if (committed) {
            return;
        }
        headers.remove(name);
    }

    public void removeHeader(String name, String value) {
        if (committed) {
            return;
        }

        List<String> values = headers.get(name);
        if (values != null) {
            values.remove(value);
            if (values.isEmpty()) {
                headers.remove(name);
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
        return DATE_FORMAT.get().format(new Date(date));
    }
    //</editor-fold>

    //<editor-fold desc="内容相关">
    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public void setContentType(String type) {
        if (committed) {
            return;
        }

        if (type == null) {
            this.contentType = null;
            headers.remove(Header.CONTENT_TYPE);
            return;
        }

        this.contentType = type;

        // 解析 charset
        String lowerType = type.toLowerCase(Locale.ROOT);
        int charsetIndex = lowerType.indexOf("charset=");
        if (charsetIndex >= 0) {
            String charsetPart = type.substring(charsetIndex + 8).trim();
            int semicolonIndex = charsetPart.indexOf(';');
            if (semicolonIndex >= 0) {
                charsetPart = charsetPart.substring(0, semicolonIndex).trim();
            }
            // 去除引号
            if (charsetPart.length() > 1 &&
                    charsetPart.startsWith("\"") && charsetPart.endsWith("\"")) {
                charsetPart = charsetPart.substring(1, charsetPart.length() - 1);
            }
            if (!charsetPart.isEmpty()) {
                this.characterEncoding = charsetPart;
            }
        }

        List<String> values = new ArrayList<>();
        values.add(type);
        headers.put(Header.CONTENT_TYPE, values);
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if (committed || writerUsed) {
            return;
        }
        this.characterEncoding = charset;

        // 更新Content-Type头部
        if (contentType != null && !contentType.contains("charset=")) {
            setContentType(contentType + "; charset=" + charset);
        }
    }

    public long getContentLength() {
        return contentLength;
    }

    @Override
    public void setContentLength(int len) {
        if (committed) {
            return;
        }
        this.contentLength = len;
        List<String> values = new ArrayList<>();
        values.add(Long.toString(len));
        headers.put(Header.CONTENT_LENGTH, values);
    }

    @Override
    public void setContentLengthLong(long len) {
        if (committed) {
            return;
        }
        this.contentLength = len;
        setHeader(Header.CONTENT_LENGTH, Long.toString(len));
    }
    //</editor-fold>

    //<editor-fold desc="Cookie相关">
    @Override
    public void addCookie(Cookie cookie) {
        if (committed) {
            return;
        }
        if (cookie != null) {
            cookies.add(cookie);
        }
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }
    //</editor-fold>

    //<editor-fold desc="错误和重定向">
    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, getReasonPhrase(sc));
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (committed) {
            throw new IllegalStateException("Cannot send error after response has been committed");
        }

        setError();
        setStatus(sc, msg);

        // 重置缓冲区
        if (responseStream != null) {
            responseStream.suspend();
        }

        resetBuffer();

        // 发送错误页面
        String errorPage = generateErrorPage(sc, msg);
        byte[] errorBytes = errorPage.getBytes(StandardCharsets.UTF_8);

        setContentType("text/html; charset=UTF-8");
        setContentLengthLong(errorBytes.length);

        // 确保不使用chunked编码
        removeHeader(Header.TRANSFER_ENCODING);

        getWriter().write(errorPage);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (committed) {
            throw new IllegalStateException("Cannot send redirect after response has been committed");
        }

        // 清空缓冲区
        resetBuffer();

        // 设置重定向状态和头部
        setStatus(SC_FOUND);
        setHeader(Header.LOCATION, location);

        // 发送重定向页面
        String redirectPage = generateRedirectPage(location);
        byte[] redirectBytes = redirectPage.getBytes(StandardCharsets.UTF_8);

        setContentType("text/html; charset=UTF-8");
        setContentLengthLong(redirectBytes.length);

        getWriter().write(redirectPage);
    }

    /**
     * 发送100 Continue响应
     */
    public void sendAck() throws IOException {
        if (committed) {
            return;
        }

        // 直接发送100 Continue响应
        String ack = request.getProtocol() + " 100 Continue" + CRLF + CRLF;
        clientOutputStream.write(ack.getBytes(StandardCharsets.ISO_8859_1));
        clientOutputStream.flush();
    }

    private String generateErrorPage(int sc, String msg) {
        return "<html><head><title>Error " + sc + "</title></head>" +
                "<body><h1>HTTP Error " + sc + "</h1>" +
                "<p>" + msg + "</p>" +
                "<hr><address>Livonia/1.0</address></body></html>";
    }

    private String generateRedirectPage(String location) {
        return "<html><head><title>Redirect</title>" +
                "<meta http-equiv=\"refresh\" content=\"0;url=" + location + "\"></head>" +
                "<body><p>Redirecting to <a href=\"" + location + "\">" + location + "</a></p>" +
                "</body></html>";
    }
    //</editor-fold>

    //<editor-fold desc="缓冲区管理">
    @Override
    public void flushBuffer() throws IOException {
        if (!committed) {
            sendHeaders();
        }

        if (writer != null) {
            writer.flush();
        } else if (responseStream != null) {
            responseStream.flush();
        }

        if (clientOutputStream != null) {
            clientOutputStream.flush();
        }
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public void setBufferSize(int size) {
        if (committed) {
            throw new IllegalStateException("Cannot set buffer size after response has been committed");
        }

        if (outputStreamUsed || writerUsed) {
            throw new IllegalStateException("Cannot set buffer size after output has been used");
        }

        this.bufferSize = size;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void resetBuffer() {
        if (committed) {
            throw new IllegalStateException("Cannot reset buffer after response has been committed");
        }

        if (responseStream != null) {
            responseStream.recycle();
        }

        writer = null;
        outputStreamUsed = false;
        writerUsed = false;
    }

    @Override
    public void reset() {
        if (committed) {
            throw new IllegalStateException("Cannot reset after response has been committed");
        }

        // 重置状态
        status = SC_OK;
        statusMessage = "OK";
        error = false;

        // 清空头部和cookies
        headers.clear();
        cookies.clear();

        // 重置内容
        contentType = null;
        contentLength = -1;
        characterEncoding = "UTF-8";

        // 重置缓冲区
        resetBuffer();
    }
    //</editor-fold>

    //<editor-fold desc="Chunking相关">
    public boolean isAllowChunking() {
        return allowChunking;
    }

    public void setAllowChunking(boolean allowChunking) {
        this.allowChunking = allowChunking;
    }

    public boolean isChunking() {
        String te = getHeader(Header.TRANSFER_ENCODING);
        return te != null && te.toLowerCase().contains("chunked");
    }

    public boolean isCloseConnection() {
        String connection = getHeader(Header.CONNECTION);
        return connection != null && connection.equalsIgnoreCase(Header.CLOSE);
    }
    //</editor-fold>

    //<editor-fold desc="getter & setter">
    public HttpConnector getConnector() {
        return connector;
    }

    public void setConnector(HttpConnector connector) {
        this.connector = connector;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public Map<String, List<String>> getHeadersMap() {
        return this.headers;
    }
    //</editor-fold>

    //<editor-fold desc="未实现的方法">
    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public void setLocale(Locale loc) {
        if (committed) {
            return;
        }
        // 可以设置Content-Language头部
        if (loc != null) {
            setHeader("Content-Language", loc.toString());
        }
    }

    @Override
    public String encodeURL(String url) {
        // 简单实现，实际应该进行URL重写以包含session ID
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }
    //</editor-fold>

    private static class ResponsePrintWriter extends PrintWriter {
        private volatile boolean closed = false;

        public ResponsePrintWriter(Writer out) {
            super(out, false);
        }

        @Override
        public void close() {
            if (closed) {
                return;  // 已经关闭，直接返回
            }

            synchronized (lock) {
                if (closed) {
                    return;
                }
                try {
                    flush();
                    super.close();
                } catch (Exception e) {
                    // 忽略关闭时的异常
                } finally {
                    closed = true;
                }
            }
        }

        @Override
        public void flush() {
            if (!closed) {
                super.flush();
            }
        }

        @Override
        public void write(int c) {
            if (!closed) {
                super.write(c);
            }
        }

        @Override
        public void write(char[] buf, int off, int len) {
            if (!closed) {
                super.write(buf, off, len);
            }
        }

        @Override
        public void write(String s, int off, int len) {
            if (!closed) {
                super.write(s, off, len);
            }
        }

        @Override
        public boolean checkError() {
            return closed || super.checkError();
        }
    }

    // 修改 getWriter() 方法，使用 ResponsePrintWriter：
    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStreamUsed) {
            throw new IllegalStateException("getOutputStream() has already been called for this response");
        }

        writerUsed = true;

        if (writer == null) {
            if (responseStream == null) {
                responseStream = createOutputStream();
            }

            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = "ISO-8859-1";
            }

            OutputStreamWriter osw = new OutputStreamWriter(responseStream, encoding);
            writer = new ResponsePrintWriter(osw);  // 使用自定义的 PrintWriter
        }

        return writer;
    }
}

