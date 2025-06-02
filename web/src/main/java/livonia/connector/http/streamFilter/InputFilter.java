package livonia.connector.http.streamFilter;


import livonia.connector.http.ByteChunk;

import java.io.IOException;

/**
 * 一个可插拔的输入过滤器，用于对上游流数据进行不同的解码处理。
 * @author LILINJIAN
 * @version 2025/06/02
 */
public interface InputFilter {
    /**
     * 返回此 Filter 支持的 Transfer-Encoding 名称，
     * 例如 "chunked"、"gzip" 等。
     */
    String getEncodingName();

    /**
     * 从上游流（下一个 Filter 或 SocketInputBuffer）读取并解码数据，
     * 将解码后的字节追加到 ByteChunk 中。
     *
     * @param chunk 用于存放解码后的字节
     * @return 实际写入 chunk 的字节数；如果到达流末尾，应返回 -1
     * @throws IOException 如果读取或解码失败
     */
    int doRead(ByteChunk chunk) throws IOException;

    /**
     * 当业务认为“读完请求体”或需要写入尾部定界符时调用，
     * 例如分块编码需要写入 "0\r\n\r\n"。如果无需写尾部，此方法可以留空。
     *
     * @throws IOException 如果写尾部过程出错
     */
    void end() throws IOException;

    /**
     * 在复用此 Filter 处理下一个请求之前调用，
     * 重置内部所有状态，以便重复使用。
     */
    void recycle();

}
