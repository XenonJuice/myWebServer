package erangel.Resource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@link LocalResource} 接口的一个具体实现
 * 表示一个基于文件的资源，使用 {@link Path} 来定位该文件。
 * （针对直接存放在 WEB-INF/classes 中的文件）
 * @author LILINJIAN
 * @version 2025/3/6
 */
public class FileResource implements LocalResource {

    private final Path path;

    public FileResource(Path path) {
        this.path = path;
    }

    @Override
    public boolean exists() {
        return Files.exists(path);
    }

    @Override
    public byte[] getContent() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public long getLastModified() throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    @Override
    public URL getURL() throws MalformedURLException {
        return path.toUri().toURL();
    }
}
//<editor-fold desc = "XXXXXXX">
//</editor-fold>
//<editor-fold desc = "XXXXXXX">
//</editor-fold>
//<editor-fold desc = "XXXXXXX">
//</editor-fold>
//<editor-fold desc = "XXXXXXX">
//</editor-fold>
//<editor-fold desc = "XXXXXXX">
//</editor-fold>
//<editor-fold desc = "XXXXXXX">
//</editor-fold>

