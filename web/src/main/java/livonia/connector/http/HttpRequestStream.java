package livonia.connector.http;

import livonia.base.Const;
import livonia.connector.http.streamFilter.*;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static livonia.base.Const.Header.CONTENT_LENGTH;
import static livonia.base.Const.Header.TRANSFER_ENCODING;

/**
 * 自定义的 ServletInputStream 实现，用于包装底层的 FilterChainInputStream
 *
 * @author LILINJIAN
 * @version 2025/06/04
 */
public class HttpRequestStream extends ServletInputStream {
    //<editor-fold desc = "属性">
    // 整个过滤链适配后的输入流
    private FilterChainInputStream filterStream;
    // 顶层过滤器（保存引用以便复用）
    private InputFilter topFilter;
    // 底层输入流（通常是SocketInputBuffer）
    private InputStream socketInputStream;
    // 当前使用的过滤器类型
    private FilterType currentFilterType = FilterType.NONE;
    // Content-Length过滤器引用（用于复用）
    private ContentLengthFilter contentLengthFilter;
    // Chunked过滤器引用（用于复用）
    private ChunkedFilter chunkedFilter;
    // 状态标志
    private boolean isFinished = false;
    private boolean closed = false;
    // 统计信息
    private long bytesRead = 0;

    // 过滤器类型枚举
    private enum FilterType {
        NONE,
        PASSTHROUGH,
        CONTENT_LENGTH,
        CHUNKED
    }
    //</editor-fold>

    //<editor-fold desc = "构造器">

    /**
     * 构造函数，初始化 HttpRequestStream
     *
     * @param request HTTP请求对象
     */
    public HttpRequestStream(HttpRequest request) {
        this.socketInputStream = request.getStream();
        setupFilters(request);
    }
    //</editor-fold>

    //<editor-fold desc = "初始化方法">

    /**
     * 根据请求头设置合适的过滤器链
     *
     * @param request HTTP请求对象
     */
    private void setupFilters(HttpRequest request) {
        Map<String, List<String>> headers = request.getHeaderMap();

        // 检查Transfer-Encoding
        List<String> transferEncodingList = headers.get(TRANSFER_ENCODING);
        if (transferEncodingList != null && !transferEncodingList.isEmpty()) {
            for (String transferEncoding : transferEncodingList) {
                if (Const.Header.CHUNKED.equalsIgnoreCase(transferEncoding)) {
                    setupChunkedFilter();
                    return;
                }
            }
        }

        // 检查Content-Length
        List<String> contentLengthList = headers.get(CONTENT_LENGTH);
        if (contentLengthList != null && !contentLengthList.isEmpty()) {
            try {
                long contentLength = Long.parseLong(contentLengthList.get(0));
                setupContentLengthFilter(contentLength);
                return;
            } catch (NumberFormatException e) {
                // 无效的Content-Length，使用透传过滤器
            }
        }

        // 既无Transfer-Encoding也无Content-Length，使用透传过滤器
        setupPassthroughFilter();
    }

    /**
     * 设置分块编码过滤器
     */
    private void setupChunkedFilter() {
        if (chunkedFilter == null) {
            chunkedFilter = new ChunkedFilter(socketInputStream);
        }
        topFilter = chunkedFilter;
        currentFilterType = FilterType.CHUNKED;
        filterStream = new FilterChainInputStream(topFilter);
    }

    /**
     * 设置固定长度过滤器
     *
     * @param contentLength 内容长度
     */
    private void setupContentLengthFilter(long contentLength) {
        if (contentLengthFilter == null) {
            contentLengthFilter = new ContentLengthFilter(socketInputStream, contentLength);
        } else {
            contentLengthFilter.setContentLength(contentLength);
        }
        topFilter = contentLengthFilter;
        currentFilterType = FilterType.CONTENT_LENGTH;
        filterStream = new FilterChainInputStream(topFilter);
    }

    /**
     * 设置透传过滤器
     */
    private void setupPassthroughFilter() {
        topFilter = new PassthroughFilter(socketInputStream);
        currentFilterType = FilterType.PASSTHROUGH;
        filterStream = new FilterChainInputStream(topFilter);
    }
    //</editor-fold>

