import livonia.connector.http.SocketInputBuffer;
import livonia.connector.http.streamFilter.FilterChainInputStream;
import livonia.connector.http.streamFilter.PassthroughFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 测试类：演示 SocketInputBuffer + PassthroughFilter + FilterChainInputStream 的协同工作
 */
public class FilterChainTest {

    public static void main(String[] args) {
        try {
            // 构造一个“假”的 HTTP 请求，包含请求行、头部和正文
            String rawRequest =
                    """
                            GET /hello HTTP/1.1\r
                            Host: example.com\r
                            Content-Length: 11\r
                            \r
                            hello world""";
            byte[] rawBytes = rawRequest.getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream fakeSocket = new ByteArrayInputStream(rawBytes);

            // 将假 SocketInputStream 包装到 SocketInputBuffer（缓冲区大小设为 16KB）
            SocketInputBuffer socketBuffer = new SocketInputBuffer(fakeSocket, 16 * 1024);

            // 用 PassthroughFilter 包装 SocketInputBuffer
            PassthroughFilter passthrough = new PassthroughFilter(socketBuffer);

            // 用 FilterChainInputStream 将 PassthroughFilter 封装为标准 InputStream
            FilterChainInputStream fin = new FilterChainInputStream(passthrough);

            // 业务层循环读取，直到 EOF
            byte[] buffer = new byte[128];
            int totalRead = 0;
            int bytesRead;
            while ((bytesRead = fin.read(buffer, totalRead, buffer.length - totalRead)) > 0) {
                totalRead += bytesRead;
            }

            // 将读取到的所有字节转换为字符串，并打印
            String result = new String(buffer, 0, totalRead, StandardCharsets.UTF_8);
            System.out.println("业务层读到的全部内容：");
            System.out.println("---------------------------------");
            System.out.println(result);
            System.out.println("---------------------------------");

            // 通知链写入尾部（PassthroughFilter 无实际操作，但为了一致性调用）
            fin.end();

            // 复用前可以调用 recycle()
            fin.recycle();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}