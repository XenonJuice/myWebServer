package livonia.connector.http.streamFilter;

import livonia.connector.http.ByteChunk;

import java.io.IOException;
import java.io.InputStream;

public class FilterChainInputStream extends InputStream {

    //<editor-fold desc = "attr">
    /**
     * 拦截器链最顶层的过滤器。业务读取时会调用它的 doRead 方法，
     * 并传入一个 ByteChunk 用于存放解码后的数据。
     */
    private final InputFilter topFilter;
    //</editor-fold>

    //<editor-fold desc = "构造器">
    /**
     * 构造时传入“顶层过滤器”实例。例如： new PassthroughFilter(socketInputBuffer)。
     *
     * @param topFilter 最顶层的 InputFilter
     */
    public FilterChainInputStream(InputFilter topFilter) {
        this.topFilter = topFilter;
    }
    //</editor-fold>
    //<editor-fold desc = "抽象方法实现">
    /**
     * 单字节读取。委托给 read(b, off, len) 实现。
     */
    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        return (n <= 0) ? -1 : (one[0] & 0xFF);
    }

    /**
     * 从过滤器链中读取至多 len 个字节，并写入到 b[off..off+len-1]。
     *   1. 创建一个容量为 len 的 ByteChunk。
     *   2. 调用 topFilter.doRead(chunk)，将解码后的字节放入 chunk 中。
     *   3. 如果 doRead 返回 -1，表示 EOF，直接返回 -1。
     *   4. 否则，将 chunk 中的字节复制到目标数组 b，并返回复制的字节数。
     *
     * @param b   目标字节数组
     * @param off 写入时的起始偏移
     * @param len 最多读取的字节数
     * @return 读取到的字节数；如果到达流末尾，返回 -1
     * @throws IOException 如果读取或解码过程中发生 I/O 错误
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException("Destination array is null");
        if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException("Invalid offset/length");
        // 用 len 大小创建一个临时 ByteChunk，用于存放顶层过滤器的输出
        ByteChunk chunk = new ByteChunk(len);
        // 调用顶层过滤器读取并解码数据到 chunk 中
        int n = topFilter.doRead(chunk);
        if (n <= 0) {
            // 到达流末尾或上游无数据
            return -1;
        }
        // 将解码后的字节从 chunk 复制到 b[off...off+n-1]
        chunk.getBytes(b, off);
        // 重置 chunk，以便复用（如果后续还有读取）
        chunk.recycle();
        return n;
    }
    //</editor-fold>

    //<editor-fold desc = "其他方法">
    /**
     * 当业务层认为“请求体已读完”时，需要调用此方法通知过滤器链写入任何必要的尾部定界符，
     * 例如分块编码要在流尾写入 "0\r\n\r\n"。如果过滤器不需要尾部（如 PassthroughFilter），则此方法无操作。
     *
     * @throws IOException 如果写尾部时发生 I/O 错误
     */
    public void end() throws IOException {
        topFilter.end();
    }

    /**ƒ
     * 复用此 FilterChainInputStream 之前，需要调用此方法让所有过滤器重置状态。
     * 例如 ChunkedFilter 会在这里将其内部状态（remainingLength、eof 等）归零，
     * 以便下一个请求再次使用。
     */
    public void recycle() {
        topFilter.recycle();
    }
    //</editor-fold>

}
