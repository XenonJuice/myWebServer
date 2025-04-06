package erangel.xml;

/**
 * 在元素解析完后，将子对象pop from stack，调用父对象的方法注入
 */
public class SetNextRule implements Rule {
    private final String method;

    public SetNextRule(String method) {
        this.method = method;
    }

    @Override
    public void end(String path, MiniDigester d) {
        Object child = d.pop();
        Object parent = d.peek(Object.class);
        try {
            parent.getClass().getMethod(method, child.getClass()).invoke(parent, child);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