    //<editor-fold desc = "ServletInputStream方法实现">

    /**
     * 检查输入流是否已完成读取
     *
     * @return 如果所有数据都已读取，则返回 true；否则返回 false
     */
    @Override
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * 检查输入流是否准备好读取数据
     *
     * @return 始终返回 true（阻塞I/O模式）
     */
    @Override
    public boolean isReady() {
        return !isFinished;
    }

    /**
     * 设置 ReadListener，用于非阻塞 I/O
     *
     * @param readListener ReadListener 实现
     */
    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Non-blocking I/O not supported");
    }

    /**
     * 读取单个字节
     *
     * @return 读取到的字节（0-255），如果到达流末尾返回-1
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (isFinished) {
            return -1;
        }

        int b = filterStream.read();
        if (b == -1) {
            isFinished = true;
        } else {
            bytesRead++;
        }
        return b;
    }

    /**
     * 读取多个字节到缓冲区
     *
     * @param b   目标缓冲区
     * @param off 起始偏移量
     * @param len 最大读取长度
     * @return 实际读取的字节数，如果到达流末尾返回-1
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (b == null) {
            throw new NullPointerException("Buffer cannot be null");
        }

        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid parameters: off=" + off + ", len=" + len + ", buffer.length=" + b.length);
        }

        if (len == 0) {
            return 0;
        }

        if (isFinished) {
            return -1;
        }

        int n = filterStream.read(b, off, len);
        if (n == -1) {
            isFinished = true;
        } else {
            bytesRead += n;
        }
        return n;
    }

    /**
     * 关闭输入流
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        try {
            // 消费剩余的所有数据
            if (!isFinished && socketInputStream.available() > 0) {
                byte[] buffer = new byte[1024];
                while (read(buffer) != -1) {
                    // 继续读取直到EOF
                }
            }

            // 通知过滤器链结束
            filterStream.end();
        } finally {
            closed = true;
            isFinished = true;
        }
    }
    //</editor-fold>

    //<editor-fold desc = "复用相关方法">

    /**
     * 为下一个请求重置流状态
     */
    public void recycle() {
        // 重置状态
        isFinished = false;
        closed = false;
        bytesRead = 0;

        // 重置过滤器
        if (topFilter != null) {
            topFilter.recycle();
        }
    }

    /**
     * 为新请求配置过滤器（根据新的请求头）
     */
    public void setupForNewRequest(HttpRequest request) {
        this.socketInputStream = request.getStream();
        if (this.socketInputStream == null) {
            throw new IllegalStateException("Base input stream is null");
        }
        // 根据新请求的头部重新配置过滤器
        setupFilters(request);
    }

    /**
     * 获取已读取的字节数
     *
     * @return 字节数
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * 获取当前使用的过滤器类型
     *
     * @return 过滤器类型
     */
    public String getCurrentFilterType() {
        return currentFilterType.name();
    }
    //</editor-fold>

    //<editor-fold desc = "其他方法">

    /**
     * 跳过指定数量的字节
     *
     * @param n 要跳过的字节数
     * @return 实际跳过的字节数
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public long skip(long n) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (n <= 0) {
            return 0;
        }

        long remaining = n;
        int size = (int) Math.min(2048, remaining);
        byte[] skipBuffer = new byte[size];

        while (remaining > 0) {
            int nr = read(skipBuffer, 0, (int) Math.min(size, remaining));
            if (nr < 0) {
                break;
            }
            remaining -= nr;
        }

        return n - remaining;
    }

    /**
     * 返回可读取的字节数估计值
     *
     * @return 可读字节数
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (isFinished) {
            return 0;
        }

        // 对于Content-Length过滤器，可以返回准确的剩余字节数
        if (currentFilterType == FilterType.CONTENT_LENGTH && contentLengthFilter != null) {
            return (int) Math.min(Integer.MAX_VALUE, contentLengthFilter.getRemaining());
        }

        // 其他情况返回0（保守估计）
        return 0;
    }
    //</editor-fold>
}