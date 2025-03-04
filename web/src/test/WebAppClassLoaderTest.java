import erangel.loader.WebAppClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.net.URL;

// 示例测试类
public class WebAppClassLoaderTest {

    private WebAppClassLoader webAppClassLoader;

    @BeforeEach
    public void setUp() {
        URL[] urls = new URL[0];
        webAppClassLoader = new WebAppClassLoader("TestWebAppClassLoader", urls, this.getClass().getClassLoader());
    }

    /**
     * 测试：让WebAppClassLoader去加载本模块中的已知类
     */
    @Test
    public void testLoadClass_FoundInSamePackage() throws ClassNotFoundException {
        Class<?> clazz = webAppClassLoader.loadClass("WebAppClassLoaderTest");
        Assertions.assertNotNull(clazz);
        Assertions.assertEquals("WebAppClassLoaderTest", clazz.getName());
    }

    /**
     * 测试：尝试加载不存在的类，应抛出ClassNotFoundException
     */
    @Test
    public void testLoadClass_NotFound() {
        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> webAppClassLoader.loadClass("com.example.XXXX.ClassXYZ")
        );
    }

    /**
     * 测试：当 delegate 为 true 时，先从 parentClassLoader 尝试加载。
     */
    @Test
    public void testLoadClass_WithDelegate() throws ClassNotFoundException {
        // 修改WebAppClassLoader的delegate标志
        webAppClassLoader.setDelegate(true);
        // java.lang.String
        Class<?> clazz = webAppClassLoader.loadClass("java.lang.String");
        Assertions.assertNotNull(clazz);
        Assertions.assertEquals("java.lang.String", clazz.getName());
    }

    /**
     * 测试：delegate 为 false 时，先尝试自身加载，再委托给父加载器
     * 可设置自己的类路径，验证加载顺序
     */
    @Test
    public void testLoadClass_WithoutDelegate() throws ClassNotFoundException {
        webAppClassLoader.setDelegate(false);

        // 期望行为：WebAppClassLoader先尝试自身加载，如果找不到再交给父加载器
        // 这里同样以java.lang.String作为演示
        Class<?> clazz = webAppClassLoader.loadClass("java.lang.String");
        Assertions.assertNotNull(clazz);
        Assertions.assertEquals("java.lang.String", clazz.getName());
    }

    /**
     * 测试：已加载过的类的行为
     *
     */
    @Test
    public void testLoadClass_Cache() throws ClassNotFoundException {
        // 期望行为：WebAppClassLoader从缓存中取得已加载过的类
        Class<?> clazz = webAppClassLoader.loadClass("java.lang.String");
        Class<?> clazz0 = webAppClassLoader.loadClass("java.lang.String");
        Class<?> clazz1= webAppClassLoader.loadClass("java.lang.Float");
        Class<?> clazz2= webAppClassLoader.loadClass("java.lang.Double");
        Class<?> clazz4= webAppClassLoader.loadClass("java.lang.Float");

    }

    /**
     * 测试：对resolve参数的影响。当resolve为true时，会触发类的解析过程
     */
    @Test
    public void testLoadClass_WithResolve() throws ClassNotFoundException {
        // 注意：resolve阶段在JVM层面主要进行符号引用解析等工作，
        // 这里更多是验证API调用不出现异常
        Class<?> clazz = webAppClassLoader.loadClass("java.net.URL", true);
        Assertions.assertNotNull(clazz);
        Assertions.assertEquals("java.net.URL", clazz.getName());
    }

}
