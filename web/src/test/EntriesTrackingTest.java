import erangel.loader.WebAppClassLoader;
import erangel.webResource.FileSystemResourceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntriesTrackingTest {

    private WebAppClassLoader webAppClassLoader;
    private Path fixedWebInfClassesDir;

    @BeforeEach
    public void setUp() throws IOException {
        // 初始化 WebAppClassLoader 实例（假设存在无参构造函数）
        webAppClassLoader = new WebAppClassLoader();

        // 固定目录：/Users/lilinjian/webResource/WEB-INF 下的 classes 文件夹
        Path webInfPath = Paths.get("/Users/lilinjian/webResource/WEB-INF");
        fixedWebInfClassesDir = webInfPath.resolve("classes");

        // 如果 classes 文件夹不存在，则创建之
        if (!Files.exists(fixedWebInfClassesDir)) {
            Files.createDirectories(fixedWebInfClassesDir);
        }
        // 为了测试，确保 classes 文件夹下至少有两个 .class 文件
        Path dummyClass = fixedWebInfClassesDir.resolve("Dummy.class");
        if (!Files.exists(dummyClass)) {
            Files.createFile(dummyClass);
        }
        Path testClass = fixedWebInfClassesDir.resolve("Test.class");
        if (!Files.exists(testClass)) {
            Files.createFile(testClass);
        }
    }

    @Test
    public void testEntriesContainsAllResourcesForClasses() throws IOException {
        // 调用 addRepository，repository 参数设为 "/WEB-INF/classes/"，path 为固定的 classes 目录
        webAppClassLoader.addRepository("/WEB-INF/classes/", fixedWebInfClassesDir);

        // 利用 FileSystemResourceContext 模拟获得该目录下所有文件的列表
        FileSystemResourceContext context = new FileSystemResourceContext(fixedWebInfClassesDir);
        List<Path> expectedFiles = context.listResources(fixedWebInfClassesDir.toString());

        // 通过 getter 获取 WebAppClassLoader 内部的 tracking entries
        Map<String, WebAppClassLoader.ResourceEntry> entries = webAppClassLoader.getEntries();
        assertNotNull(entries, "entries 不应为 null");

        // 验证 expectedFiles 列表中的每个文件都被记录在 entries 中
        for (Path file : expectedFiles) {
            String key = file.toString();
            assertTrue(entries.containsKey(key),
                    "entries 中应包含文件 " + key + " 的跟踪记录");
        }
    }
}