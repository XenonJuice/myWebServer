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
    private byte[] buf;
    // 当前有效数据在 buf 中的起始位置，用于标记“已消费”或“待读取”数据的起始偏移
    // append 方法会在 buf[end] 写入新字节，然后 end++，
    private int start = 0;
    // 当前有效数据在 buf 中的结束位置，buf[start..end-1] 范围内存放的都是尚未被业务取走的有效字节。
    // 当 append(byte b) 或 append(byte[] b, …) 时，会将新字节写到 buf[end]，并将 end 增加 len。
    private int end = 0;
    // 缓冲区的容量限制（可能小于buf.length，用于控制实际可用空间）
    private int limit;
    // 是否允许自动扩容
    private boolean autoExpand = false;
    // 最大容量限制（防止无限扩容）
    private static final int MAX_CAPACITY = 1024 * 1024;
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
        this.limit = capacity;
    }

    /**
     * 构造一个支持自动扩容的 ByteChunk。
     *
     * @param initialCapacity 初始容量
     * @param autoExpand      是否允许自动扩容
     */
    public ByteChunk(int initialCapacity, boolean autoExpand) {
        this.buf = new byte[initialCapacity];
        this.limit = initialCapacity;
        this.autoExpand = autoExpand;
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
     * 获取有效数据的起始位置
     */
    public int getStart() {
        return start;
    }

    /**
     * 获取有效数据的结束位置
     */
    public int getEnd() {
        return end;
    }

    /**
     * 获取当前容量限制
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 设置容量限制（不能超过实际缓冲区大小）
     */
    public void setLimit(int limit) {
        if (limit > buf.length) {
            throw new IllegalArgumentException("限制不能超过缓冲区大小");
        }
        this.limit = limit;
    }

    /**
     * 获取剩余可用空间
     */
    public int getAvailable() {
        return limit - end;
    }

    /**
     * 压缩缓冲区：将有效数据移动到缓冲区开头
     * 这在需要腾出更多空间时很有用
     */
    public void compact() {
        if (start == 0) {
            // 已经在开头，无需压缩
            return;
        }

        int len = getLength();
        if (len > 0) {
            System.arraycopy(buf, start, buf, 0, len);
        }
        start = 0;
        end = len;
    }

    /**
     * 将单个字节追加到容器末尾。
     *
     * @param b 要追加的字节
     */
    public void append(byte b) {
        // 检查是否需要扩容或压缩
        if (end >= limit) {
            if (autoExpand) {
                expand(1);
            } else if (start > 0) {
                compact(); // 尝试压缩以获得空间
            }
            if (end >= limit) {
                throw new IndexOutOfBoundsException("ByteChunk overflow");
            }
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
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length");
        }
        // 检查是否需要扩容或压缩
        if (end + len > limit) {
            if (autoExpand) {
                expand(len);
            } else if (start > 0) {
                compact(); // 尝试压缩以获得空间
            }
            if (end + len > limit) {
                throw new IndexOutOfBoundsException("ByteChunk overflow: need " + len + " bytes, available " + getAvailable());
            }
        }
        System.arraycopy(b, off, buf, end, len);
        end += len;
    }

    /**
     * 扩容内部缓冲区
     *
     * @param minCapacity 需要的最小额外容量
     */
    private void expand(int minCapacity) {
        if (!autoExpand) {
            throw new IllegalStateException("自动扩展未启用");
        }
        // 先尝试压缩
        compact();
        int needed = end + minCapacity;
        if (needed <= limit) {
            // 压缩后空间足够
            return;
        }

        // 计算新容量：当前容量的1.5倍或所需容量，取较大值
        int newCapacity = Math.max(buf.length + (buf.length >> 1), needed);
        newCapacity = Math.min(newCapacity, MAX_CAPACITY);

        if (newCapacity <= buf.length) {
            throw new IndexOutOfBoundsException("无法超出最大容量扩展");
        }

        byte[] newBuf = new byte[newCapacity];
        System.arraycopy(buf, start, newBuf, 0, getLength());
        end = getLength();
        start = 0;
        buf = newBuf;
        limit = newCapacity;
    }

    /**
     * 消费指定数量的字节（移动start指针）
     *
     * @param n 要消费的字节数
     */
    public void consume(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("无法消耗负字节");
        }
        if (n > getLength()) {
            throw new IllegalArgumentException("无法消耗超过可用字节的数量");
        }
        start += n;
        if (start == end) {
            // 所有数据都已消费，重置到开头
            start = 0;
            end = 0;
        }
    }

    /**
     * 将当前有效数据（buf[start..end-1]）复制到 dest[startDest..]
     *
     * @param dest 要被写入的目标数组
     * @param off  写入目标数组时的起始偏移
     * @return 复制的字节数（等于 getLength()）
     */
    public int getBytes(byte[] dest, int off) {
        if (dest == null) throw new NullPointerException("Destination array is null");
        int len = getLength();
        if (off < 0 || off + len > dest.length)
            throw new IndexOutOfBoundsException("Invalid offset/length for destination");
        if (len > 0) System.arraycopy(buf, start, dest, off, len);
        return len;
    }

    /**
     * 获取指定位置的字节（相对于start）
     *
     * @param index 相对于start的索引
     * @return 指定位置的字节
     */
    public byte getByte(int index) {
        if (index < 0 || index >= getLength()) {
            throw new IndexOutOfBoundsException("索引超出范围： " + index);
        }
        return buf[start + index];
    }

    /**
     * 清空当前内容，将 start 和 end 都重置到 0。下一次使用时相当于一个空容器。
     */
    public void recycle() {
        start = 0;
        end = 0;
    }

    /**
     * 完全重置ByteChunk到初始状态
     *
     * @param newCapacity 新的容量（如果为0则保持当前容量）
     */
    public void reset(int newCapacity) {
        start = 0;
        end = 0;
        if (newCapacity > 0 && newCapacity != buf.length) {
            buf = new byte[newCapacity];
            limit = newCapacity;
        }
    }

    /**
     * 将ByteChunk的内容转换为字符串（用于调试）
     *
     * @param charset 字符集
     * @return 字符串表示
     */
    public String toString(String charset) {
        if (getLength() == 0) {
            return "";
        }
        try {
            return new String(buf, start, getLength(), charset);
        } catch (Exception e) {
            return new String(buf, start, getLength());
        }
    }

    @Override
    public String toString() {
        return "ByteChunk[start=" + start + ", end=" + end + ", limit=" + limit + ", length=" + getLength() + "]";
    }

    //</editor-fold>
}
