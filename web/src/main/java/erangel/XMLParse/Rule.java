package erangel.XMLParse;

import org.xml.sax.Attributes;

public interface Rule {
    default void begin(String path, Attributes attrs, MiniDigester digester) {
    }

    default void body(String path, String text, MiniDigester digester) {
    }

    default void end(String path, MiniDigester digester) {
    }
}
