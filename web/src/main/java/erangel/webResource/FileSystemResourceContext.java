package erangel.webResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * 封装web资源
 *
 * @author LILINJIAN
 * @version 2025/2/28
 */
public class FileSystemResourceContext implements ResourceContext {
    private final Path root;
    private long lastModified = 0;

    public FileSystemResourceContext(Path root) {
        this.root = root;
    }

    @Override
    public Path getResource(String path) {
        // 去除开头的 /
        return root.resolve(path.substring(1));
    }

    @Override
    public List<Path> listResources(String path) throws IOException {
        List<Path> resourceList = new ArrayList<>();
        Path dir = getResource(path);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            for (Path p : directoryStream) {
                resourceList.add(p);
            }
        }
        return resourceList;
    }

    /**
     * 检查文件资源自上次检查以来是否已被修改。
     *
     * @return 如果资源已被修改则返回 true， 否则返回 false
     * @throws IOException 如果在访问文件系统时发生 I/O 错误
     */
    @Override
    public boolean isModified() throws IOException {
        long currentLatest = getLatestModified(root.toFile());
        return currentLatest != this.lastModified;
    }

    /**
     * 递归遍历文件和子目录，返回其中最新的修改时间
     */
    private long getLatestModified(File file) {
        long latest = file.lastModified();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    long childModified = getLatestModified(child);
                    if (childModified > latest) {
                        latest = childModified;
                    }
                }
            }
        }
        return latest;
    }
}
