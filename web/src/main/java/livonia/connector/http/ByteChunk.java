package livonia.connector.http;

/**
 * 一个简单的字节容器，用于存放从FilterChain中解码/读取出来的数据。
 * 存放从FilterChain中解码/读取出来的数据
 *
 * @author LILINJIAN
 * @version 2025/06/01
 */
public class ByteChunk {
    //<editor-fold desc = "attr">
    // ByteChunk 内部使用的字节数组缓冲区，用于存放解码或读取出来的有效数据
    private final byte[] buf;
    // 当前有效数据在 buf 中的起始位置，用于标记“已消费”或“待读取”数据的起始偏移
    // append 方法会在 buf[end] 写入新字节，然后 end++，
    private int start = 0;
    // 当前有效数据在 buf 中的结束位置，buf[start..end-1] 范围内存放的都是尚未被业务取走的有效字节。
    // 当 append(byte b) 或 append(byte[] b, …) 时，会将新字节写到 buf[end]，并将 end 增加 len。
    private int end = 0;
    //
    //</editor-fold>
    //<editor-fold desc = "构造器">
    /**
     * 构造一个固定容量的 ByteChunk。
     *
     * @param capacity 容量大小（字节数），例如 8192
     */
    public ByteChunk(int capacity) {
        this.buf = new byte[capacity];
    }
    //</editor-fold>
    //<editor-fold desc = "方法">
    /**
     * 返回当前有效数据的长度 (end - start)。
     */
    public int getLength() {
        return end - start;
    }

    /**
     * 返回内部的字节数组引用。有效数据位于 buf[start..end-1] 范围内。
     */
    public byte[] getBuffer() {
        return buf;
    }

    /**
     * 将单个字节追加到容器末尾。append 之前务必保证 getLength() < buf.length。
     *
     * @param b 要追加的字节
     */
    public void append(byte b) {
        if (end >= buf.length) {
            throw new IndexOutOfBoundsException("ByteChunk overflow");
        }
        buf[end++] = b;
    }

    /**
     * 将 byte[] 中从 off 开始的 len 个字节追加到容器末尾。
     *
     * @param b   源数组
     * @param off 源数组的起始偏移
     * @param len 要复制的字节数
     */
    public void append(byte[] b, int off, int len) {
        if (b == null) throw new NullPointerException("Source array is null");
        if (off < 0 || len < 0 || off + len > b.length) throw new IndexOutOfBoundsException("Invalid offset/length");
        if (end + len > buf.length) throw new IndexOutOfBoundsException("ByteChunk overflow");
        System.arraycopy(b, off, buf, end, len);
        end += len;
    }

    /**
     * 将当前有效数据（buf[start..end-1]）复制到 dest[startDest..]
     *
     * @param dest 要被写入的目标数组
     * @param off  写入目标数组时的起始偏移
     * @return     复制的字节数（等于 getLength()）
     */
    public int getBytes(byte[] dest, int off) {
        if (dest == null) throw new NullPointerException("Destination array is null");
        int len = getLength();
        if (off < 0 || off + len > dest.length) throw new IndexOutOfBoundsException("Invalid offset/length for destination");
        System.arraycopy(buf, start, dest, off, len);
        return len;
    }

    /**
     * 清空当前内容，将 start 和 end 都重置到 0。下一次使用时相当于一个空容器。
     */
    public void recycle() {
        start = 0;
        end = 0;
    }

    //</editor-fold>
}
