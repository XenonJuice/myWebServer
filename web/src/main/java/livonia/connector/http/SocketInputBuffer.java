package livonia.connector.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * 带内部缓冲区的输入流，用于高效读取Socket数据
 */
public class SocketInputBuffer extends InputStream {
    //<editor-fold desc = "attr">
    // socket.getInputStream()
    private InputStream socketInputStream;
    // 内部缓冲区
    private final byte[] innerBuffer;
    // 缓冲区有效字节数
    public int bufferCount = 0;
    // 内部缓冲中下一个应该读取的位置
    private int pos = 0;
    // 标记是否已到达流末尾
    private boolean eof = false;
    // 是否已关闭
    private boolean closed = false;
    // 从流中读取的总字节数（用于统计）
    private long totalBytesRead = 0;

    //</editor-fold>
    //<editor-fold desc = "构造器">
    public SocketInputBuffer(InputStream inputStream, int bufferSize) {
        this.socketInputStream = inputStream;
        this.innerBuffer = new byte[bufferSize];
    }
    //</editor-fold>
    //<editor-fold desc = "读取请求头">
    //</editor-fold>
    //<editor-fold desc = "读取，填充，非阻塞字节数">

    /**
     * 从内部缓冲区或底层输入流读取下一个字节的数据。
     * 如果内部缓冲区耗尽，它会尝试从输入流重新填充缓冲区。
     *
     * @return 下一个字节的数据，作为无符号整数，范围在 0 到 255 之间；
     * 如果已到达流的末尾，则返回 -1。
     * @throws IOException 如果在从输入流读取时发生 I/O 错误。
     */
    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        // 如果已经到达EOF，直接返回-1
        if (eof) {
            return -1;
        }
        // 如果当前的位置大于等于内部缓冲的有效字节数，说明要从底层流向内部缓冲填充数据
        if (pos >= bufferCount) {
            fillBuffer();
            // 若填充之后内部缓冲大小小于等于0 则说明底层流已被榨干
            if (bufferCount <= 0) {
                eof = true;
                return -1;
            }
        }
        totalBytesRead++;
        // 返回一个正整数
        return innerBuffer[pos++] & 0xFF;
    }

    /**
     * 从输入流中读取最多 {@code len} 字节的数据到字节数组中。
     * 此方法将在有输入可用、检测到流的结尾或抛出异常之前阻塞。
     *
     * @param buffer 用于存储读取到的数据的字节数组。
     * @param off    数据写入数组的起始偏移量。
     * @param len    最大读取字节数。
     * @return 读取到缓冲区的字节总数，如果因为达到流的末尾没有更多数据，则返回 -1。
     * @throws IndexOutOfBoundsException 如果 {@code off < 0}、{@code len < 0} 或 {@code off + len > buffer.length}。
     * @throws IOException               如果发生 I/O 错误。
     */
    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (buffer == null) {
            throw new NullPointerException("Buffer cannot be null");
        }

        if (off < 0 || len < 0 || off + len > buffer.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid parameters: off=" + off + ", len=" + len + ", buffer.length=" + buffer.length);
        }
        if (len == 0) {
            return 0;
        }

        // 如果已经到达EOF，直接返回-1
        if (eof) {
            return -1;
        }
        // 更高效的做法，既然我们需要取出一块较大数据。那么直接先判断内部缓冲区和所需数据的大小
        int available = availableInnerBuffer();

        if (available > 0) {
            if (available >= len) {
                // 缓冲区数据足够，直接复制
                System.arraycopy(innerBuffer, pos, buffer, off, len);
                pos += len;
                totalBytesRead += len;
                return len;
            }

            // 缓冲区数据不够，先复制缓冲区中的所有数据
            System.arraycopy(innerBuffer, pos, buffer, off, available);
            pos += available;

            // 然后尝试直接从底层流读取剩余数据
            int bytesRead = socketInputStream.read(buffer, off + available, len - available);

            if (bytesRead == -1) {
                // 底层流已结束
                eof = true;
                totalBytesRead += available;
                return available;
            }

            totalBytesRead += available + bytesRead;
            return available + bytesRead;
        }

        // 缓冲区为空，直接从底层流读取
        int bytesRead = socketInputStream.read(buffer, off, len);
        if (bytesRead == -1) {
            eof = true;
        } else {
            totalBytesRead += bytesRead;
        }
        return bytesRead;
    }

    // 填充缓冲区，从底层流读取数据
    public void fillBuffer() throws IOException {
        pos = 0;
        bufferCount = socketInputStream.read(innerBuffer, 0, innerBuffer.length);
        if (bufferCount == -1) {
            bufferCount = 0;
            eof = true;
        }
    }

    /**
     * 返回缓冲区当前尚未消费的字节数，即 count - pos。
     *
     * @return 缓冲区中可读取的字节数
     */
    public int availableInnerBuffer() {
        return bufferCount - pos;
    }

    /**
     * 返回可以从此输入流读取（或跳过）的字节数的估计值
     *
     * @return 可读取的字节数估计值
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (eof) {
            return 0;
        }

        int n = availableInnerBuffer();
        if (n > 0) {
            return n;
        }

        // 如果缓冲区为空，检查底层流
        return socketInputStream.available();
    }

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

        long totalSkipped = 0;

        // 首先尝试跳过缓冲区中的数据
        int available = availableInnerBuffer();
        if (available > 0) {
            int toSkip = (int) Math.min(n, available);
            pos += toSkip;
            totalSkipped += toSkip;
            n -= toSkip;
        }

        // 如果还需要跳过更多，从底层流跳过
        if (n > 0 && !eof) {
            long skipped = socketInputStream.skip(n);
            if (skipped > 0) {
                totalSkipped += skipped;
            }
        }

        return totalSkipped;
    }

    //<editor-fold desc = "复用相关方法">

    /**
     * 为复用准备：设置新的底层输入流
     *
     * @param newInputStream 新的输入流
     */
    public void setInputStream(InputStream newInputStream) {
        if (newInputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        this.socketInputStream = newInputStream;
        recycle();
    }

    /**
     * 回收并重置状态，为下一次使用做准备
     * 注意：不会关闭底层流
     */
    public void recycle() {
        pos = 0;
        bufferCount = 0;
        eof = false;
        closed = false;
        totalBytesRead = 0;
    }

    /**
     * 获取已读取的总字节数
     *
     * @return 总字节数
     */
    public long getTotalBytesRead() {
        return totalBytesRead;
    }

    /**
     * 检查是否已到达流末尾
     *
     * @return true如果已到达EOF
     */
    public boolean isEof() {
        return eof;
    }
    //</editor-fold>
    //<editor-fold desc = "关闭相关">

    /**
     * 关闭此输入流并释放与该流关联的所有系统资源
     *
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            // 注意：在keepAlive场景下，不应该关闭底层socket流
            // 只标记当前包装流为已关闭
        }
    }

    /**
     * 真正关闭底层流（在连接真正结束时调用）
     *
     * @throws IOException 如果发生I/O错误
     */
    public void closeUnderlying() throws IOException {
        close();
        if (socketInputStream != null) {
            socketInputStream.close();
        }
    }
    //</editor-fold>
}
