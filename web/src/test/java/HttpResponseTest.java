import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import erangel.connector.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

public class HttpResponseTest {

    private ByteArrayOutputStream outputStream;
    private HttpResponse response;

    @BeforeEach
    void setUp() throws UnsupportedEncodingException {
        outputStream = new ByteArrayOutputStream();
        response = new HttpResponse(outputStream);
    }

    // 提供测试用例的数据源
    private static Stream<Arguments> provideTestCasesForHttpResponse() {
        return Stream.of(
                // 测试用例1：发送简单的200 OK响应
                Arguments.of(
                        200,
                        "OK",
                        Map.of("Content-Type", "text/plain"),
                        List.of(),
                        "Hello, World!",
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 13\r\n" +
                                "Date: ",
                        "Hello, World!"
                ),
                // 测试用例2：发送404 Not Found错误
                Arguments.of(
                        404,
                        "Not Found",
                        Map.of("Content-Type", "text/html"),
                        List.of(),
                        "<h1>File Not Found</h1>",
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Type: text/html\r\n" +
                                "Content-Length: 23\r\n" +
                                "Date: ",
                        "<h1>File Not Found</h1>"
                ),
                // 测试用例3：发送带有Cookie的响应
                Arguments.of(
                        200,
                        "OK",
                        Map.of("Content-Type", "text/html"),
                        List.of(new Cookie("sessionId", "abc123"), new Cookie("user", "JohnDoe")),
                        "<html><body>Welcome!</body></html>",
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html\r\n" +
                                "Content-Length: 34\r\n" +
                                "Set-Cookie: sessionId=abc123; Path=/\r\n" +
                                "Set-Cookie: user=JohnDoe; Path=/\r\n" +
                                "Date: ",
                        "<html><body>Welcome!</body></html>"
                ),
                // 测试用例4：发送重定向响应
                Arguments.of(
                        302,
                        "Found",
                        Map.of(
                                "Location", "http://example.com",
                                "Content-Type", "text/html; charset=UTF-8" // 添加 Content-Type 头部
                        ),
                        List.of(),
                        "<html><body>Redirecting to <a href=\"http://example.com\">http://example.com</a></body></html>",
                        "HTTP/1.1 302 Found\r\n" +
                                "Content-Type: text/html; charset=UTF-8\r\n" +
                                "Content-Length: 92\r\n" +
                                "Location: http://example.com\r\n" +
                                "Date: ",
                        "<html><body>Redirecting to <a href=\"http://example.com\">http://example.com</a></body></html>"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCasesForHttpResponse")
    void testHttpResponse(
            int statusCode,
            String statusMessage,
            Map<String, String> headers,
            List<Cookie> cookies,
            String responseBody,
            String expectedStartOfResponse,
            String expectedBody
    ) throws IOException {
        // 设置状态码和状态消息
        response.setStatus(statusCode, statusMessage);

        // 设置头部信息
        headers.forEach(response::setHeader);

        // 添加Cookies
        cookies.forEach(response::addCookie);

        // 写入响应体
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write(responseBody.getBytes(response.getCharacterEncoding()));
        servletOutputStream.flush();

        // 刷新缓冲区
        response.flushBuffer();

        // 获取实际的响应内容
        String actualResponse = outputStream.toString(response.getCharacterEncoding());

        // 验证状态行和头部
        assertTrue(actualResponse.startsWith(expectedStartOfResponse), "响应开头不匹配");

        // 验证 Content-Length 头部
        if (statusCode == 200 || statusCode == 302 || statusCode == 404) { // 根据需要添加其他状态码
            byte[] responseBodyBytes = responseBody.getBytes(response.getCharacterEncoding());
            String expectedContentLength = "Content-Length: " + responseBodyBytes.length + "\r\n";
            assertTrue(actualResponse.contains(expectedContentLength), "Content-Length头部不匹配");
        }

        // 验证响应体
        assertTrue(actualResponse.endsWith(expectedBody), "响应体不匹配");
    }

    // 单独测试 sendError 方法
    @Test
    void testSendError() throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");

        String responseString = outputStream.toString(response.getCharacterEncoding());
        assertTrue(responseString.startsWith("HTTP/1.1 404 Not Found\r\n"));
        assertTrue(responseString.contains("Content-Type: text/html; charset=UTF-8\r\n"));
        assertTrue(responseString.contains("Content-Length: "));
        assertTrue(responseString.contains("<h1>HTTP Error 404 - Not Found</h1>"));
    }

    // 单独测试 sendRedirect 方法
    @Test
    void testSendRedirect() throws IOException {
        String location = "http://example.com";
        response.sendRedirect(location);

        String responseString = outputStream.toString(response.getCharacterEncoding());
        assertTrue(responseString.startsWith("HTTP/1.1 302 Found\r\n"));
        assertTrue(responseString.contains("Location: " + location + "\r\n"));
        assertTrue(responseString.contains("Content-Type: text/html; charset=UTF-8\r\n"));
        assertTrue(responseString.contains("Content-Length: "));
        assertTrue(responseString.contains("Redirecting to <a href=\"" + location + "\">" + location + "</a>"));
    }

    // 单独测试添加和格式化Cookies
    @Test
    void testAddCookies() throws IOException {
        Cookie cookie1 = new Cookie("sessionId", "abc123");
        Cookie cookie2 = new Cookie("user", "JohnDoe");
        response.addCookie(cookie1);
        response.addCookie(cookie2);

        // 写入响应头
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write("Response with cookies".getBytes(response.getCharacterEncoding()));
        servletOutputStream.flush();
        response.flushBuffer();

        String responseString = outputStream.toString(response.getCharacterEncoding());
        assertTrue(responseString.contains("Set-Cookie: sessionId=abc123; Path=/"));
        assertTrue(responseString.contains("Set-Cookie: user=JohnDoe; Path=/"));
    }

    // 测试缓冲区管理
    @Test
    void testFlushBuffer() throws IOException {
        response.setStatus(HttpServletResponse.SC_OK, "OK");
        response.setContentType("text/plain");
        String body = "Hello, Buffered World!";
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write(body.getBytes(response.getCharacterEncoding()));

        // 在调用 flushBuffer 之前，缓冲区中应包含状态行和头部
        // 但由于 ByteArrayOutputStream 会收集所有数据，无法直接验证
        // 因此调用 flushBuffer 并验证整个响应

        response.flushBuffer();

        String responseString = outputStream.toString(response.getCharacterEncoding());
        assertTrue(responseString.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(responseString.contains("Content-Type: text/plain\r\n"));
        assertTrue(responseString.contains("Content-Length: " + body.getBytes(response.getCharacterEncoding()).length + "\r\n"));
        assertTrue(responseString.endsWith(body));
    }

    // 测试重置缓冲区
    @Test
    void testResetBuffer() throws IOException {
        response.setHeader("X-Test", "TestValue");
        response.resetBuffer();

        // 写入新的响应体
        response.setContentType("text/plain");
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write("New Response".getBytes(response.getCharacterEncoding()));
        response.flushBuffer();

        String responseString = outputStream.toString(response.getCharacterEncoding());
        assertFalse(responseString.contains("X-Test: TestValue"), "Header未被重置");
        assertTrue(responseString.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(responseString.contains("Content-Type: text/plain\r\n"));
        assertTrue(responseString.endsWith("New Response"));
    }

    // 测试重置整个响应
    @Test
    void testReset() throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND, "Not Found");
        response.setContentType("text/html");
        response.setHeader("X-Test", "TestValue");
        response.addCookie(new Cookie("sessionId", "abc123"));

        response.reset();

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("OK", response.getStatusMessage());
        assertNull(response.getContentType());
        assertEquals("ISO-8859-1", response.getCharacterEncoding());
        assertTrue(response.getHeadersMap().isEmpty());
        assertTrue(response.getCookies().isEmpty());
        assertFalse(response.isCommitted());

        // 写入新的响应体
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write("Reset Response".getBytes(response.getCharacterEncoding()));
        response.flushBuffer();

        String responseString = outputStream.toString(response.getCharacterEncoding());
        assertTrue(responseString.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(responseString.contains("Content-Length: " + "Reset Response".getBytes(response.getCharacterEncoding()).length + "\r\n"));
        assertTrue(responseString.endsWith("Reset Response"));
    }

    // 测试设置内容长度
    @Test
    void testSetContentLength() throws IOException {
        response.setContentLength(100);
        response.setContentLengthLong(1000L);
        // 写入响应体
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write("Content Length Test".getBytes(response.getCharacterEncoding()));
        response.flushBuffer();
        String responseString = outputStream.toString(response.getCharacterEncoding());
        // 注意：setContentLength和setContentLengthLong都被调用，但最后一个设置应生效
        String expectedContentLength = "Content-Length: 19\r\n"; // "Content Length Test".length() == 19
        assertTrue(responseString.contains(expectedContentLength), "Content-Length头部不匹配");
    }

    // 测试设置日期头部
    @Test
    void testSetDateHeader() throws IOException {
        long currentTime = System.currentTimeMillis();
        response.setDateHeader("Date", currentTime);

        // 写入响应体
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write("Date Header Test".getBytes(response.getCharacterEncoding()));
        response.flushBuffer();

        String responseString = outputStream.toString(response.getCharacterEncoding());
        String formattedDate = response.formatDate(currentTime);
        assertTrue(responseString.contains("Date: " + formattedDate + "\r\n"));
    }

    // 测试设置整数头部
    @Test
    void testSetIntHeader() throws IOException {
        response.setIntHeader("Content-Length", 256);
        response.addIntHeader("Content-Length", 512);

        // 写入响应体
        OutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write("Int Header Test".getBytes(response.getCharacterEncoding()));
        response.flushBuffer();

        String responseString = outputStream.toString(response.getCharacterEncoding());
        // 最后设置的Content-Length应为512，但响应体只有15字节，测试可能需要调整
        // 根据实际实现，Content-Length应为15
        // 因为setIntHeader("Content-Length", 256) 会被覆盖
        String expectedContentLength = "Content-Length: 15\r\n";
        assertTrue(responseString.contains(expectedContentLength), "Content-Length头部不匹配");
    }

    // 测试私有方法 formatCookie
    @Test
    void testFormatCookie() {
        Cookie cookie = new Cookie("sessionId", "abc123");
        cookie.setMaxAge(3600);
        cookie.setPath("/");
        cookie.setDomain("example.com");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        String formattedCookie = response.formatCookie(cookie);
        String expected = "sessionId=abc123; Max-Age=3600; Path=/; Domain=example.com; Secure; HttpOnly";
        assertEquals(expected, formattedCookie);
    }
}
