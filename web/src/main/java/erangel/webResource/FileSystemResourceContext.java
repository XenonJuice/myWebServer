package erangel.webResource;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;


/**
 * 封装web资源
 * @author LILINJIAN
 * @version 2025/2/28
 */
public class FileSystemResourceContext implements ResourceContext {
    private final Path root;

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
}
