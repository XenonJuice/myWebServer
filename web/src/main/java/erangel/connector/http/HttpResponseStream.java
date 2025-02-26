package erangel.connector.http;

import erangel.Const;
import erangel.Const.Header;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 自定义的 ServletOutputStream 实现，直接写入 BufferedOutputStream。
 */
public class HttpResponseStream extends ServletOutputStream {
    private static final int CHUNK_SIZE = 1024; // 每个 Chunk 的大小 (1 KB)
    private static final int BUFFER_SIZE = 8192; // 缓冲区大小 (8 KB)
    private static final int CHUNK_BUFFER_SIZE = 8192;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final OutputStream clienteOutputStream;
    private final HttpResponse response;
    private int bufferIndex = 0; // 缓冲区中当前写入位置
    private boolean useChunkedEncoding = false;
    private int byteCount;
    private boolean closed = false;


    /**
     * 构造函数，初始化 socketOutputStream。
     */
    public HttpResponseStream(HttpResponse response) {
        this.response = response;
        this.clienteOutputStream = response.getStream();
        checkChunking(response);
    }

    /**
     * 检查输出流是否准备好写入数据。
     *
     * @return 始终返回 true
     */
    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 设置 WriteListener 用于非阻塞 IO。
     * 这里不实现非阻塞 IO，直接抛出异常。
     *
     * @param writeListener WriteListener 实现
     */
    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException("Non-blocking IO not supported.");
    }

    /**
     * 写入单个字节到缓冲区。
     *
     * @param b 要写入的字节
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed.");
        }
        // 将字节写入到缓冲区
        buffer[bufferIndex++] = (byte) b;
        byteCount++;
        // 如果缓冲区已经满了，刷新缓冲区到输出流
        if (bufferIndex == CHUNK_BUFFER_SIZE) {
            writeChunkBufferToStream();
        }

    }

    @Override
    public void flush() {
        try {
            writeChunkBufferToStream();
        } catch (IOException _) {

        }
    }

    /**
     * 写入字节数组的一部分到缓冲区。
     *
     * @param b   字节数组
     * @param off 起始偏移量
     * @param len 写入长度
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed.");
        }
        while (len > 0) {
            int spaceLeft = BUFFER_SIZE - bufferIndex; // 缓冲区剩余空间
            if (len <= spaceLeft) {
                // 剩余数据可直接放入缓冲区
                System.arraycopy(b, off, buffer, bufferIndex, len);
                bufferIndex += len;
                byteCount += len;
                break;
            } else {
                // 填满缓冲区，并将其刷新到底层输出流
                System.arraycopy(b, off, buffer, bufferIndex, spaceLeft);
                bufferIndex += spaceLeft;
                byteCount += spaceLeft;
                writeChunkBufferToStream(); // 刷新缓冲区内容到输出流
                off += spaceLeft;
                len -= spaceLeft;
            }
        }
    }

    /**
     * 将缓存区中的数据刷新到底层输出流。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void writeChunkBufferToStream() throws IOException {
        response.writeStatusLineAndHeaders(); // 确保状态行和 Header 先写入

        if (bufferIndex > 0) { // 缓冲区有内容时
            if (useChunkedEncoding) {
                // 将缓冲区的数据拆分成多个 Chunk 写入
                int start = 0;
                while (start < bufferIndex) {
                    int remaining = bufferIndex - start; // 缓冲区剩余内容大小
                    int chunkSize = Math.min(CHUNK_SIZE, remaining); // 单次写入的 chunk 大小
                    writeChunk(buffer, start, chunkSize); // 按 Chunk 写入
                    start += chunkSize; // 移动到下一 Chunk 开始位置
                }
            } else {
                // 未使用 Chunked 时，直接写入整个缓冲区内容
                clienteOutputStream.write(buffer, 0, bufferIndex);
            }
            bufferIndex = 0; // 清空缓冲区索引
        }
    }

    /**
     * 按 Chunked Encoding 写入数据。
     *
     * @param b   数据
     * @param off 偏移
     * @param len 长度
     * @throws IOException 如果发生 I/O 错误
     */
    private void writeChunk(byte[] b, int off, int len) throws IOException {
        // 写入 Chunk 大小（十六进制） + CRLF
        String chunkSize = Integer.toHexString(len) + Const.PunctuationMarks.CRLF;
        clienteOutputStream.write(chunkSize.getBytes());
        // 写入实际数据
        clienteOutputStream.write(b, off, len);
        // 写入 Chunk 结束符 CRLF
        clienteOutputStream.write(Const.PunctuationMarks.CRLF.getBytes());

    }


    /**
     * 获取已写入的字节数。
     *
     * @return 已写入的字节数
     */
    public int getByteCount() {
        return byteCount;
    }

    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                writeChunkBufferToStream(); // 刷新缓冲区，确保所有数据写入
                if (useChunkedEncoding) {
                    // 终止块：写入 0 CRLF CRLF
                    clienteOutputStream.write(("0" + Const.PunctuationMarks.CRLF + Const.PunctuationMarks.CRLF).getBytes());
                }
            } finally {
                closed = true;
                response.flushBuffer(); // 完成响应，确保所有缓冲都下推
            }
        }
    }

    private void checkChunking(HttpResponse response) {
        // 当流中已有数据时，不处理
        if (getByteCount() != 0) return;
        useChunkedEncoding = (!response.isCommitted()
                && response.getContentLength() == -1
                && response.getStatus() != HttpServletResponse.SC_NOT_MODIFIED);
        // 连接器不允许chunk时，关闭连接
        if (!response.isAllowChunking() && useChunkedEncoding) response.setHeader(Header.CONNECTION, Header.CLOSE);
        useChunkedEncoding = (useChunkedEncoding && !response.isCloseConnection());
        if (useChunkedEncoding) {
            response.addHeader(Header.TRANSFER_ENCODING, Header.CHUNKED);
            // 移除可能重复添加chunked的情况
        } else if (response.isAllowChunking()) {
            response.removeHeader(Header.TRANSFER_ENCODING, Header.CHUNKED);
            response.addHeader(Header.CONNECTION, Header.CLOSE);
        }
    }

}
