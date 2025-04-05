package erangel.xml;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CallMethodRule implements Rule {
    private final String method;
    private final List<String> bodyTexts = new ArrayList<>();

    public CallMethodRule(String method) {
        this.method = method;
    }

    @Override
    public void body(String path, String text, MiniDigester d) {
        bodyTexts.add(text);
    }

    @Override
    public void end(String path, MiniDigester d) {
        Object target = d.peek(Object.class);
        try {
            Method[] methods = target.getClass().getMethods();
            for (Method m : methods) {
                if (!m.getName().equals(method)) continue;
                if (m.getParameterCount() == bodyTexts.size()) {
                    Object[] args = new Object[bodyTexts.size()];
                    Class<?>[] types = m.getParameterTypes();
                    for (int i = 0; i < args.length; i++) {
                        args[i] = MiniDigester.convert(types[i], bodyTexts.get(i));
                    }
                    m.invoke(target, args);
                    return;
                }
            }
            throw new IllegalArgumentException("No method found for: " + method);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
