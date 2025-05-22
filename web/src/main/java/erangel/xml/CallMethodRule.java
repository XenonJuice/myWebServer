package erangel.xml;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static erangel.xml.MiniDigester.convert;

public class CallMethodRule implements Rule {
    private final String method;
    private final List<String> paramList;
    private final Class<?>[] paramTypes;

    public CallMethodRule(String method, List<String> paramList, Class<?>... paramTypes) {
        this.method = method;
        this.paramList = paramList;
        this.paramTypes = paramTypes;
    }

    @Override
    public void end(String path, MiniDigester d) {
        Object target = d.peek();
        try {
            Method m = target.getClass().getMethod(method, paramTypes);
            Object[] args = new Object[paramList.size()];
            for (int i = 0; i < paramList.size(); i++) {
                args[i] = convert(paramTypes[i], paramList.get(i));
            }
            m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
