package erangel.xml;

import org.xml.sax.Attributes;

import static erangel.xml.MiniDigester.getAttribute;

/**
 * 根据<xxx class="SomeClass"/>反射实例话一个对象，并入stack
 */
public class ObjectCreateRule implements Rule {
    private final String attrName;
    private final Class<?> type;

    public ObjectCreateRule() {
        this.attrName = "class";
        this.type = null;
    }
    public ObjectCreateRule(String attrName) {
        this.attrName = attrName;
        this.type = null;
    }

    public ObjectCreateRule(Class<?> type) {
        this.type = type;
        this.attrName = null;
    }

    @Override
    public void begin(String path, Attributes attrs, MiniDigester d) {
        try {
            Object instance;
            if (type != null) {
                instance = type.getDeclaredConstructor().newInstance();
            } else {
                String clazz = getAttribute(attrs, attrName);
                if (clazz == null)
                    throw new IllegalArgumentException("Missing attribute '" + attrName + "' on " + path);
                instance = Class.forName(clazz).getDeclaredConstructor().newInstance();
            }
            d.push(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}