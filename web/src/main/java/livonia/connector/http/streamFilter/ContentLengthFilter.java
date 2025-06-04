package livonia.connector.http.streamFilter;

import livonia.connector.http.ByteChunk;

import java.io.IOException;
import java.io.InputStream;

/**
 * Content-Length 过滤器，用于处理具有固定长度的请求体
 *
 * @author LILINJIAN
 * @version 2025/06/04
 */
public class ContentLengthFilter implements InputFilter {
    //<editor-fold desc = "属性">
    // 上游输入流
    private final InputStream next;
    // 初始的Content-Length值
    private long contentLength = 0;
    // 剩余未读字节数
    private long remaining = 0;
    // 是否已到达EOF
    private boolean eof = false;
    // 统计信息
    private long totalBytesRead = 0;
    // 临时缓冲区大小
    private static final int TEMP_BUFFER_SIZE = 8192;
    //</editor-fold>

    //<editor-fold desc = "构造器">
    /**
     * 构造一个Content-Length过滤器
     *
     * @param next 上游输入流
     * @param contentLength Content-Length值
     */
    public ContentLengthFilter(InputStream next, long contentLength) {
        if (next == null) {
            throw new IllegalArgumentException("Upstream input stream cannot be null");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content-Length cannot be negative: " + contentLength);
        }
        this.next = next;
        this.contentLength = contentLength;
        this.remaining = contentLength;
        this.eof = (contentLength == 0);
    }
    //</editor-fold>

    //<editor-fold desc = "接口实现">
    /**
     * 返回此 Filter 的编码名称
     */
    @Override
    public String getEncodingName() {
        return "content-length";
    }

    /**
     * 从上游流读取数据，但不超过Content-Length指定的长度
     *
     * @param chunk 用于存放读取到的字节
     * @return 实际写入 chunk 的字节数；如果到达流末尾，返回 -1
     * @throws IOException 如果读取失败
     */
    @Override
    public int doRead(ByteChunk chunk) throws IOException {
        if (eof || remaining == 0) {
            eof = true;
            return -1;
        }

        // 计算本次最多能读取的字节数
        int available = chunk.getAvailable();
        if (available <= 0) {
            return 0; // chunk缓冲区已满
        }

        // 不能超过剩余的Content-Length
        int toRead = (int) Math.min(available, Math.min(remaining, TEMP_BUFFER_SIZE));

        // 创建临时缓冲区
        byte[] buffer = new byte[toRead];

        // 从上游读取数据
        int bytesRead = next.read(buffer, 0, toRead);

        if (bytesRead == -1) {
            // 上游流意外结束（在Content-Length之前）
            if (remaining > 0) {
                throw new IOException("Unexpected EOF: expected " + remaining + " more bytes");
            }
            eof = true;
            return -1;
        }

        if (bytesRead > 0) {
            // 将读取到的数据追加到chunk
            chunk.append(buffer, 0, bytesRead);
            remaining -= bytesRead;
            totalBytesRead += bytesRead;

            // 如果已经读取了所有数据，标记EOF
            if (remaining == 0) {
                eof = true;
            }
        }

        return bytesRead;
    }

    /**
     * 结束处理，确保所有Content-Length指定的数据都已读取
     *
     * @throws IOException 如果还有未读数据
     */
    @Override
    public void end() throws IOException {
        // 如果还有剩余数据未读，消费掉
        while (remaining > 0 && !eof) {
            // 创建一个临时缓冲区来消费剩余数据
            byte[] buffer = new byte[(int) Math.min(remaining, TEMP_BUFFER_SIZE)];
            int bytesRead = next.read(buffer);

            if (bytesRead == -1) {
                throw new IOException("Unexpected EOF: expected " + remaining + " more bytes");
            }

            remaining -= bytesRead;
            totalBytesRead += bytesRead;
        }

        eof = true;
    }

    /**
     * 重置过滤器状态，为下一次使用做准备
     */
    @Override
    public void recycle() {
        remaining = 0;
        contentLength = 0;
        eof = false;
        totalBytesRead = 0;
    }
    //</editor-fold>

    //<editor-fold desc = "其他方法">
    /**
     * 设置新的Content-Length（用于复用时）
     *
     * @param contentLength 新的Content-Length值
     */
    public void setContentLength(long contentLength) {
        if (contentLength < 0) {
            throw new IllegalArgumentException("Content-Length cannot be negative: " + contentLength);
        }
        this.contentLength = contentLength;
        this.remaining = contentLength;
        this.eof = (contentLength == 0);
        this.totalBytesRead = 0;
    }

    /**
     * 获取原始的Content-Length值
     *
     * @return Content-Length值
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * 获取剩余未读字节数
     *
     * @return 剩余字节数
     */
    public long getRemaining() {
        return remaining;
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
     * 检查是否已读取完所有数据
     *
     * @return true如果已读取完毕
     */
    public boolean isFinished() {
        return eof || remaining == 0;
    }
    //</editor-fold>
}