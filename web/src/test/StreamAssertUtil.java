import org.junit.jupiter.api.Assertions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StreamAssertUtil {
    public static void assertStreamClosed(MockClientOutputStream clientStream) {
        assertTrue(clientStream.isClosed(), "Stream should be closed.");
    }

    public static void assertOutputData(MockClientOutputStream clientStream, String expectedOutput) {
        assertEquals(expectedOutput, clientStream.getData(), "Output written to the client does not match expected data.");
    }

    public static void assertInputStreamClosed(MockClientInputStream clientStream) {
        Assertions.assertTrue(clientStream.isClosed(), "Stream should be closed properly.");
    }

    public static void assertStreamPosition(MockClientInputStream clientStream, int expectedPosition) {
        Assertions.assertEquals(expectedPosition, clientStream.getPosition(), "Stream position does not match expected value.");
    }
}