package livonia.XMLParse;

import java.lang.reflect.Method;

/**
 * 在元素解析完后，将子对象pop from stack，调用父对象的方法注入
 */
public class SetNextRuleAccessible implements Rule {
    private final String method;

    public SetNextRuleAccessible(String method) {
        this.method = method;
    }

    @Override
    public void end(String path, MiniDigester d) {
        Object child = d.pop();
        Object parent = d.peek();
        Class<?> childClass = child.getClass();

        try {
            for (Method m : parent.getClass().getMethods()) {
                // 包含继承的 public
                if (!m.getName().equals(method)) continue;
                if (m.getParameterCount() != 1) continue;

                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isAssignableFrom(childClass)) {          // 兼容父类型
                    m.invoke(parent, child);
                    return;                                            // 调用成功
                }
            }
            // 若 public 没找到，再扫 declared (protected/private)
            for (Method m : parent.getClass().getDeclaredMethods()) {
                if (!m.getName().equals(method) || m.getParameterCount() != 1)
                    continue;
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isAssignableFrom(childClass)) {
                    m.setAccessible(true);
                    m.invoke(parent, child);
                    return;
                }
            }
            throw new NoSuchMethodException(
                    "No suitable " + method + "(...) in " + parent.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
