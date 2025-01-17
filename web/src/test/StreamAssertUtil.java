import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StreamAssertUtil {
    public static void assertStreamClosed(MockClientOutputStream clientStream) {
        assertTrue(clientStream.isClosed(), "Stream should be closed.");
    }

    public static void assertOutputData(MockClientOutputStream clientStream, String expectedOutput) {
        assertEquals(expectedOutput, clientStream.getData(), "Output written to the client does not match expected data.");
    }
}