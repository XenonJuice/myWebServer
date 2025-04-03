package erangel.utils;

import java.nio.charset.Charset;

public class Decoder {
    public static String decode(String value, Charset charset) {
        return java.net.URLDecoder.decode(value, charset);
    }
}
