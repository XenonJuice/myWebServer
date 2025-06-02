package livonia.connector.http.streamFilter;

import livonia.connector.http.ByteChunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static livonia.base.Const.PunctuationMarks.SEMICOLON;

/**
 * 一个用于解析 HTTP/1.1 分块传输编码（chunked）的过滤器。
 * 它从上游 InputStream 中先读取一行十六进制长度，解析出本次 chunk 的大小，
 * 然后再读取对应的正文数据，最后跳过 chunk 末尾的 CRLF (\"\\r\\n\")。
 * 当读到长度为 0 时，再跳过最后的尾部 CRLFCRLF，并返回 EOF。
 *
 * @author LILINJIAN
 * @version 2025/06/02
 */
public class ChunkedFilter implements InputFilter {
    //<editor-fold desc = "attr">
    // 上游流：可能是 SocketInputBuffer 或另一个 InputFilter 的包装流。
    private final InputStream next;
    // 当前正要处理的chunk中剩下还没读的字节数。当需要读取一个新的 chunk 时，这个值为 0。
    private int remainingInChunk = 0;
    // 当已经读取到最后一个 chunk（长度为 0）并跳过尾部后，标志流已结束。
    private boolean eof = false;
    //</editor-fold>
    //<editor-fold desc = "构造器">

    /**
     * 构造时传入上游流。
     *
     * @param next 上游 InputStream，例如 SocketInputBuffer 的返回流
     */
    public ChunkedFilter(InputStream next) {
        this.next = next;
    }
    //</editor-fold>
    //<editor-fold desc = "接口实现">

    /**
     * 返回此 Filter 的编码名称：chunked。
     */
    @Override
    public String getEncodingName() {
        return "chunked";
    }

    /**
     * 从上游流读取并解码 chunked 编码的数据，将解码后的明文字节追加到 chunk 中。
     * <p>
     * 实现逻辑：
     * 1. 如果 eof 已为 true，返回 -1。
     * 2. 如果 remainingInChunk == 0，需要先读取一行 CRLF 结尾的十六进制数字，
     * 得到本次 chunk 的长度 L。
     * - 如果 L == 0，表示这是最后一个 chunk，随后还会有一个空行(即 CRLF)，
     * 跳过后标记 eof=true，返回 -1。
     * - 否则将 remainingInChunk=L。
     * 3. 此时 remainingInChunk > 0，从上游一次性读取 min(remainingInChunk, chunk 容量) 个字节，
     * 追加到 chunk 中，remainingInChunk -= 实际读取字节数。
     * 4. 如果 remainingInChunk 读到 0，就跳过本 chunk 末尾的 CRLF(2 字节)，准备下一次再读新的 chunk 长度行。
     * 5. 将实际向 chunk 写入的字节数返回。如果读取过程中碰到 EOF（上游返回 -1），则返回 -1。
     * <p>
     * 例子：
     * 5\r\n
     * Hello\r\n
     * 5\r\n
     * World\r\n
     * 0\r\n
     * \r\n
     * <p>
     *
     * @param chunk 用于存放解码后的字节
     * @return 写入 chunk 的字节数；若到达流尾，返回 -1
     * @throws IOException 如果读取或解码过程中出现 I/O 错误
     */
    @Override
    public int doRead(ByteChunk chunk) throws IOException {
        if (eof) return -1;

        // 1. 如果当前没有剩余的 chunk 内容，需要先读取新 chunk 长度行
        if (remainingInChunk == 0) {
            String sizeLine = readLineFromUpstream();
            if (sizeLine == null) {
                // 上游流已结束，但未见到“0”结尾，视为 EOF
                eof = true;
                return -1;
            }
            // 大写并去掉可能的扩展信息（分号及以后）
            int semicolonIndex = sizeLine.indexOf(SEMICOLON);
            String hexLength = (semicolonIndex >= 0)
                    ? sizeLine.substring(0, semicolonIndex).trim()
                    : sizeLine.trim();
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(hexLength, 16);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid chunk size: " + hexLength);
            }

            if (chunkSize == 0) {
                // 最后一个块：跳过 “0\r\n” 之后的那一个空行 "\r\n"
                skipCRLF();       // 跳过 “0” 行结尾的 CRLF
                skipCRLF();       // 跳过最后的空行 CRLFCRLF 中的第二对
                eof = true;
                return -1;
            }
            // 否则设定 remainingInChunk
            remainingInChunk = chunkSize;
        }

        // 2. 当前 remainingInChunk > 0，从上游读取数据
        int freeSpace = chunk.getBuffer().length - chunk.getLength();
        int maxToRead = Math.min(remainingInChunk, freeSpace);
        byte[] tmp = new byte[maxToRead];
        int n = next.read(tmp, 0, maxToRead);
        if (n <= 0) {
            // 上游提前结束
            eof = true;
            return -1;
        }
        // 把读到的字节追加到 chunk
        chunk.append(tmp, 0, n);
        remainingInChunk -= n;

        // 3. 如果刚好读完了本次 chunk，跳过本 chunk 末尾的 CRLF(2 字节)
        if (remainingInChunk == 0) {
            skipCRLF();
        }

        return n;
    }

    /**
     * 当所有 chunk 都读完后，如果还需写尾部定界符，通常在此写入。
     * 由于 read() 已经在读到最后一个 chunk (size=0) 时跳过了尾部 CRLFCRLF，
     * 所以这里不再需要额外写尾部，留空即可。
     */
    @Override
    public void end() throws IOException {
        // no-op
    }

    /**
     * 重置状态，以便复用：
     * - remainingInChunk 设为 0
     * - eof 设为 false
     */
    @Override
    public void recycle() {
        remainingInChunk = 0;
        eof = false;
    }

    /**
     * 从上游 InputStream 一次性读取一行，直到 CRLF 为止（返回结果中不含 CRLF）
     * 如果上游在此过程返回 -1，视为 EOF，返回 null
     *
     * @return 读取到的一行字符串（无 CRLF），如果 EOF，则返回 null
     * @throws IOException 如果读取失败
     */
    private String readLineFromUpstream() throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        int prev = -1;
        while (true) {
            int b = next.read();
            if (b == -1) {
                // 上游流已结束
                return null;
            }
            if (prev == '\r' && b == '\n') {
                // 遇到 CRLF，返回 line（去掉末尾的 \r）
                byte[] raw = lineBuffer.toByteArray();
                // raw 里最后一个字节对应的就是 CR 字符，所以去掉它
                return new String(raw, 0, raw.length - 1, StandardCharsets.UTF_8);
            }
            lineBuffer.write(b);
            prev = b;
        }
    }

    /**
     * 丢弃上游流中的一对 CRLF（共 2 个字节）。如果遇到 EOF，则立即返回。
     */
    private void skipCRLF() throws IOException {
        int b1 = next.read();
        if (b1 == -1) {
            return;
        }
        int b2 = next.read();
        // 无论 b2 是否为 -1，直接返回即可
    }
    //</editor-fold>

}
