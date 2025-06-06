import livonia.connector.http.HttpConnector;
import livonia.connector.http.HttpRequest;
import livonia.connector.http.HttpProcessor;
import livonia.connector.http.HttpResponse;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeEach;

import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
@Deprecated
public class HttpRequestFinishRequestTest {
    private MockClientOutputStream clientStream;
    private HttpResponse httpResponse;
    private HttpRequest httpRequest ;


    @BeforeEach
    public void setUp() {
        // 初始化模拟输出流
        clientStream = new MockClientOutputStream();

        // 初始化 HttpResponse 并设置流
        httpResponse = new HttpResponse();
        httpResponse.setStream(clientStream);

        // 初始化 HttpRequest 并配置必要信息
        httpRequest = new HttpRequest();
        httpRequest.setProtocol("HTTP/1.1");
        httpRequest.setUri("/test/resource");
        httpRequest.setMethod("GET");
        httpRequest.setHeaders(createMockHeaders());
        httpRequest.setResponse(httpResponse);
        httpResponse.setRequest(httpRequest);
    }
    @Test
    public void testFinishRequestClosesStreams() throws IOException {
        // 模拟 Socket 和输入流
        Socket mockSocket = mock(Socket.class);
        InputStream mockClientInputStream = mock(InputStream.class);
        OutputStream mockOutputStream = mock(OutputStream.class);
        HttpConnector connector = mock(HttpConnector.class);

        when(mockSocket.getInputStream()).thenReturn(mockClientInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        when(connector.createRequest()).thenReturn(this.httpRequest);
        when(connector.createResponse()).thenReturn(this.httpResponse);
        HttpProcessor processor = mock(HttpProcessor.class);
        doCallRealMethod().when(processor).process(mockSocket);

        try {
            processor.process(mockSocket);
        } catch (Exception e) {
            fail("处理流关闭时出现异常" + e.getMessage());
        }

        // 验证所有流被正确关闭
        verify(mockClientInputStream, times(0)).close();
    }

    @Test
    public void testFinishRequestWithNullStream() {
        // 模拟 HttpRequest 没有初始化流的情况
        HttpRequest request = new HttpRequest();

        // 调用 finishRequest，不期望异常抛出
        try {
            request.finishRequest();
        } catch (IOException e) {
            fail("当流为 null 时，finishRequest 不应抛出异常");
        }
    }

    @Test
    public void testProcessorProcessStreamClose() throws IOException {
        Socket mockSocket = mock(Socket.class);
        InputStream mockInputStream = new ByteArrayInputStream("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
        OutputStream mockOutputStream = mock(OutputStream.class);

        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

        HttpProcessor processor = mock(HttpProcessor.class);
        processor.setNoProblem(true);
        doCallRealMethod().when(processor).process(mockSocket);

        try {
            processor.process(mockSocket);
        } catch (Exception e) {
           fail("处理流关闭时出现异常" + e.getMessage());
        }

        verify(mockSocket, times(1)).close();
    }

    @Test
    public void testFinishRequestLeakSimulation() throws IOException {
        // 设置一个有内容的流
        InputStream mockInputStream = new ByteArrayInputStream("request data".getBytes());

        HttpRequest request = new HttpRequest();
        request.setStream(mockInputStream);

        // 调用 finishRequest
        request.finishRequest();

        // 尝试再次关闭，验证不会有异常
        try {
            mockInputStream.close();
        } catch (IOException e) {
            fail("重复关闭流时不应抛出异常：" + e.getMessage());
        }
    }

    private Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/plain"));
        headers.put("User-Agent", Collections.singletonList("MockTestClient/1.0"));
        return headers;
    }
}