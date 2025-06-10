package livonia.XMLParse;

import org.xml.sax.Attributes;

import java.lang.reflect.Constructor;

public class ObjectCreateRule implements Rule {
    private final Class<?> defaultClass;
    private final String classAttr;

    public ObjectCreateRule(Class<?> defaultClass) {
        this(defaultClass, null);
    }

    public ObjectCreateRule(Class<?> defaultClass, String classAttr) {
        this.defaultClass = defaultClass;
        this.classAttr = classAttr;
    }

    @Override
    public void begin(String path, Attributes attrs, MiniDigester d) {
        Class<?> clazz = defaultClass;

        // 尝试从 XML 属性中读取 className
        if (classAttr != null) {
            String className = attrs.getValue(classAttr);
            if (className != null && !className.isEmpty()) {
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("className not found: " + className, e);
                }
            }
        }

        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object instance = ctor.newInstance();
            d.push(instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate class: " + clazz.getName(), e);
        }
    }
}
