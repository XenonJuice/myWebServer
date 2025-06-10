package livonia.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Decoder {
    public static String decode(String value, Charset charset) {
        return java.net.URLDecoder.decode(value, charset);
    }

    public static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
