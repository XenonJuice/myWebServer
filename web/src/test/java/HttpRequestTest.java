
import erangel.connector.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpRequestTest {

    private static Stream<Arguments> provideTestCasesForHttpRequest() {
        return Stream.of(
                // 测试用例1：正常的HTTP请求
                Arguments.of("GET /index.html HTTP/1.1\nHost: localhost\n\n", "GET", "/index.html", "HTTP/1.1", "localhost"),
                // 测试用例2：没有请求头的HTTP请求
                Arguments.of("POST /login.html HTTP/1.1\n\n", "POST", "/login.html", "HTTP/1.1", null)//，
                // 测试用例3：没有请求方法的HTTP请求
                //Arguments.of("/index.html HTTP/1.1\nHost: localhost\n\n", null, "/index.html", "HTTP/1.1", "localhost"),
                // 测试用例4：没有请求URI的HTTP请求
                //Arguments.of("GET HTTP/1.1\nHost: localhost\n\n", "GET", null, "HTTP/1.1", "localhost"),
                // 测试用例5：没有协议版本的HTTP请求
               // Arguments.of("GET /index.html\nHost: localhost\n\n", "GET", "/index.html", null, "localhost")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCasesForHttpRequest")
    public void testHttpRequest(String request, String expectedMethod, String expectedUri, String expectedProtocol, String expectedHost) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(request.getBytes());
        HttpRequest httpRequest = new HttpRequest(inputStream);
        assertEquals(expectedMethod, httpRequest.getMethod());
        assertEquals(expectedUri, httpRequest.getRequestURI());
        assertEquals(expectedProtocol, httpRequest.getProtocol());
        assertEquals(expectedHost, httpRequest.getHeader("Host"));
    }
}
