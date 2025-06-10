package livonia.XMLParse;

import livonia.base.Vas;
import org.xml.sax.Attributes;

import java.lang.reflect.Method;

//  Host 和 Context 是运行时动态添加的
public class CopyClassLoaderFromParentRule implements Rule {
    private final MiniDigester digester;

    public CopyClassLoaderFromParentRule(MiniDigester digester) {
        this.digester = digester;
    }

    @Override
    public void begin(String path, Attributes attrs, MiniDigester d) {
        try {
            // 从stack中取出父容器与子容器（stack顶部为子容器）
            Vas child = digester.peek(0);
            Vas parent = digester.peek(1);
            // 通过反射调用 parent.getParentClassLoader()
            Method m = parent.getClass().getMethod("getParentClassLoader");
            ClassLoader cl = (ClassLoader) m.invoke(parent);
            // 将父容器的类加载器设置给子容器
            child.setParentClassLoader(cl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy parent class loader", e);
        }
    }
}
