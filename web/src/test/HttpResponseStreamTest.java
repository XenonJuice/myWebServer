import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.connector.http.HttpResponseStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模拟 Servlet 流式传输内容测试
 */
@Deprecated
public class HttpResponseStreamTest {

    private HttpResponse response;
    private HttpResponseStream responseStream;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        response = new HttpResponse();
        outputStream = new ByteArrayOutputStream();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setProtocol("HTTP/1.1");
        httpRequest.setUri("/test/resource");
        httpRequest.setMethod("GET");
        httpRequest.setHeaders(createMockHeaders());
        httpRequest.setResponse(response);
        response.setRequest(httpRequest);
        response.setStream(outputStream); // 将模拟流设置到响应对象中
    }

    private Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/plain"));
        headers.put("User-Agent", Collections.singletonList("MockTestClient/1.0"));
        return headers;
    }

    // 测试：流式传输内容 + 间隔1秒
    @Test
    void testStreamedContentWithIntervals() throws IOException, InterruptedException {
        // 配置
        response.setHeader("Content-Type", "text/plain");

        // 模拟向 Servlet 写入流式数据
        ServletOutputStream outputStream = response.getOutputStream();

        // 开始写入
        outputStream.write("Message Part 1\n".getBytes());
        response.flushBuffer(); //
        Thread.sleep(1000); // 模拟间隔1秒
        response.flushBuffer(); //
        outputStream.write("Message Part 2\n".getBytes());
        Thread.sleep(1000); // 再次间隔1秒
        outputStream.write("Message Part 3\n".getBytes());
        response.flushBuffer(); // 提交缓冲区
        response.finishResponse();
        // 检查响应结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证响应行和头部信息
        assertTrue(result.contains("HTTP/1.1 200 OK")); // 响应行
        assertTrue(result.contains("Content-Type: text/plain")); // Content-Type 响应头

        // 验证内容只包含 1 次响应头，且未重复
        assertEquals(1, getOccurrence(result));

        // 验证正文写入是否完整
        assertTrue(result.contains("Message Part 1\n"));
        assertTrue(result.contains("Message Part 2\n"));
        assertTrue(result.contains("Message Part 3\n"));
    }

    @Test
    void testChunkedTransferEncoding() throws IOException {
        // 设置响应为 chunked
        // response.setHeader("Transfer-Encoding", "chunked");
        response.setAllowChunking(true);
        // 模拟 ServletOutputStream
        ServletOutputStream outputStream = response.getOutputStream();


        // 写入三块数据
        outputStream.write("Chunk 1".getBytes());
        response.flushBuffer(); // 发送第一块
        outputStream.write("Chunk 2".getBytes());
        response.flushBuffer(); // 发送第二块
        outputStream.write("Chunk 3".getBytes());
        response.flushBuffer(); // 发送第三块

        // 结束响应
        response.finishResponse(); // 通知结束

        // 检查输出内容
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证相应内容包含分块编码结构
        // 验证块大小和内容
        assertTrue(result.contains("15\r\nChunk 1Chunk 2Chunk 3\r\n"));
        // 验证分块结束符
        assertTrue(result.contains("0\r\n\r\n")); // 数据传输结束块
    }

    /**
     * 工具方法：计算字符串中某子串的出现次数
     *
     * @param string 原始字符串
     * @return 出现次数
     */
    private int getOccurrence(String string) {
        int count = 0;
        int index = string.indexOf("HTTP/1.1 200 OK");
        while (index != -1) {
            count++;
            string = string.substring(index + "HTTP/1.1 200 OK".length());
            index = string.indexOf("HTTP/1.1 200 OK");
        }
        return count;
    }

    @Test
    void testChunkedTransferEncodingFillBufferOnce() throws IOException {
        response.setAllowChunking(true); // 启用 Chunked 传输
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备刚好填满 8192 字节缓冲区的数据（A 字节填充）
        byte[] buffer = new byte[8192];
        Arrays.fill(buffer, (byte) 'A');

        // 写入数据并刷新
        outputStream.write(buffer);
        response.flushBuffer(); // 主动触发缓冲区刷出

        // 完成响应
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证块大小（8192 十六进制为 2000）
        assertTrue(result.contains("2000\r\n")); // 块大小标识
        // 验证块数据内容
        assertTrue(result.contains(new String(buffer)));
        // 验证结束符 0（表明传输结束）
        assertTrue(result.contains("0\r\n\r\n"));
    }

    @Test
    void testChunkedTransferEncodingExceedBuffer() throws IOException {
        response.setAllowChunking(true); // 启用 Chunked 传输
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备 12288 字节（> 8192 字节）的数据，分两块输出
        byte[] buffer = new byte[12288]; // 1.5 倍缓冲区大小
        Arrays.fill(buffer, (byte) 'B');

        // 写入数据并刷新
        outputStream.write(buffer);
        response.flushBuffer();

        // 完成响应
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证分两个块输出：8192 (2000) 和 4096 (1000)
        assertTrue(result.contains("2000\r\n")); // 第一个块大小标识
        assertTrue(result.contains("1000\r\n")); // 第二个块大小标识
        // 验证结束符
        assertTrue(result.contains("0\r\n\r\n"));
    }
}