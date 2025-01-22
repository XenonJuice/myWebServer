import java.io.IOException;
import java.io.InputStream;

public class MockClientInputStream extends InputStream {
    private final String data;
    private int position = 0;
    private boolean closed = false;

    public MockClientInputStream(String data) {
        this.data = data;
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("Stream already closed!");
        }
        if (position >= data.length()) {
            return -1; // End of stream
        }
        return data.charAt(position++);
    }

    @Override
    public void close() throws IOException {
        super.close();
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public int getPosition() {
        return position;
    }
}