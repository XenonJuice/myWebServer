package livonia.connector.http;

import livonia.base.Const.Header;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import static livonia.base.Const.PunctuationMarks.CRLF;

/**
 * 自定义的 ServletOutputStream 实现
 * 修复了chunked编码逻辑，增强了keepAlive场景下的复用能力
 *
 * @author LILINJIAN
 * @version 2025/06/04
 */
public class HttpResponseStream extends ServletOutputStream {
    //<editor-fold desc = "常量">
    private static final int DEFAULT_BUFFER_SIZE = 8192;  // 默认缓冲区大小
    private static final int MIN_CHUNK_SIZE = 512;        // 最小chunk大小
    private static final int MAX_CHUNK_SIZE = 16384;      // 最大chunk大小
    private static final byte[] CRLF_BYTES = CRLF.getBytes(); // CRLF字节
    private static final byte[] ZERO_CHUNK = ("0" + CRLF + CRLF).getBytes(); // 结束chunk
    //</editor-fold>
    // 输出流和响应对象
    private final OutputStream clientOutputStream;
    private final HttpResponse response;
    //<editor-fold desc = "属性">
    // 内部缓冲区
    private byte[] buffer;
    private int bufferSize;
    private int bufferCount = 0;
    // 状态标志
    private boolean useChunkedEncoding = false;
    private boolean closed = false;
    private boolean committed = false;
    private boolean suspended = false;

    // 统计信息
    private long totalBytesWritten = 0;
    private int chunksWritten = 0;

    // chunk大小（动态调整）
    private int currentChunkSize = MIN_CHUNK_SIZE;
    //</editor-fold>

    //<editor-fold desc = "构造器">

    /**
     * 构造函数，初始化输出流
     *
     * @param response HTTP响应对象
     */
    public HttpResponseStream(HttpResponse response) {
        this(response, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 构造函数，初始化输出流（指定缓冲区大小）
     *
     * @param response   HTTP响应对象
     * @param bufferSize 缓冲区大小
     */
    public HttpResponseStream(HttpResponse response, int bufferSize) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }

        this.response = response;
        this.clientOutputStream = response.getStream();
        this.bufferSize = bufferSize;
        this.buffer = new byte[bufferSize];
    }
    //</editor-fold>

    //<editor-fold desc = "ServletOutputStream方法实现">

    /**
     * 检查输出流是否准备好写入数据
     *
     * @return 始终返回 true（阻塞I/O模式）
     */
    @Override
    public boolean isReady() {
        return !closed;
    }

