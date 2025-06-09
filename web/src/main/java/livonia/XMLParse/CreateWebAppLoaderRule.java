package livonia.XMLParse;

import livonia.base.Loader;
import livonia.base.Vas;
import org.xml.sax.Attributes;

public class CreateWebAppLoaderRule implements Rule {
    private final MiniDigester digester;
    private final String loaderClassName;

    public CreateWebAppLoaderRule(String loaderClassName, MiniDigester digester) {
        this.digester = digester;
        this.loaderClassName = loaderClassName;
    }

    @Override
    public void begin(String path, Attributes attrs, MiniDigester d) {
        // 取出当前容器（即为Context）
        Vas vas = digester.peek();
        // 反射创建一个WebAppLoader
        try {
            Class<?> loaderClass = Class.forName(loaderClassName);
            // 取得WebAppLoader构造函数需要的参数类型
            Class<?> type = ClassLoader.class;
            // 取得容器的父容器的类加载器
            ClassLoader arg = vas.getParentClassLoader();
            // 取得方法
            // Method m = vas.getClass().getMethod("setLoader", Loader.class);
            // 实例化
            Loader loader = (Loader) loaderClass.getDeclaredConstructor(type).newInstance(arg);
            // m.invoke(vas, loader);
            digester.push(loader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void end(String path, MiniDigester d) {
        digester.pop();

    }
}
