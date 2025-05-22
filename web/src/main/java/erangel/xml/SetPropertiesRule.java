package erangel.xml;

import org.xml.sax.Attributes;

import java.beans.Introspector;
import java.util.Arrays;

import static erangel.xml.MiniDigester.convert;

/**
 * 把当前xml元素的属性，自动通过setter注入当前对象中
 */
public  class SetPropertiesRule implements Rule {
    @Override
    public void begin(String path, Attributes attrs, MiniDigester d) {
        Object bean = d.peek();
        for (int i = 0; i < attrs.getLength(); i++) {
            String name = attrs.getQName(i);
            if (name == null || name.isEmpty()) name = attrs.getLocalName(i);
            String val = attrs.getValue(i);
            setProperty(bean, name, val);
        }
    }

    private void setProperty(Object bean, String name, String value) {
        try {
            var pds = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
            Arrays.stream(pds)
                    .filter(p -> p.getName().equals(name) && p.getWriteMethod() != null)
                    .findFirst()
                    .ifPresent(p -> {
                        try { p.getWriteMethod().invoke(bean, convert(p.getPropertyType(), value)); }
                        catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {}
    }
}