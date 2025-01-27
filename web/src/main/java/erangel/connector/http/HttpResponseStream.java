package erangel.connector.http;

import erangel.connector.http.Const.Header;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 自定义的 ServletOutputStream 实现，直接写入 BufferedOutputStream。
 */
public class HttpResponseStream extends ServletOutputStream {
    private static final int CHUNK_BUFFER_SIZE = 8192;
    private final OutputStream outputStream;
    private final byte[] buffer = new byte[CHUNK_BUFFER_SIZE];
    private boolean useChunkedEncoding = false;
    private int byteCount;
    private boolean closed = false;
    private int bufferIndex = 0;// 缓冲区中当前写入的位置


    /**
     * 构造函数，初始化 socketOutputStream。
     *
     * @param outputStream 用于实际数据写入的 socketOutputStream
     */
    public HttpResponseStream(OutputStream outputStream, HttpResponse r) {
        this.outputStream = outputStream;
        checkChunking(r);
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
        if (!useChunkedEncoding) {
            while (len > 0) {
                int spaceLeft = CHUNK_BUFFER_SIZE - bufferIndex;
                if (len <= spaceLeft) {
                    System.arraycopy(b, off, buffer, bufferIndex, len);
                    bufferIndex += len;
                    break;
                } else {
                    System.arraycopy(b, off, buffer, bufferIndex, spaceLeft);
                    bufferIndex += spaceLeft;
                    writeChunkBufferToStream();
                    off += spaceLeft;
                    len -= spaceLeft;
                }
            }
            return;
        }

        if (len == 0) {
            return; // 无数据时什么也不做
        }

        // 如果数据长度超过缓冲区剩余空间，分批写入
        while (len > 0) {
            int spaceLeft = CHUNK_BUFFER_SIZE - bufferIndex; // 缓冲区剩余空间
            if (len <= spaceLeft) {
                // 如果剩余空间足够容纳本次写入，则写入缓冲区
                System.arraycopy(b, off, buffer, bufferIndex, len);
                bufferIndex += len;
                byteCount += len;
                break;
            } else {
                // 如果数据超过剩余空间，填满缓冲区并刷新
                System.arraycopy(b, off, buffer, bufferIndex, spaceLeft);
                bufferIndex += spaceLeft;
                byteCount += spaceLeft;
                writeChunkBufferToStream(); // 刷新到输出流
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
        if (bufferIndex > 0) {
            if (useChunkedEncoding) {
                writeChunk(buffer, 0, bufferIndex); // 如果启用了 Chunked Encoding，写入 Chunk
            } else {
                outputStream.write(buffer, 0, bufferIndex); // 普通模式写入缓冲区
            }
            bufferIndex = 0; // 清空缓冲区
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
        outputStream.write(chunkSize.getBytes());
        // 写入实际数据
        outputStream.write(b, off, len);
        // 写入 Chunk 结束符 CRLF
        outputStream.write(Const.PunctuationMarks.CRLF.getBytes());
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
                writeChunkBufferToStream(); // 关闭前刷新缓冲区
                if (useChunkedEncoding) {
                    // 如果启用了 Chunked ，写入终止块
                    outputStream.write(("0"
                            + Const.PunctuationMarks.CRLF
                            + Const.PunctuationMarks.CRLF)
                            .getBytes());
                }
            } finally {
                closed = true;
                outputStream.flush();
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
        }
    }

}
