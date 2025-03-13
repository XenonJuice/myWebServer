package erangel;

import erangel.resource.ResourceManager;

import javax.servlet.ServletContext;

/**
 * 代表一个Web程序上下文
 *
 * @author LILINJIAN
 * @version 2025/02/20
 */
public interface Context extends Vas {
    /**
     * 获取资源的根目录
     */
    String getBasePath();

    /**
     * 设置资源的根目录
     */
    String setRealPath(String path);

    /**
     * 取得是否可重载标志位
     */
    boolean getReloadable();

    /**
     * 返回servlet上下文。
     */
    ServletContext getServletContext();

    /**
     * 获取关联的本地资源。
     *
     * @return 一个表示关联本地资源的 LocalResource，
     * 如果没有关联的资源，则返回 null。
     */
    ResourceManager getResources();

    /**
     * 重载
     */
    void reload();

    /**
     * 查找给定文件扩展名的 MIME 类型映射。
     *
     * @param ext 要查找 MIME 类型的文件扩展名，通常不包含前导点（例如，“txt”或“png”）。
     * @return 与提供的文件扩展名对应的 MIME 类型，
     * 如果不存在映射则返回 null。
     */
    String findMimeMapping(String ext);

    /**
     * 检索与上下文关联的应用程序事件监听器数组。
     *
     * @return 一个表示应用程序事件监听器的对象数组。
     * 如果没有关联的监听器，返回一个空数组。
     */
    Object[] getApplicationEventListeners();

    /**
     * 获取与此上下文相关联的显示名称。
     *
     * @return 表示上下文显示名称的字符串。
     */
    String getDisplayName();
}
