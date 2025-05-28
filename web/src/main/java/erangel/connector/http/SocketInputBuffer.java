package erangel.connector.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * 带内部缓冲区的输入流，用于先读取请求行和头，
 */
public class SocketInputBuffer extends InputStream {
    //<editor-fold desc = "attr">
    // socket.getInputStream()
    private final InputStream socketInputStream;
    // 内部缓冲区
    private final byte[] innerBuffer;
    // 缓冲区有效字节数
    public int bufferCount = 0;
    // 内部缓冲中下一个应该读取的位置
    private int pos = 0;

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
        // 如果当前的位置大于等于内部缓冲的有效字节数，说明要从底层流向内部缓冲填充数据
        if (pos >= bufferCount) {
            fillBuffer();
            // 若填充之后内部缓冲大小小于等于0 则说明底层流已被榨干
            if (bufferCount <= 0) return -1;
        }
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
        if (off < 0 || len < 0 || off + len > buffer.length) {
            throw new IndexOutOfBoundsException();
        }
//        // 标记相对于需要获取长度之中的当前位置。
//        int i = 0;
//        // 如果当前位置依然小于需要获取的长度，则继续读。
//        while (i < len) {
//            int count = read();
//            // 若读取到的流的末尾
//            if (count < 0) {
//                // 判断一开始就读到末尾和读到一部分之后才读到末尾的情况，若i=0 则说明
//                // 一开始就读到了流的末尾；若i不为0，则说明读到了一部分数据才达到流的末尾。
//                return i == 0 ? -1 : i;
//            }
//            buffer[off + i] = (byte) count;
//            i++;
//        }

        // 更高效的做法，既然我们需要取出一块较大数据。那么直接先判断内部缓冲区和所需数据的大小
        // 如果缓冲区的可用字节大于等于所需大小，可以直接把所需大小复制到容器数组中
        int available = available();
        if (available > 0) {
            if (available >= len) {
                //                src,      srcPos, dest, destPos,length
                System.arraycopy(innerBuffer, pos, buffer, off, len);
                // 更新内部缓冲中下一次读取的位置
                pos += len;
                return len;
            }
            // 如果数据不够用，则把先把内部缓冲中的数据取出来
            System.arraycopy(innerBuffer, pos, buffer, off, available);
            pos += available;
            // 然后再把需要的大小读进来
            int i = socketInputStream.read(buffer, off, len - available);
            if (i == -1) return available;
            return available + i;
        }
        // 若内部缓存没有数据，则直接从底层读取
        return socketInputStream.read(buffer, off, len);
    }

    // 填充缓冲区，从底层流读取数据
    public void fillBuffer() throws IOException {
        pos = 0;
        bufferCount = socketInputStream.read(innerBuffer, 0, innerBuffer.length);
    }

    /**
     * 返回可以从此输入流读取（或跳过）不会阻塞的可用字节数。
     *
     * @return 不会阻塞的可以读取或跳过的字节数。
     * @throws IOException 如果发生 I/O 错误。
     **/
    @Override
    public int available() throws IOException {
        return bufferCount - pos + socketInputStream.available();
    }
    //</editor-fold>

    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
