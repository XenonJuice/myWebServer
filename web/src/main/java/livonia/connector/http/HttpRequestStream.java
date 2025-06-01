package livonia.connector.http;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 自定义的 ServletInputStream 实现，用于包装底层的 InputStream。
 * 主要用于 HttpRequest 类中，提供读取请求体数据的功能。
 */
public class HttpRequestStream extends ServletInputStream {
    private final InputStream inputStream;
    private final BufferedInputStream bufferedInputStream;
    private boolean isFinished = false;
    private final boolean isReady = true; // 始终返回 true
    private ReadListener readListener;

    public void setHttp11(Boolean http11) {
        this.http11 = http11;
    }

    private  Boolean http11 =false;
    private final int bufferSize = 8192;
    private boolean closed = false;

    public boolean isUseChunkedEncoding() {
        return useChunkedEncoding;
    }

    public void setUseChunkedEncoding(boolean useChunkedEncoding) {
        this.useChunkedEncoding = useChunkedEncoding;
    }

    private boolean useChunkedEncoding = false;
    private int chunkSize = 0; // 当前chunk的剩余大小

    /**
     * 构造函数，初始化 BufferedInputStream。
     */
    public HttpRequestStream(HttpRequest request,HttpResponse response) {
        this.inputStream = request.getStream();
        this.bufferedInputStream = new BufferedInputStream(inputStream, bufferSize);
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
     * 读取单个字节。
     *
     * @return 读取的字节，或者 -1 表示流结束
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public int read() throws IOException {
        if (!useChunkedEncoding) {
            // 非分块模式，直接读取输入流
            int data = bufferedInputStream.read();
            if (data == -1) {
                isFinished = true;
            }
            return data;
        }

        // 分块模式
        while (chunkSize == 0) { // 如果当前 chunk 大小为 0，读取新 chunk
            StringBuilder hexSize = new StringBuilder();
            int ch;
            while ((ch = bufferedInputStream.read()) != -1) {
                if (ch == '\r') { // 遇到 CR 时，跳过后续的 LF
                    bufferedInputStream.read(); // 读取 '\n'
                    break;
                }
                hexSize.append((char) ch);
            }

            if (hexSize.isEmpty() || ch == -1) { // 没有读取到有效数据或到达流结束
                isFinished = true;
                return -1;
            }

            // 解析 chunk size
            chunkSize = Integer.parseInt(hexSize.toString(), 16);

            if (chunkSize == 0) {
                // Chunk 长度为 0，表示结束
                isFinished = true;
                bufferedInputStream.read(); // 跳过 "\r\n" 的 CR
                bufferedInputStream.read(); // 跳过 "\r\n" 的 LF
                return -1; // 标志结束
            }
        }

        // 读取 chunk 中的一个字节
        int result = bufferedInputStream.read();
        if (result != -1) {
            chunkSize--; // 更新 chunk 剩余大小
        }

        // 如果 chunk 数据读取完毕，跳过尾随的 CRLF
        if (chunkSize == 0) {
            bufferedInputStream.read(); // 跳过 CR
            bufferedInputStream.read(); // 跳过 LF
        }

        return result;
    }

    /**
     * 读取字节数组的一部分。
     * chunk-size\r\n
     * chunk-data\r\n
     *
     * @param b   目标字节数组
     * @param off 目标数组的起始偏移量
     * @param len 要读取的最大字节数
     * @return 实际读取的字节数，或者 -1 表示流结束
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!useChunkedEncoding) {
            return bufferedInputStream.read(b, off, len); // 不采用分块时 沿用原有逻辑
        }

        int bytesRead = 0;// 已读取字节数量

        while (bytesRead < len) { // 已读字节 小于 期望读取的长度 时

            if (chunkSize <= 0) { // 还未开始读取chunkSize
                // 读取新的 chunk size (格式：chunk size + CRLF)
                StringBuilder hexSize = new StringBuilder();
                int ch; // 读取到的ASCII值
                while ((ch = bufferedInputStream.read()) != -1) { // 只要流中依然有数据传入
                    if (ch == '\r') { // 跳过\r'
                        bufferedInputStream.read(); // 再跳过 '\n'
                        break;
                    }
                    hexSize.append((char) ch);// 设置值，为分块大小
                }
                chunkSize = Integer.parseInt(hexSize.toString(), 16);

                if (chunkSize == 0) {
                    isFinished = true; // 结束 chunked 流
                    bufferedInputStream.read(); // 结束符 CRLF 的 "\r"
                    bufferedInputStream.read(); // 结束符 CRLF 的 "\n"
                    return -1;
                }
            }

            // 读取 chunk 内容
            // Math.min 返回两数中的最小值
            // 比较 分块大小 和 目标长度-已读字节数量 ，取最小值
            int toRead = Math.min(chunkSize, len - bytesRead);
            int readNow = bufferedInputStream.read(b, off + bytesRead, toRead);// 读取
            if (readNow == -1) { // 流中已无数据
                isFinished = true;
                return bytesRead > 0 ? bytesRead : -1;
            }

            bytesRead += readNow;
            chunkSize -= readNow;

            // 如果 chunk 完成，跳过 CRLF
            if (chunkSize == 0) {
                bufferedInputStream.read(); // 跳过 '\r'
                bufferedInputStream.read(); // 跳过 '\n'
            }
        }
        return bytesRead;
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
        if (useChunkedEncoding) {
            while (chunkSize > 0) {
                int a = read();
                if (a < 0) break;
            }
        } else {
            if (http11) {
                int a = read();
                while (a != -1) {
                    a = read();
                }
            }
        }
        closed = true;
    }
}
