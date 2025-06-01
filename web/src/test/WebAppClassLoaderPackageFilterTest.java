import livonia.loader.WebAppClassLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 过滤条件，
 */
public class WebAppClassLoaderPackageFilterTest {

    private WebAppClassLoader webAppClassLoader;

    @BeforeEach
    public void setUp() {
        webAppClassLoader = new WebAppClassLoader();
        webAppClassLoader.setDelegate(true);
    }

    /**
     * 测试：当要被过滤的包名为 "javax"，检查加载 "javax.servlet.Servlet" 是否被拒绝
     * 期望抛出 ClassNotFoundException
     */
    @Test
    public void testLoadClass_FilteredByPackage() {
        // FilterPackageNames.PACKAGE_JAVAX 中的包名为 "javax"（示例）
        // 假设 classFilter 方法会过滤掉任何以 "javax" 开头的包
        String filteredClassName = "javax.servlet.Servlet";

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> webAppClassLoader.loadClass(filteredClassName)
        );
    }

    /**
     * 测试：当要被过滤的具体类名为 "javax.servlet.Servlet"，加载该类时是否抛出异常
     * 若已在包名检测中被判定，此处可视为叠加验证。
     */
    @Test
    public void testLoadClass_FilteredByClassName() {
        // FilterClassNames.CLASS_SERVLET 中的类名为 "javax.servlet.Servlet"
        String filteredClassName = "javax.servlet.Servlet";

        Assertions.assertThrows(
                ClassNotFoundException.class,
                () -> webAppClassLoader.loadClass(filteredClassName)
        );
    }

    /**
     * 测试：加载普通系统类（如java.lang.String），不应被过滤，应能正常返回
     */
    @Test
    public void testLoadClass_NotFiltered() throws ClassNotFoundException {
        String notFilteredClass = "java.lang.String";
        Class<?> clazz = webAppClassLoader.loadClass(notFilteredClass);

        Assertions.assertNotNull(clazz);
        Assertions.assertEquals(notFilteredClass, clazz.getName());
    }

    /**
     * 可根据需求，测试其他包名、类名是否被过滤
     * 例如：若仅 "javax.servlet" 被过滤，则加载 "javax.sql.DataSource" 可能不被过滤
     */
    @Test
    public void testLoadClass_AnotherJavaxClass() {
        // 若过滤规则仅限于 "javax.servlet" 下的类，而对 javax.sql 不作过滤，测试可验证此差异
        String someClass = "javax.sql.DataSource";
        // 具体看 classFilter 内部逻辑决定是否抛出
        // 如果 classFilter 未过滤该类，但类本身不存在或不可达，也会导致 ClassNotFoundException
        // 故在真实测试中通常更换为一定存在的类或自己打包的JAR内的类
        Assertions.assertThrows(ClassNotFoundException.class,
                () -> webAppClassLoader.loadClass(someClass),
                "若类真实存在且未被过滤，应能正常加载；此处仅作演示");
    }
}
