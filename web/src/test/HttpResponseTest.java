import erangel.base.Const;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

public class HttpResponseTest {

    private MockClientOutputStream clientStream;
    private HttpResponse httpResponse;

    @BeforeEach
    public void setUp() {
        // 初始化模拟输出流
        clientStream = new MockClientOutputStream();

        // 初始化 HttpResponse 并设置流
        httpResponse = new HttpResponse();
        httpResponse.setStream(clientStream);

        // 初始化 HttpRequest 并配置必要信息
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setProtocol("HTTP/1.1");
        httpRequest.setUri("/test/resource");
        httpRequest.setMethod("GET");
        httpRequest.setHeaders(createMockHeaders());
        httpRequest.setResponse(httpResponse);
        httpResponse.setRequest(httpRequest);
    }

    @Test
    public void testNormalResponseFlow() throws IOException {
        // 设置响应内容
        writeToResponse();

        // 验证输出内容和流关闭
        verifyResponse("HTTP/1.1 200 OK\r\n" +
                "Date: Fri, 17 Jan 2025 07:06:03 GMT\r\n" +
                "Server: CustomJavaServer\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: 11\r\n" +
                "\r\n" + // 响应头和正文的分隔符
                "Hello World");
    }

    @Test
    public void testErrorResponse() throws IOException {
        // 模拟 404 错误并完成响应
        httpResponse.sendError(SC_NOT_FOUND);
        httpResponse.finishResponse();

        // 验证输出是否为预期的错误页面
        String errorPage = "<html><head><title>Error</title></head><body>"
                + "<h1>HTTP Error " + SC_NOT_FOUND + " - " + "Not Found" + "</h1>"
                + "</body></html>";
        String expectedOutput = "HTTP/1.1 404 Not Found\r\n" +
                "Date: Fri, 17 Jan 2025 07:06:03 GMT\r\n" +
                "Server: CustomJavaServer\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: 94\r\n" +

                "\r\n" + errorPage;
        verifyResponse(expectedOutput);
    }

    @Test
    public void testWriterAndOutputStreamConflict() {
        // 获取 Writer，不应抛出异常
        assertDoesNotThrow(() -> httpResponse.getWriter());

        // 再尝试获取 OutputStream，应该抛出 IllegalStateException
        Exception exception = assertThrows(IllegalStateException.class, () -> httpResponse.getOutputStream());
        assertEquals("getWriter() has already been called on this response.", exception.getMessage());
    }

    @Test
    public void testBufferFlush() throws IOException {
        // 写入部分数据
        httpResponse.getWriter().write("Partial Data");

        // 调用 flushBuffer 方法，将缓冲区内容写入流
        httpResponse.flushBuffer();
        httpResponse.finishResponse();
        StreamAssertUtil.assertOutputData(clientStream, "HTTP/1.1 200 OK\r\n" +
                "Date: Fri, 17 Jan 2025 07:06:03 GMT\r\n" +
                "Server: CustomJavaServer\r\n" + "Connection: close\r\n"+Const.PunctuationMarks.CRLF + "Partial Data");

        // 完成响应并验证流关闭
        httpResponse.finishResponse();
        StreamAssertUtil.assertStreamClosed(clientStream);
    }

    /**
     * 辅助方法：向 HttpResponse 写入内容
     *
     * @throws IOException 如果写入失败
     */
    private void writeToResponse() throws IOException {
        httpResponse.setContentType("text/plain; charset=UTF-8");
        httpResponse.setContentLength(11);
        httpResponse.getWriter().write("Hello World");
        httpResponse.finishResponse();
    }

    /**
     * 辅助方法：验证响应输出与流状态
     *
     * @param expectedOutput 期待的输出内容
     */
    private void verifyResponse(String expectedOutput) {
        // 验证内容
        StreamAssertUtil.assertOutputData(clientStream, expectedOutput);

        // 验证流关闭
        StreamAssertUtil.assertStreamClosed(clientStream);
    }

    /**
     * 创建一个模拟的请求头
     *
     * @return header map
     */
    private Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/plain"));
        headers.put("User-Agent", Collections.singletonList("MockTestClient/1.0"));
        return headers;
    }
}