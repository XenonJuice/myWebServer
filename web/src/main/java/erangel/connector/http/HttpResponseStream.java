package erangel.connector.http;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * 自定义的 ServletOutputStream 实现，使用 BufferedOutputStream 进行数据写入。
 */
public class HttpResponseStream extends ServletOutputStream {
    private final BufferedOutputStream bufferedOutputStream;

    /**
     * 构造函数，初始化 BufferedOutputStream。
     *
     * @param bufferedOutputStream 用于实际数据写入的 BufferedOutputStream
     */
    public HttpResponseStream(BufferedOutputStream bufferedOutputStream) {
        this.bufferedOutputStream = bufferedOutputStream;
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
        bufferedOutputStream.write(b);
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
        bufferedOutputStream.write(b, off, len);
    }

    /**
     * 刷新缓冲区，将数据写入底层输出流。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void flush() throws IOException {
        bufferedOutputStream.flush();
    }

    /**
     * 关闭输出流，释放资源。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        bufferedOutputStream.close();
    }
}
