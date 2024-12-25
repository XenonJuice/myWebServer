

import erangel.connector.http.HttpResponse;
import org.junit.jupiter.api.*;

import javax.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpResponseTest {

    private ByteArrayOutputStream clientOut;
    private HttpResponse response;

    @BeforeEach
    void setUp() throws UnsupportedEncodingException {
        // 每个测试前都重置一个新的 ByteArrayOutputStream 以及 HttpResponse
        clientOut = new ByteArrayOutputStream();
        response = new HttpResponse(clientOut);
    }

    @Test
    @DisplayName("初始状态：Status = 200, 未提交, 无头部")
    void testInitialState() throws IOException {
        assertEquals(200, response.getStatus());   // SC_OK
        assertFalse(response.isCommitted());
        assertTrue(response.getHeaderNames().isEmpty());
        assertNotNull(response.getWriter());       // 能获取 Writer
    }

    @Test
    @DisplayName("setStatus() 能正确设置状态码和原因短语")
    void testSetStatus() {
        response.setStatus(HttpResponse.SC_NOT_FOUND);
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getStatusMessage());

        response.setStatus(HttpResponse.SC_BAD_REQUEST, "Custom Bad Request");
        assertEquals(400, response.getStatus());
        assertEquals("Custom Bad Request", response.getStatusMessage());
    }

    @Test
    @DisplayName("setHeader() & addHeader()")
    void testHeaders() throws IOException {
        response.setHeader("X-Test", "Hello");
        assertEquals("Hello", response.getHeader("X-Test"));

        response.addHeader("X-Test", "World");
        // 再次读取 X-Test，应该有2个值
        List<String> headers = (List<String>) response.getHeaders("X-Test");
        assertEquals(2, headers.size());
        assertTrue(headers.contains("Hello"));
        assertTrue(headers.contains("World"));

        // flush 之后，检查输出内容里是否包含 X-Test
        response.flushBuffer();
        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        assertTrue(written.contains("X-Test: Hello"));
        assertTrue(written.contains("X-Test: World"));
    }

    @Test
    @DisplayName("setContentType() & setCharacterEncoding()")
    void testContentTypeAndCharset() throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        assertEquals("text/html; charset=UTF-8", response.getContentType());
        assertEquals("UTF-8", response.getCharacterEncoding());

        // 写点文本
        response.getWriter().write("你好");
        response.flushBuffer();

        String written = clientOut.toString(StandardCharsets.UTF_8);
        // 检查头部包含 "Content-Type: text/html; charset=UTF-8"
        assertTrue(written.contains("Content-Type: text/html; charset=UTF-8"));
        // 检查输出体
        assertTrue(written.endsWith("你好"));  // 说明用 UTF-8 编码成功
    }

    @Test
    @DisplayName("setContentLength, setIntHeader, setDateHeader")
    void testContentLengthAndDateHeader() throws IOException {
        response.setContentLength(123);
        assertEquals("123", response.getHeader("Content-Length"));

        response.setIntHeader("X-Number", 42);
        assertEquals("42", response.getHeader("X-Number"));

        // 测试 DateHeader (只验证字符串格式包含 GMT 即可)
        long now = System.currentTimeMillis();
        response.setDateHeader("X-Date", now);

        response.flushBuffer();
        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        assertTrue(written.contains("Content-Length: 123"));
        assertTrue(written.contains("X-Number: 42"));
        // 简单检查下 "X-Date: Wed, 25 Oct 2023 ..." 之类的格式
        assertTrue(written.contains("X-Date: "));
        assertTrue(written.contains("GMT"));
    }

    @Test
    @DisplayName("addCookie() 及输出 Set-Cookie")
    void testCookies() throws IOException {
        Cookie c1 = new Cookie("sessionId", "ABC123");
        c1.setPath("/test");
        c1.setMaxAge(3600);
        c1.setHttpOnly(true);

        response.addCookie(c1);
        response.flushBuffer();

        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        assertTrue(written.contains("Set-Cookie: sessionId=ABC123"));
        assertTrue(written.contains("Max-Age=3600"));
        assertTrue(written.contains("Path=/test"));
        assertTrue(written.contains("HttpOnly"));
    }

    @Test
    @DisplayName("sendError() 后生成错误页面，并自动提交")
    void testSendError() throws IOException {
        // sendError(404, "Not Found") 并输出
        response.sendError(404, "File Not Found");
        // 已提交
        assertTrue(response.isCommitted());
        // 检查输出
        String written = clientOut.toString(StandardCharsets.UTF_8);
        // 状态行
        assertTrue(written.contains("HTTP/1.1 404 File Not Found"));
        // 内容
        assertTrue(written.contains("<h1>HTTP Error 404 - File Not Found</h1>"));
    }

    @Test
    @DisplayName("sendRedirect() 测试")
    void testSendRedirect() throws IOException {
        response.sendRedirect("/newLocation");
        // 检查状态码
        assertEquals(302, response.getStatus());
        assertEquals("Found", response.getStatusMessage());
        // 已提交
        assertTrue(response.isCommitted());

        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        assertTrue(written.contains("HTTP/1.1 302 Found"));
        assertTrue(written.contains("Location: /newLocation"));
        assertTrue(written.contains("<h1>Redirecting to <a href=\"/newLocation\">"));
    }

    @Test
    @DisplayName("flushBuffer() 不重复提交，且 isCommitted=true 之后不可改头部")
    void testFlushBuffer() throws IOException {
        response.setHeader("X-FlushTest", "BeforeFlush");
        response.flushBuffer();

        // 已提交
        assertTrue(response.isCommitted());
        // 再次改头部应抛异常
        assertThrows(IllegalStateException.class, () -> response.setHeader("X-FlushTest", "AfterFlush"));

        // 看输出是否包含
        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        assertTrue(written.contains("X-FlushTest: BeforeFlush"));
    }

    @Test
    @DisplayName("resetBuffer() 能清空尚未提交的数据")
    void testResetBuffer() throws IOException {
        response.getWriter().write("Hello, World!");
        // 尚未 flush，所以 isCommitted = false
        assertFalse(response.isCommitted());

        // resetBuffer -> 清空
        response.resetBuffer();
        response.getWriter().write("New Data");

        // 现在 flush
        response.flushBuffer();
        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        // 不应包含 "Hello, World!"
        assertFalse(written.contains("Hello, World!"));
        assertTrue(written.contains("New Data"));
    }

    @Test
    @DisplayName("reset() 在未提交时清空状态码、头部、Cookies以及正文")
    void testReset() throws IOException {
        response.setStatus(HttpResponse.SC_BAD_REQUEST, "BeforeReset");
        response.setHeader("X-Test", "oldHeader");
        response.addCookie(new Cookie("TestCookie", "123"));
        response.getWriter().write("OldData");
        assertEquals(400, response.getStatus());

        response.reset();
        // 状态码应回到 200
        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("X-Test"));
        assertTrue(response.getCookies().isEmpty());

        response.getWriter().write("NewData");
        response.flushBuffer();

        String written = clientOut.toString(StandardCharsets.ISO_8859_1);
        // 不含旧的数据
        assertFalse(written.contains("OldData"));
        // 不含 "X-Test: oldHeader"
        assertFalse(written.contains("X-Test: oldHeader"));
        // 新的数据
        assertTrue(written.contains("NewData"));
    }
}
