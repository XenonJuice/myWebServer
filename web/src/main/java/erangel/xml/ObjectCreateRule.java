package erangel.xml;

import org.xml.sax.Attributes;

import static erangel.xml.MiniDigester.getAttribute;

/**
 * 根据<xxx class="SomeClass"/>反射实例话一个对象，并入stack
 */
public class ObjectCreateRule implements Rule {
    private final String attrName;

    public ObjectCreateRule() {
        this("class");
    }

    public ObjectCreateRule(String attrName) {
        this.attrName = attrName;
    }

    @Override
    public void begin(String path, Attributes attrs, MiniDigester d) {
        try {
            String clazz = getAttribute(attrs, attrName);
            if (clazz == null)
                throw new IllegalArgumentException("Missing attribute '" + attrName + "' on " + path);
            Object instance = Class.forName(clazz).getDeclaredConstructor().newInstance();
            d.push(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}