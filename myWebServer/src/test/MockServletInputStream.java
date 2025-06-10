import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MockServletInputStream extends ServletInputStream {

    private final InputStream sourceStream;

    public MockServletInputStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    @Override
    public int read() throws IOException {
        return sourceStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return sourceStream.read(b, off, len);
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }
}