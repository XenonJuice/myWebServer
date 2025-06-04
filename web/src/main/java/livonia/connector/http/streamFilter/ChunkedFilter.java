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
    // 最大chunk大小行长度
    private static final int MAX_CHUNK_SIZE_LINE_LENGTH = 4096;
    // 最大trailer头部大小
    private static final int MAX_TRAILER_SIZE = 8192;
    // 临时缓冲区大小
    private static final int TEMP_BUFFER_SIZE = 1024;

    // 上游流：可能是 SocketInputBuffer 或另一个 InputFilter 的包装流。
    private final InputStream next;
    // 临时缓冲区（用于读取单个字节）
    private final byte[] singleByte = new byte[1];
    // 当前正要处理的chunk中剩下还没读的字节数。当需要读取一个新的 chunk 时，这个值为 0。
    private int remainingInChunk = 0;
    // 当已经读取到最后一个 chunk（长度为 0）并跳过尾部后，标志流已结束。
    private boolean eof = false;
    // 是否需要跳过chunk末尾的CRLF
    private boolean needSkipCRLF = false;
    // 统计信息
    private long totalBytesRead = 0;
    private int chunksProcessed = 0;
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
     * 从上游流读取并解码 chunked 编码的数据，将解码后的明文字节追加到 chunk 中
     *
     * @param chunk 用于存放解码后的字节
     * @return 写入 chunk 的字节数；若到达流尾，返回 -1
     * @throws IOException 如果读取或解码过程中出现 I/O 错误
     */
    @Override
    public int doRead(ByteChunk chunk) throws IOException {
        if (eof) {
            return -1;
        }

        // 如果需要跳过上一个chunk末尾的CRLF
        if (needSkipCRLF) {
            skipCRLF();
            needSkipCRLF = false;
        }

        // 如果当前没有剩余的chunk内容，读取新chunk的长度
        if (remainingInChunk == 0) {
            String sizeLine = readChunkSizeLine();
            if (sizeLine == null) {
                eof = true;
                return -1;
            }

            // 解析chunk大小
            int chunkSize = parseChunkSize(sizeLine);

            if (chunkSize == 0) {
                // 最后一个chunk，处理可能的trailer headers
                parseTrailers();
                eof = true;
                return -1;
            }

            remainingInChunk = chunkSize;
            chunksProcessed++;
        }

        // 读取chunk数据
        int toRead = Math.min(remainingInChunk, chunk.getAvailable());
        if (toRead <= 0) {
            return 0; // chunk缓冲区已满
        }

        // 创建临时缓冲区
        byte[] buffer = new byte[Math.min(toRead, TEMP_BUFFER_SIZE)];
        int totalRead = 0;

        // 循环读取直到达到所需字节数或遇到EOF
        while (totalRead < toRead) {
            int remaining = toRead - totalRead;
            int bytesToRead = Math.min(remaining, buffer.length);

            int bytesRead = next.read(buffer, 0, bytesToRead);
            if (bytesRead == -1) {
                // 意外的EOF
                throw new IOException("Unexpected EOF while reading chunk data");
            }

            chunk.append(buffer, 0, bytesRead);
            totalRead += bytesRead;
            remainingInChunk -= bytesRead;
            totalBytesRead += bytesRead;
        }

        // 如果读完了当前chunk，标记需要跳过末尾的CRLF
        if (remainingInChunk == 0) {
            needSkipCRLF = true;
        }

        return totalRead;
    }

    /**
     * 解析trailer headers
     *
     * @throws IOException 如果读取失败
     */
    private void parseTrailers() throws IOException {
        // 读取trailer headers直到遇到空行
        int totalSize = 0;

        while (totalSize < MAX_TRAILER_SIZE) {
            String line = readTrailerLine();
            if (line == null) {
                throw new IOException("Unexpected EOF while reading trailers");
            }

            if (line.isEmpty()) {
                // 空行表示trailer结束
                break;
            }

            totalSize += line.length() + 2; // 加上CRLF

            // 这里可以处理trailer headers，但通常被忽略
            // 可以添加回调或存储trailer的逻辑
        }

        if (totalSize >= MAX_TRAILER_SIZE) {
            throw new IOException("Trailer headers too large (max " + MAX_TRAILER_SIZE + " bytes)");
        }
    }

    /**
     * 读取trailer行
     *
     * @return trailer行（不包含CRLF），如果遇到EOF返回null
     * @throws IOException 如果读取失败
     */
    private String readTrailerLine() throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        boolean foundCR = false;

        while (true) {
            int b = next.read();

            if (b == -1) {
                return null;
            }

            if (foundCR) {
                if (b == '\n') {
                    // 找到CRLF
                    byte[] bytes = lineBuffer.toByteArray();
                    if (bytes.length == 1) {
                        // 只有CR，表示空行
                        return "";
                    }
                    return new String(bytes, 0, bytes.length - 1, StandardCharsets.ISO_8859_1);
                } else {
                    // CR后面不是LF
                    throw new IOException("Invalid trailer encoding: CR not followed by LF");
                }
            }

            lineBuffer.write(b);
            foundCR = (b == '\r');
        }
    }

    /**
     * 结束处理，确保所有数据都已读取
     */
    @Override
    public void end() throws IOException {
        // 如果还没有到达EOF，消费剩余的所有数据
        while (!eof) {
            ByteChunk dummy = new ByteChunk(1024);
            if (doRead(dummy) == -1) {
                break;
            }
        }
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
        needSkipCRLF = false;
        totalBytesRead = 0;
        chunksProcessed = 0;
    }

    /**
     * 读取chunk大小行
     *
     * @return chunk大小行（不包含CRLF），如果遇到EOF返回null
     * @throws IOException 如果读取失败或行太长
     */
    private String readChunkSizeLine() throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        boolean foundCR = false;

        while (lineBuffer.size() < MAX_CHUNK_SIZE_LINE_LENGTH) {
            int b = next.read();

            if (b == -1) {
                if (lineBuffer.size() == 0) {
                    return null; // 正常的EOF
                }
                throw new IOException("Unexpected EOF while reading chunk size");
            }

            if (foundCR) {
                if (b == '\n') {
                    // 找到CRLF，返回行内容（不包含CR）
                    byte[] bytes = lineBuffer.toByteArray();
                    return new String(bytes, 0, bytes.length - 1, StandardCharsets.ISO_8859_1);
                } else {
                    // CR后面不是LF，这是错误格式
                    throw new IOException("Invalid chunk encoding: CR not followed by LF");
                }
            }

            lineBuffer.write(b);
            foundCR = (b == '\r');
        }

        throw new IOException("Chunk size line too long (max " + MAX_CHUNK_SIZE_LINE_LENGTH + " bytes)");
    }

    /**
     * 解析chunk大小
     *
     * @param sizeLine chunk大小行
     * @return chunk大小
     * @throws IOException 如果格式无效
     */
    private int parseChunkSize(String sizeLine) throws IOException {
        if (sizeLine == null || sizeLine.isEmpty()) {
            throw new IOException("Empty chunk size line");
        }

        // 去掉可能的chunk扩展（分号及以后的内容）
        int semicolonIndex = sizeLine.indexOf(SEMICOLON);
        String hexSize = (semicolonIndex >= 0)
                ? sizeLine.substring(0, semicolonIndex).trim()
                : sizeLine.trim();

        if (hexSize.isEmpty()) {
            throw new IOException("Empty chunk size");
        }

        try {
            int size = Integer.parseInt(hexSize, 16);
            if (size < 0) {
                throw new IOException("Negative chunk size: " + size);
            }
            return size;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid chunk size: " + hexSize, e);
        }
    }

    /**
     * 跳过CRLF
     *
     * @throws IOException 如果读取失败或格式错误
     */
    private void skipCRLF() throws IOException {
        int cr = next.read();
        if (cr == -1) {
            throw new IOException("Unexpected EOF while expecting CRLF");
        }
        if (cr != '\r') {
            throw new IOException("Invalid chunk encoding: expected CR, got " + cr);
        }

        int lf = next.read();
        if (lf == -1) {
            throw new IOException("Unexpected EOF while expecting LF");
        }
        if (lf != '\n') {
            throw new IOException("Invalid chunk encoding: expected LF after CR, got " + lf);
        }
    }
    //</editor-fold>

    //<editor-fold desc = "统计方法">

    /**
     * 获取已读取的总字节数
     *
     * @return 总字节数
     */
    public long getTotalBytesRead() {
        return totalBytesRead;
    }

    /**
     * 获取已处理的chunk数量
     *
     * @return chunk数量
     */
    public int getChunksProcessed() {
        return chunksProcessed;
    }
    //</editor-fold>
}
