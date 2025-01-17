import java.io.IOException;
import java.io.OutputStream;

public class MockClientOutputStream extends OutputStream {
    private final StringBuilder data = new StringBuilder();
    private boolean closed = false;

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream already closed!");
        }
        data.append((char) b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Stream already closed!");
        }
        data.append(new String(b, off, len));
    }

    @Override
    public void close() throws IOException {
        super.close();
        closed = true;
    }

    public String getData() {
        return data.toString();
    }

    public boolean isClosed() {
        return closed;
    }
}