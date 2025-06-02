package livonia.connector.http.streamFilter;

import livonia.connector.http.ByteChunk;

import java.io.IOException;
import java.io.InputStream;

public class PassthroughFilter implements InputFilter {
    //<editor-fold desc = "attr">
    // 上游流：可以是另一个 InputFilter，
    // 也可以直接是 SocketInputBuffer。
    private final InputStream next;
    //</editor-fold>
    //<editor-fold desc = "构造器">
    /**
     * 构造时传入上游流。
     *
     * @param next 上游 InputStream（可能来自上一个 Filter，或是底层的 SocketInputBuffer）
     */
    public PassthroughFilter(InputStream next) {
        this.next = next;
    }
    //</editor-fold>
    //<editor-fold desc = "接口实现">
    /**
     * 返回此 Filter 的编码名称：passthrough 表示不做任何编码/解码。
     */
    @Override
    public String getEncodingName() {
        return "passthrough";
    }

    /**
     * 直接从上游流读取原始字节，然后将它们追加到 chunk 中。
     * 如果上游返回 -1，则表示流末尾，返回 -1。
     *
     * @param chunk 用于存放读取到的字节
     * @return 实际写入 chunk 的字节数；如果上游流已到末尾，则返回 -1
     * @throws IOException 如果从上游读取失败
     */
    @Override
    public int doRead(ByteChunk chunk) throws IOException {
        // 临时缓冲，用于从上游流读取数据
        byte[] tmp = new byte[1024];
        int n = next.read(tmp);
        if (n <= 0) {
            // 上游流已结束，返回 -1
            return -1;
        }
        // 将上游读到的原始字节追加到 chunk 中
        chunk.append(tmp, 0, n);
        return n;
    }

    /**
     * passthrough 过滤器不需要写任何尾部定界符，方法留空
     */
    @Override
    public void end() throws IOException {
        // no op
    }

    /**
     * passthrough 过滤器没有内部状态需要复位，方法留空。
     */
    @Override
    public void recycle() {
        // no op
    }
    //</editor-fold>
}
