package erangel.connector.http;

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
    private final BufferedInputStream bufferedInputStream;
    private boolean isFinished = false;
    private boolean isReady = true; // 始终返回 true
    private ReadListener readListener;

    /**
     * 构造函数，初始化 BufferedInputStream。
     *
     * @param inputStream 客户端发送的 InputStream
     */
    public HttpRequestStream(InputStream inputStream) {
        this.bufferedInputStream = new BufferedInputStream(inputStream, 8192);
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
        int data = bufferedInputStream.read();
        if (data == -1) {
            isFinished = true;
        }
        return data;
    }

    /**
     * 读取字节数组的一部分。
     *
     * @param b   目标字节数组
     * @param off 目标数组的起始偏移量
     * @param len 要读取的最大字节数
     * @return 实际读取的字节数，或者 -1 表示流结束
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = bufferedInputStream.read(b, off, len);
        if (bytesRead == -1) {
            isFinished = true;
        }
        return bytesRead;
    }

    /**
     * 关闭输入流，释放资源。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        bufferedInputStream.close();
        super.close();
    }
}
