import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpRequestStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRequestTest {
    private HttpRequest httpRequest;

    @BeforeEach
    public void setUp() {
        // 初始化 HttpRequest
        httpRequest = new HttpRequest();

        // 初始化 headers 和其他可能为 null 的集合变量
        httpRequest.setHeaders(new HashMap<>()); // 确保 headers 不为 null
        httpRequest.setParameters(new HashMap<>()); // 确保 parameters 不为 null
    }

    /**
     * 测试正常流读取与解析
     */
    @Test
    public void testNormalStreamRead() throws IOException {
        String mockBody = "field1=value1&field2=value2";

        // 初始化 Mock 请求
        ByteArrayInputStream inputStream = new ByteArrayInputStream(mockBody.getBytes("UTF-8"));
        MockHttpRequest mockRequest = new MockHttpRequest();
        mockRequest.setStream(new HttpRequestStream(inputStream));
        mockRequest.setContentLength(mockBody.length());

        // 使用通用方法解析请求体
        parseRequestBody(mockRequest);

        // 验证解析内容
        String parsedBody = new String(mockRequest.getBody(), StandardCharsets.UTF_8);
        assertEquals(mockBody, parsedBody, "解析的请求体不匹配");
    }

    /**
     * 测试请求头解析
     */
    @Test
    public void testSetAndGetHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("Authorization", Collections.singletonList("Bearer token123"));

        httpRequest.setHeaders(headers);
        assertEquals("application/json", httpRequest.getHeader("Content-Type"), "请求头 Content-Type 解析错误");
        assertEquals("Bearer token123", httpRequest.getHeader("Authorization"), "请求头 Authorization 解析错误");

        // 验证 getHeaders 方法
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        List<String> headerNameList = Collections.list(headerNames);
        assertTrue(headerNameList.contains("Content-Type"), "默认 headers 未包含 Content-Type");
        assertTrue(headerNameList.contains("Authorization"), "默认 headers 未包含 Authorization");
    }

    /**
     * 测试设置和获取请求参数
     */
    @Test
    public void testSetAndGetParameters() {
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("param1", Collections.singletonList("value1"));
        parameters.put("param2", Arrays.asList("value2", "value3"));

        httpRequest.setParameters(parameters);

        assertEquals("value1", httpRequest.getParameter("param1"), "请求参数 param1 错误");
        assertArrayEquals(new String[]{"value2", "value3"}, httpRequest.getParameterValues("param2"), "请求参数 param2 错误");
    }

    @Test
    public void testEmptyStreamHandling() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        MockHttpRequest mockRequest = new MockHttpRequest();
        mockRequest.setStream(new HttpRequestStream(inputStream));
        mockRequest.setContentLength(0);

        parseRequestBody(mockRequest);

        assertEquals(0, mockRequest.getBody().length, "空流的请求体长度应为 0");
    }

    @Test
    public void testIncompleteContentLength() throws IOException {
        String mockBody = "short";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(mockBody.getBytes("UTF-8"));
        MockHttpRequest mockRequest = new MockHttpRequest();
        mockRequest.setStream(new HttpRequestStream(inputStream));
        mockRequest.setContentLength(20); // 设置的 Content-Length 大于可用数据长度

        IOException exception = assertThrows(IOException.class, () -> parseRequestBody(mockRequest));
        assertEquals("请求体数据不完整，读取的字节数与 Content-Length 不匹配", exception.getMessage(), "异常信息不匹配");
    }

    /**
     * 测试含字符编码的解析行为（非默认 UTF-8）
     */
    @Test
    public void testParseBodyWithSpecificCharacterEncoding() throws IOException {
        // 测试请求体内容
        String body = "key=value中文";
        String charset = "GBK";

        // 模拟输入流及头部信息
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body.getBytes(charset));
        MockHttpRequest mockRequest = new MockHttpRequest(); // 用模拟的 HttpRequest
        mockRequest.setCharacterEncoding(charset); // 设置字符编码
        mockRequest.setContentLength(body.getBytes(charset).length); // 模拟 Content-Length
        mockRequest.setStream(new HttpRequestStream(inputStream)); // 设置流

        // 调用独立解析方法
        parseRequestBody(mockRequest);

        // 验证解析结果
        String parsedBody = new String(mockRequest.getBody(), Charset.forName(charset));
        assertEquals(body, parsedBody, "请求体解析错误，解析出的内容与原始请求体不匹配");
    }

    /**
     * 解析请求体（从 HttpProcessor 提取的逻辑）
     *
     * @param request 模拟的 HttpRequest 对象，包含输入流和头部信息
     * @throws IOException 如果流读取发生错误
     */
    private void parseRequestBody(MockHttpRequest request) throws IOException {
        int contentLength = request.getContentLength();
        String characterEncoding = request.getCharacterEncoding();

        // 检查 Content-Length
        if (contentLength <= 0) {
            request.setBody(new byte[0]);
            return;
        }

        // 从输入流读取请求体
        ServletInputStream inputStream = request.getStream();
        byte[] body = new byte[contentLength];
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int readCount = inputStream.read(body, bytesRead, contentLength - bytesRead);
            if (readCount == -1) {
                break; // 读取结束
            }
            bytesRead += readCount;
        }

        // 如果读取到的字节数与 Content-Length 不匹配，抛出异常
        if (bytesRead != contentLength) {
            throw new IOException("请求体数据不完整，读取的字节数与 Content-Length 不匹配");
        }

        // 设置到请求对象中
        request.setBody(body);
    }

    /**
     * 测试请求头为空的行为
     */
    @Test
    public void testEmptyHeaders() {
        Map<String, List<String>> emptyHeaders = new HashMap<>();
        httpRequest.setHeaders(emptyHeaders);

        assertNull(httpRequest.getHeader("Content-Type"), "当请求头为空时，getHeader 应返回 null");
    }

}