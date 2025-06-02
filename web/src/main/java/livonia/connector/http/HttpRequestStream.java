package livonia.connector.http;

import livonia.base.Const;
import livonia.connector.http.streamFilter.ChunkedFilter;
import livonia.connector.http.streamFilter.FilterChainInputStream;
import livonia.connector.http.streamFilter.InputFilter;
import livonia.connector.http.streamFilter.PassthroughFilter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static livonia.base.Const.Header.CONTENT_LENGTH;
import static livonia.base.Const.Header.TRANSFER_ENCODING;

/**
 * 自定义的 ServletInputStream 实现，用于包装底层的 FilterChainInputStream。
 * 主要用于 HttpRequest 类中，提供读取请求体数据的功能。
 */
public class HttpRequestStream extends ServletInputStream {
    // 整个过滤链适配后的输入流
    private final FilterChainInputStream filterStream;
    private boolean isFinished = false;
    private final boolean isReady = true; // 始终返回 true
    private ReadListener readListener;
    private boolean closed = false;

    /**
     * 构造函数，初始化 FilterChainInputStream。
     */
    public HttpRequestStream(HttpRequest request,HttpResponse response) {
        SocketInputBuffer socketBuffer = request.getSocketInputBuffer();
        Map<String, List<String>> headers = request.getHeaderMap();
        List<String> transferEncodingList = headers.get(TRANSFER_ENCODING);
        InputFilter topFilter = null;
        for (String transferEncoding : transferEncodingList) {
            if (Const.Header.CHUNKED.equalsIgnoreCase(transferEncoding)) {
                topFilter = new ChunkedFilter(socketBuffer);
            } else if (headers.containsKey(CONTENT_LENGTH)) {
                // 没有 Transfer-Encoding，再看 Content-Length
                long len = Long.parseLong(headers.get(CONTENT_LENGTH).getFirst());
                // TODO make a content-length filter
                // topFilter = new ContentLengthFilter(socketBuffer, len);
            } else {
                // 既无 Transfer-Encoding 也无 Content-Length，全部透传
                topFilter = new PassthroughFilter(socketBuffer);
            }
        }
        this.filterStream = new FilterChainInputStream(topFilter);
    }

    /**
     * 检查输入流是否已完成读取。
     *
     * @return 如果所有数据都已读取，则返回 true；否则返回 false
     */
    @Override
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * 检查输入流是否准备好读取数据。
     *
     * @return 如果输入流准备好读取数据，则返回 true；否则返回 false
     */
    @Override
    public boolean isReady() {
        return isReady;
    }

    /**
     * 设置 ReadListener，用于非阻塞 I/O。
     * 这里不支持非阻塞 I/O，直接抛出异常。
     *
     * @param readListener ReadListener 实现
     */
    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Non-blocking IO not supported.");
    }

    /**
     * 读多字节，直接委托给 filterStream.read(...)。
     * 如果读到 -1，标记 closed 为 true。
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (isFinished) {
            return -1;
        }
        int n = filterStream.read(b, off, len);
        if (n == -1) {
            isFinished = true;
        }
        return n;
    }

    /**
     * 单字节读取，委托给 filterStream.read()。
     */
    @Override
    public int read() throws IOException {
        if (isFinished) {
            return -1;
        }
        int n = filterStream.read();
        if (n == -1) {
            isFinished = true;
        }
        return n;
    }

    /**
     * 设定停止标志位
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("Stream already closed!");
        }
        while(read()!=-1){
            read();
        }
        filterStream.end();
        closed = true;
        super.close();
    }
}
