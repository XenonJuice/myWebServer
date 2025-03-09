package erangel.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static erangel.Const.webApp.DOTCLASS;

/**
 * {@link LocalResource} 接口的一个具体实现
 * 表示一个基于文件的资源，使用 {@link Path} 来定位该文件。
 * （针对直接存放在 WEB-INF/classes 中的文件）
 *
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
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    @Override
    public boolean canRead() {
        return Files.isReadable(path);
    }

    @Override
    public byte[] getContent()  {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URL getURL(){
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

