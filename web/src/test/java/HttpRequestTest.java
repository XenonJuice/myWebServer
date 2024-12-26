
import erangel.connector.http.HttpRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
@Deprecated
public class HttpRequestTest {

    private static Stream<Arguments> provideTestCasesForHttpRequest() {
        return Stream.of(
                // 测试用例1：正常的HTTP请求
                Arguments.of(
                        "GET /index.html HTTP/1.1\nHost: localhost\n\n",
                        "GET",
                        "/index.html",
                        "HTTP/1.1",
                        "localhost",
                        null // 没有Cookie
                ),
                // 测试用例2：没有请求头的HTTP请求
                Arguments.of(
                        "POST /login.html?name=LLJ HTTP/1.1\n\n",
                        "POST",
                        "/login.html",
                        "HTTP/1.1",
                        null,
                        null // 没有Cookie
                ),
                // 测试用例3：没有请求方法的HTTP请求
//                Arguments.of(
//                        "/index.html HTTP/1.1\nHost: localhost\n\n",
//                        null,
//                        "/index.html",
//                        "HTTP/1.1",
//                        "localhost",
//                        null // 没有Cookie
//                ),
                // 测试用例4：带有Cookie的HTTP请求
                Arguments.of(
                        "GET /dashboard HTTP/1.1\nHost: localhost\nCookie: sessionId=abc123; user=JohnDoe\n\n",
                        "GET",
                        "/dashboard",
                        "HTTP/1.1",
                        "localhost",
                        Map.of(
                                "sessionId", "abc123",
                                "user", "JohnDoe"
                        )
                ),
                // 测试用例5：带有多个Cookie的HTTP请求
                Arguments.of(
                        "POST /submit HTTP/1.1\nHost: example.com\nCookie: theme=light; lang=en-US; token=xyz789\n\n",
                        "POST",
                        "/submit",
                        "HTTP/1.1",
                        "example.com",
                        Map.of(
                                "theme", "light",
                                "lang", "en-US",
                                "token", "xyz789"
                        )
                ),
                // 测试用例6：只有一个Cookie的HTTP请求
                Arguments.of(
                        "GET /profile HTTP/1.1\nHost: mysite.com\nCookie: loggedIn=true\n\n",
                        "GET",
                        "/profile",
                        "HTTP/1.1",
                        "mysite.com",
                        Map.of(
                                "loggedIn", "true"
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCasesForHttpRequest")
    public void testHttpRequest(
            String request,
            String expectedMethod,
            String expectedUri,
            String expectedProtocol,
            String expectedHost,
            Map<String, String> expectedCookies
    ) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(request.getBytes());
        HttpRequest httpRequest = new HttpRequest();
        assertEquals(expectedMethod, httpRequest.getMethod());
        assertEquals(expectedUri, httpRequest.getRequestURI());
        assertEquals(expectedProtocol, httpRequest.getProtocol());
        assertEquals(expectedHost, httpRequest.getHeader("Host"));

        if (expectedCookies != null) {
            Cookie[] cookies = httpRequest.getCookies();
            assertEquals(expectedCookies.size(), cookies.length, "Cookie数量不匹配");

            for (Cookie cookie : cookies) {
                String expectedValue = expectedCookies.get(cookie.getName());
                assertEquals(expectedValue, cookie.getValue(), "Cookie值不匹配: " + cookie.getName());
            }
        } else {
            // 如果预期没有Cookie，确保getCookies()返回空数组或null
            Cookie[] cookies = httpRequest.getCookies();
            assertTrue(cookies == null || cookies.length == 0, "预期没有Cookie，但实际存在");
        }
    }}