    /**
     * 设置 WriteListener 用于非阻塞 IO
     *
     * @param writeListener WriteListener 实现
     */
    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException("Non-blocking I/O not supported");
    }

    /**
     * 写入单个字节到缓冲区
     *
     * @param b 要写入的字节
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (suspended) {
            return;
        }

        // 如果缓冲区已满，先刷新
        if (bufferCount >= bufferSize) {
            flushBuffer();
        }

        buffer[bufferCount++] = (byte) b;
        totalBytesWritten++;
    }

    /**
     * 写入字节数组的一部分到缓冲区
     *
     * @param b   字节数组
     * @param off 起始偏移量
     * @param len 写入长度
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
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

        if (len == 0 || suspended) {
            return;
        }

        // 如果数据量大于缓冲区，直接写入
        if (len > bufferSize) {
            // 先刷新缓冲区中的现有数据
            flushBuffer();
            // 直接写入大块数据
            writeToOutputStream(b, off, len);
            return;
        }

        int remaining = len;
        int offset = off;

        while (remaining > 0) {
            int available = bufferSize - bufferCount;
            if (available == 0) {
                flushBuffer();
                available = bufferSize;
            }

            int toWrite = Math.min(remaining, available);
            System.arraycopy(b, offset, buffer, bufferCount, toWrite);
            bufferCount += toWrite;
            offset += toWrite;
            remaining -= toWrite;
            totalBytesWritten += toWrite;
        }
    }

    /**
     * 刷新缓冲区内容到输出流
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void flush() throws IOException {
        if (closed) return;
        flushBuffer();
        clientOutputStream.flush();
    }

    /**
     * 关闭输出流
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        if (!suspended) {
            try {
                // 刷新缓冲区中的剩余数据
                flushBuffer();

                // 如果使用chunked编码，写入结束标记
                if (useChunkedEncoding && committed) {
                    clientOutputStream.write(ZERO_CHUNK);
                    clientOutputStream.flush();
                }
            } finally {
                closed = true;
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc = "内部方法">

    /**
     * 刷新缓冲区内容到底层输出流
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void flushBuffer() throws IOException {
        if (bufferCount == 0) {
            return;
        }

        // 确保响应头已发送
        if (!committed) {
            // 在发送头之前检查
            checkChunking();
            response.sendHeaders();
            committed = true;
        }

        writeToOutputStream(buffer, 0, bufferCount);
        bufferCount = 0;
    }

    /**
     * 将数据写入底层输出流
     *
     * @param data 数据
     * @param off  偏移量
     * @param len  长度
     * @throws IOException 如果发生I/O错误
     */
    private void writeToOutputStream(byte[] data, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }

        if (useChunkedEncoding) {
            writeChunked(data, off, len);
        } else {
            clientOutputStream.write(data, off, len);
            totalBytesWritten += len;  // 更新已写入字节数
        }
    }

    /**
     * 使用chunked编码写入数据
     *
     * @param data 数据
     * @param off  偏移量
     * @param len  长度
     * @throws IOException 如果发生I/O错误
     */
    private void writeChunked(byte[] data, int off, int len) throws IOException {
        int remaining = len;
        int offset = off;

        while (remaining > 0) {
            int chunkSize = Math.min(currentChunkSize, remaining);

            // 写入chunk大小（十六进制）
            String hexSize = Integer.toHexString(chunkSize);
            clientOutputStream.write(hexSize.getBytes());
            clientOutputStream.write(CRLF_BYTES);

            // 写入chunk数据
            clientOutputStream.write(data, offset, chunkSize);
            clientOutputStream.write(CRLF_BYTES);

            offset += chunkSize;
            remaining -= chunkSize;
            chunksWritten++;
            totalBytesWritten += chunkSize;  // 统计实际数据字节数（不含chunk元数据）

            // 动态调整chunk大小（逐渐增大以提高效率）
            if (currentChunkSize < MAX_CHUNK_SIZE) {
                currentChunkSize = Math.min(currentChunkSize * 2, MAX_CHUNK_SIZE);
            }
        }
    }

    /**
     * 检查并设置chunked编码
     */
    private void checkChunking() {
        // 如果已经设置了Content-Length，不使用chunked
        if (response.getContentLength() >= 0) {
            useChunkedEncoding = false;
            return;
        }

        // 检查是否是不应该有响应体的状态码
        int status = response.getStatus();
        if (status == HttpServletResponse.SC_NOT_MODIFIED ||
                status == HttpServletResponse.SC_NO_CONTENT ||
                status >= 100 && status < 200) {
            useChunkedEncoding = false;
            return;
        }

        // 检查连接器是否允许chunking
        if (!response.isAllowChunking()) {
            useChunkedEncoding = false;
            response.setHeader(Header.CONNECTION, Header.CLOSE);
            return;
        }

        // 检查是否要关闭连接
        if (response.isCloseConnection()) {
            useChunkedEncoding = false;
            return;
        }

        // 使用chunked编码
        useChunkedEncoding = true;
        response.setHeader(Header.TRANSFER_ENCODING, Header.CHUNKED);
    }
    //</editor-fold>

    //<editor-fold desc = "复用相关方法">

    /**
     * 重置流状态，为下一个响应做准备
     */
    public void recycle() {
        // 重置缓冲区
        bufferCount = 0;

        // 重置状态标志
        closed = false;
        committed = false;
        suspended = false;
        useChunkedEncoding = false;

        // 重置统计信息
        totalBytesWritten = 0;
        chunksWritten = 0;

        // 重置chunk大小
        currentChunkSize = MIN_CHUNK_SIZE;
    }

    /**
     * 暂停输出（丢弃后续写入的数据）
     */
    public void suspend() {
        suspended = true;
    }

    /**
     * 恢复输出
     */
    public void resume() {
        suspended = false;
    }
    //</editor-fold>

    //<editor-fold desc = "getter方法">

    /**
     * 获取已写入的总字节数
     *
     * @return 字节数
     */
    public long getTotalBytesWritten() {
        return totalBytesWritten;
    }

    /**
     * 获取缓冲区中尚未写入的数据大小
     *
     * @return 缓冲区中的字节数
     */
    public int getBufferedDataSize() {
        return bufferCount;
    }

    /**
     * 获取已写入的chunk数量
     *
     * @return chunk数量
     */
    public int getChunksWritten() {
        return chunksWritten;
    }

    /**
     * 检查是否使用chunked编码
     *
     * @return true如果使用chunked编码
     */
    public boolean isUsingChunkedEncoding() {
        return useChunkedEncoding;
    }

    /**
     * 检查流是否已关闭
     *
     * @return true如果已关闭
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 检查响应是否已提交
     *
     * @return true如果已提交
     */
    public boolean isCommitted() {
        return committed;
    }
    //</editor-fold>
}