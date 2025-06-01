package livonia.resource;


import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * 表示 JAR 文件中的一个资源。
 * 提供对 JAR 条目的元数据和内容的访问。实现了 LocalResource 接口。
 * （针对存放在 WEB-INF/lib 中的文件）
 *
 * @author LILINJIAN
 * @version 2025/3/6
 */
public class JarResource implements LocalResource {
    // 表示整个 jar 包文件，可以用它来遍历 jar 包内的所有条目。
    private final JarFile jarFile;
    // 表示 jar 包内的一个具体资源（例如一个 .class 文件或其他文件）
    private final JarEntry jarEntry;
    // 指向JAR 文件在磁盘上的实际位置
    private final Path jarPath;

    public JarResource(JarFile jarFile, JarEntry jarEntry, Path jarPath) {
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
        this.jarPath = jarPath;
    }

    @Override
    public boolean exists() {
        return jarEntry != null;
    }

    @Override
    public boolean isDirectory() {
        return jarEntry.isDirectory();
    }

    @Override
    public boolean canRead() {
        return jarEntry.getSize() > 0 && !jarEntry.isDirectory();
    }

    @Override
    public byte[] getContent() {
        try (InputStream is = jarFile.getInputStream(jarEntry)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return jarEntry.getName();
    }

    @Override
    public long getLastModified() {
        // 实时去文件系统读 JAR 本身的最后修改时间
        try {
            return Files.getLastModifiedTime(jarPath).toMillis();
        } catch (IOException e) {
            // 回退到条目时间戳
            return jarEntry.getTime();
        }
    }

    @Override
    public URL getURL() {
        // 实际路径 /Users/LLJ/projects/myapp/lib/myapp.jar，
        // 返回类似
        // jar:file:/Users/LLJ/projects/myapp/lib/myapp.jar!/com/example/LLJ.class
        String jarPath = jarFile.getName();
        try {
            return new URL("jar:file:" + jarPath + "!/" + jarEntry.getName());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return jarFile.getInputStream(jarEntry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}