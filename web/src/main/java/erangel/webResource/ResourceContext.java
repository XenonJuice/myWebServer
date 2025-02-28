package erangel.webResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 封装web资源
 * @author LILINJIAN
 * @version 2025/2/28
 */
public interface ResourceContext {
    /**
     * 根据路径返回对应的资源文件
     *
     * @param path 相对于 web 应用根目录的路径，如 "/WEB-INF/classes"
     * @return 对应的 Path 对象
     */
    Path getResource(String path);

    /**
     * 列出某个目录下的所有资源
     *
     * @param path 目录路径
     * @return 该目录下所有资源的集合
     * @throws IOException 如果目录读取出错
     */
    List<Path> listResources(String path) throws IOException;
}