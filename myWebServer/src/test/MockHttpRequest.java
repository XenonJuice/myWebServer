import javax.servlet.ServletInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MockHttpRequest {

    private ServletInputStream stream; // 输入流
    private int contentLength;
    private String characterEncoding;
    private byte[] body;
    private final Map<String, String> headers = new HashMap<>(); // 模拟的请求头集合

    public void setStream(ServletInputStream stream) {
        this.stream = stream;
    }

    public ServletInputStream getStream() {
        return stream;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public String getCharacterEncoding() {
        return characterEncoding != null ? characterEncoding : "UTF-8"; // 默认返回 UTF-8
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        return headers.getOrDefault(name, null);
    }

    public String getParameter(String key1) {
        return headers.getOrDefault(key1, null);
    }
}