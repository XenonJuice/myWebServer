import erangel.connector.http.HttpProcessor;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@Deprecated
public class HttpProcessorTest {

    private HttpRequest mockRequest;
    private HttpResponse mockResponse;
    private ServletInputStream mockInputStream;

    @Before
    public void setUp() {
        mockRequest = Mockito.mock(HttpRequest.class);
        mockResponse = Mockito.mock(HttpResponse.class);
    }

    /**
     * 测试GET请求，无参数，无请求体
     * 请求行示例：
     * GET /index.html HTTP/1.1
     * Host: localhost
     */
    @Test
    public void testGetRequestWithoutParams() throws IOException {
        String rawRequest =
                """
                        GET /index.html HTTP/1.1\r
                        Host: localhost\r
                        \r
                        """;
        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn(null);

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        verify(mockRequest).setMethod("GET");
        verify(mockRequest).setUri("/index.html");
        verify(mockRequest).setProtocol("HTTP/1.1");

        Map<String, List<String>> headers = processor.headers;
        assertTrue(headers.containsKey("Host"));
        assertEquals(1, headers.get("Host").size());
        assertEquals("localhost", headers.get("Host").get(0));

        Map<String, List<String>> params = processor.parameters;
        assertTrue(params.isEmpty());
    }

    /**
     * 测试GET请求，有查询参数
     * 请求行示例：
     * GET /search?q=Java&lang=en HTTP/1.1
     * Host: localhost
     */
    @Test
    public void testGetRequestWithQueryParams() throws IOException {
        String rawRequest =
                """
                        GET /search?q=Java&lang=en HTTP/1.1\r
                        Host: localhost\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn(null);

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        verify(mockRequest).setMethod("GET");
        verify(mockRequest).setUri("/search");
        verify(mockRequest).setProtocol("HTTP/1.1");

        Map<String, List<String>> params = processor.parameters;
        assertEquals(2, params.size());
        assertTrue(params.containsKey("q"));
        assertEquals("Java", params.get("q").get(0));
        assertTrue(params.containsKey("lang"));
        assertEquals("en", params.get("lang").get(0));
    }

    /**
     * 测试POST请求，有请求体参数 (application/x-www-form-urlencoded)
     * 请求行示例：
     * POST /submit HTTP/1.1
     * Host: localhost
     * Content-Type: application/x-www-form-urlencoded
     * Content-Length: 27
     * name=John+Doe&age=30&city=NY
     */
    @Test
    public void testPostRequestWithBodyParams() throws IOException {
        String rawRequest =
                """
                        POST /submit HTTP/1.1\r
                        Host: localhost\r
                        Content-Type: application/x-www-form-urlencoded\r
                        Content-Length: 28\r
                        \r
                        name=John+Doe&age=30&city=NY""";

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        verify(mockRequest).setMethod("POST");
        verify(mockRequest).setUri("/submit");
        verify(mockRequest).setProtocol("HTTP/1.1");

        Map<String, List<String>> params = processor.parameters;
        assertEquals(3, params.size());
        assertEquals("John Doe", params.get("name").get(0));
        assertEquals("30", params.get("age").get(0));
        assertEquals("NY", params.get("city").get(0));
    }

    /**
     * 测试无效请求行
     */
    @Test(expected = IOException.class)
    public void testInvalidRequestLine() throws IOException {
        String rawRequest =
                """
                        INVALID_REQUEST_LINE\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        new HttpProcessor(mockRequest, mockResponse);
    }

    /**
     * 测试空请求
     */
    @Test(expected = IOException.class)
    public void testEmptyRequest() throws IOException {
        String rawRequest = "";

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        new HttpProcessor(mockRequest, mockResponse);
    }


