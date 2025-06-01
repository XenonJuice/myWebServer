package livonia.XMLParse;

import livonia.lifecycle.LifecycleListener;
import org.xml.sax.Attributes;

/**
 * 在某个组件上添加默认的LifecycleListener。
 */
public class InnerListenerRule implements Rule {

    private final String defaultClassName;
    private final String classNameAttr;

    public InnerListenerRule(String defaultClassName, String classNameAttr) {
        this.defaultClassName = defaultClassName;
        this.classNameAttr = classNameAttr;
    }

    public InnerListenerRule(String defaultClassName) {
        this(defaultClassName, null);
    }

    @Override
    public void begin(String path, Attributes attributes, MiniDigester d) {
        Object parent = d.peek();

        try {
            String className = defaultClassName;
            if (classNameAttr != null) {
                String specified = attributes.getValue(classNameAttr);
                if (specified != null && !specified.isEmpty()) {
                    className = specified;
                }
            }

            Class<?> clazz = Class.forName(className);

            try {
                LifecycleListener instance;
                instance = (LifecycleListener) clazz.getDeclaredConstructor().newInstance();
                // 调用 parent 的 addLifecycleListener 方法
                var method = parent.getClass().getMethod("addLifecycleListener", LifecycleListener.class);
                method.invoke(parent, instance);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to instantiate class: " + className, e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to add LifecycleListener: " + defaultClassName, e);
        }
    }
}