    /**
     * 测试URI中多个同名参数
     * GET /search?name=John&name=Jane&age=30 HTTP/1.1
     */
    @Test
    public void testGetRequestWithMultipleValuesForParameter() throws IOException {
        String rawRequest =
                """
                        GET /search?name=John&name=Jane&age=30 HTTP/1.1\r
                        Host: localhost\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        Map<String, List<String>> params = processor.parameters;
        assertEquals(2, params.get("name").size());
        assertEquals("John", params.get("name").get(0));
        assertEquals("Jane", params.get("name").get(1));
        assertEquals("30", params.get("age").get(0));
    }

    /**
     * 测试POST请求无Content-Length但有请求体的数据（实际中是不规范的）
     * 预期：应当抛出异常或无法正确解析请求体
     */
    @Test(expected = IOException.class)
    public void testPostRequestWithoutContentLength() throws IOException {
        String rawRequest =
                "POST /submit HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Type: application/x-www-form-urlencoded\r\n" +
                        "\r\n" + // 空行后本应开始请求体，但没有Content-Length
                        "name=John";

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        // 没有 content-length 将导致在读取请求体时遇到问题
        // excepting an IO exception
        new HttpProcessor(mockRequest, mockResponse);
    }

    /**
     * 测试POST请求有请求体但无Content-Type头部
     * 应当能读取请求体，但不做url解析参数操作
     */
    @Test
    public void testPostRequestNoContentType() throws IOException {
        String body = "raw data123";
        String rawRequest =
                "POST /upload HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "\r\n" +
                        body;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn(null);

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 没有application/x-www-form-urlencoded，不应解析参数
        assertTrue(processor.parameters.isEmpty());
    }

    /**
     * 测试POST请求为JSON类型的请求体，不解析为参数
     * 同时检查头部是否解析正确
     */
    @Test
    public void testPostRequestJsonBody() throws IOException {
        String jsonBody = "{\"name\":\"John\", \"age\":30}";
        String rawRequest =
                "POST /submit HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + jsonBody.getBytes().length + "\r\n" +
                        "\r\n" +
                        jsonBody;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn("application/json");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证请求方法、URI、协议是否被正确设置
        verify(mockRequest).setMethod("POST");
        verify(mockRequest).setUri("/submit");
        verify(mockRequest).setProtocol("HTTP/1.1");

        // 验证参数为空，因为JSON不解析为表单参数
        assertTrue(processor.parameters.isEmpty());

        // 检查头部解析结果
        Map<String, List<String>> headers = processor.headers;
        assertTrue(headers.containsKey("Host"));
        assertEquals("localhost", headers.get("Host").get(0));

        assertTrue(headers.containsKey("Content-Type"));
        assertEquals("application/json", headers.get("Content-Type").get(0));

        assertTrue(headers.containsKey("Content-Length"));
        assertEquals(String.valueOf(jsonBody.getBytes().length), headers.get("Content-Length").get(0));

        // 同时也可以verify对mockRequest.setHeaders(...)的调用
        // 由于headers是Map<String, List<String>>，对其verify时要构造匹配器或对其进行ArgumentCaptor
        verify(mockRequest).setHeaders(argThat(h ->
                h.get("Host").contains("localhost") &&
                        h.get("Content-Type").contains("application/json") &&
                        h.get("Content-Length").contains(String.valueOf(jsonBody.getBytes().length))
        ));
    }


    /**
     * 测试请求体为二进制数据（application/octet-stream）
     * 不解析参数，但应正确读取字节数
     */
    @Test
    public void testPostBinaryData() throws IOException {
        byte[] binaryData = {0x01, 0x02, 0x03, 0x04};
        String rawRequest =
                """
                        POST /data HTTP/1.1\r
                        Host: localhost\r
                        Content-Type: application/octet-stream\r
                        Content-Length: 4\r
                        \r
                        """;

        // 构造完整数据：头部后面直接跟binaryData
        byte[] requestData = (rawRequest).getBytes(StandardCharsets.UTF_8);
        byte[] fullRequest = new byte[requestData.length + binaryData.length];
        System.arraycopy(requestData, 0, fullRequest, 0, requestData.length);
        System.arraycopy(binaryData, 0, fullRequest, requestData.length, binaryData.length);

        mockInputStream = createInputStreamFromByteArray(fullRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn("application/octet-stream");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 不会解析参数，但内部已正确读取4字节
        assertTrue(processor.parameters.isEmpty());
    }

    /**
     * 测试Content-Length过大
     * 比实际请求体多1字节
     */
    @Test(expected = IOException.class)
    public void testPostContentLengthMismatch() throws IOException {
        String body = "name=John";
        // 声称有一字节比实际更多
        int declaredLength = body.length() + 1;

        String rawRequest =
                "POST /submit HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Type: application/x-www-form-urlencoded\r\n" +
                        "Content-Length: " + declaredLength + "\r\n" +
                        "\r\n" +
                        body; // 实际比声明的少1字节

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");
        when(mockRequest.getContentType()).thenReturn("application/x-www-form-urlencoded");

        new HttpProcessor(mockRequest, mockResponse);
    }

    /**
     * 测试缺少空行分隔导致的解析错误
     * （即请求头结束后没有空行就直接开始请求体）
     * 此种情况下应当抛出异常，因为解析头部时还没有结束
     */
    @Test(expected = IOException.class)
    public void testMissingEmptyLineAfterHeaders() throws IOException {
        String rawRequest =
                "POST /submit HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: 4\r\n"
                        // 少了空行，这里直接写请求体
                        + "abcd";

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        new HttpProcessor(mockRequest, mockResponse);
    }

    /**
     * 测试包含绝大多数常见字段的请求头
     * 请求行示例：
     * GET /resource HTTP/1.1
     * Host: localhost
     * User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36
     * Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,;q=0.8
     * Accept-Encoding: gzip, deflate, br
     * Accept-Language: en-US,en;q=0.9
     * Cache-Control: no-cache
     * Connection: keep-alive
     * Authorization: Basic SOMEONE_BASE64
     */
    @Test
    public void testRequestWithMostCommonHeaders() throws IOException {
        String rawRequest =
                """
                        GET /resource HTTP/1.1\r
                        Host: localhost\r
                        User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36\r
                        Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8\r
                        Accept-Encoding: gzip, deflate, br\r
                        Accept-Language: en-US,en;q=0.9\r
                        Cache-Control: no-cache\r
                        Connection: keep-alive\r
                        Authorization: Basic SOMEONE_BASE64\r
                        Cookie: sessionId=abc123; theme=dark; userId=42\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证请求行解析正确
        verify(mockRequest).setMethod("GET");
        verify(mockRequest).setUri("/resource");
        verify(mockRequest).setProtocol("HTTP/1.1");

        // 验证请求头解析正确
        Map<String, List<String>> headers = processor.headers;
        assertEquals("localhost", headers.get("Host").get(0));
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                headers.get("User-Agent").get(0));
        assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                headers.get("Accept").get(0));
        assertEquals("gzip, deflate, br", headers.get("Accept-Encoding").get(0));
        assertEquals("en-US,en;q=0.9", headers.get("Accept-Language").get(0));
        assertEquals("no-cache", headers.get("Cache-Control").get(0));
        assertEquals("keep-alive", headers.get("Connection").get(0));
        assertEquals("Basic SOMEONE_BASE64", headers.get("Authorization").get(0));
        assertEquals("sessionId=abc123; theme=dark; userId=42", headers.get("Cookie").get(0));
    }

    /**
     * 测试Host字段解析：包含主机名和端口
     */
    @Test
    public void testHostWithPort() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: localhost:8080\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证请求头中的Host解析
        verify(mockRequest).setServerName("localhost");
        verify(mockRequest).setServerPort(8080);
    }

    /**
     * 测试Host字段解析：只包含主机名
     */
    @Test
    public void testHostWithoutPort() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: example.com\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证请求头中的Host解析
        verify(mockRequest).setServerName("example.com");
        verify(mockRequest).setServerPort(80); // 默认端口
    }

    /**
     * 测试Host字段解析：无效端口
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHostWithInvalidPort() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: localhost:invalidPort\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        // 解析过程中应该抛出IllegalArgumentException
        new HttpProcessor(mockRequest, mockResponse);
    }

    /**
     * 测试Host字段解析：缺少Host头部
     */
    @Test(expected = IOException.class)
    public void testMissingHostHeader() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        // 解析过程中应该抛出IOException，因为Host头部缺失
        new HttpProcessor(mockRequest, mockResponse);
    }

    /**
     * 单个 Cookie 的解析
     */
    @Test
    public void testSingleCookieParsing() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: localhost\r
                        Cookie: sessionId=abc123\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证 Cookie 解析结果
        Map<String, String> cookies = processor.cookies;
        assertEquals(1, cookies.size());
        assertEquals("abc123", cookies.get("sessionId"));
    }

    /**
     * 多个 Cookie 的解析
     */
    @Test
    public void testMultipleCookiesParsing() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: localhost\r
                        Cookie: sessionId=abc123; theme=dark; userId=42\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证 Cookie 解析结果
        Map<String, String> cookies = processor.cookies;
        assertEquals(3, cookies.size());
        assertEquals("abc123", cookies.get("sessionId"));
        assertEquals("dark", cookies.get("theme"));
        assertEquals("42", cookies.get("userId"));
    }

    /**
     * 无效cookie
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCookieParsing() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: localhost\r
                        Cookie: invalidCookie\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证 Cookie 解析结果
        Map<String, String> cookies = processor.cookies;
        // 无效的 Cookie 不应被解析
    }

    @Test
    public void testJSessionIdExtraction() throws IOException {
        String rawRequest =
                """
                        GET / HTTP/1.1\r
                        Host: localhost\r
                        Cookie: JSESSIONID=abc123; theme=dark\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证 JSESSIONID 是否正确提取
        verify(mockRequest).setRequestedSessionId("abc123");
        verify(mockRequest).setRequestedSessionIdFromCookie(true);
        verify(mockRequest).setRequestedSessionIdFromURL(false);
    }



    /**
     * 测试组装完整的请求头
     */
    @Test
    public void testHttpRequestAssembly() throws IOException {
        String rawRequest =
                """
                        GET /resource?q=Java HTTP/1.1\r
                        Host: localhost:8080\r
                        Cookie: sessionId=abc123; theme=dark; userId=42\r
                        Accept-Language: en-US,en;q=0.9\r
                        \r
                        """;

        mockInputStream = createInputStreamFromString(rawRequest);
        when(mockRequest.getInputStream()).thenReturn(mockInputStream);
        when(mockRequest.getCharacterEncoding()).thenReturn("UTF-8");

        HttpProcessor processor = new HttpProcessor(mockRequest, mockResponse);

        // 验证组装后的 HttpRequest 内容
        verify(mockRequest).setMethod("GET");
        verify(mockRequest).setUri("/resource");
        verify(mockRequest).setProtocol("HTTP/1.1");
        verify(mockRequest).setServerName("localhost");
        verify(mockRequest).setServerPort(8080);
        verify(mockRequest).setLocale(Locale.forLanguageTag("en-US"));
        verify(mockRequest).setParameters(argThat(params ->
                params.get("q").contains("Java")
        ));
        verify(mockRequest).setCookies(argThat(cookies ->
                cookies.size() == 3 &&
                        cookies.stream().anyMatch(c -> c.getName().equals("sessionId") && c.getValue().equals("abc123"))
        ));
    }


    private ServletInputStream createInputStreamFromString(String data) {
        return createInputStreamFromByteArray(data.getBytes());
    }

    private ServletInputStream createInputStreamFromByteArray(byte[] data) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(javax.servlet.ReadListener readListener) {
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return byteArrayInputStream.read(b, off, len);
            }
        };
    }

}
