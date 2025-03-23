package erangel.base;

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
    void setBasePath(String basePath);

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
     * 设置与此上下文相关联的资源对象。
     *
     * @param resources 关联的资源
     */
    void setResources(ResourceManager resources);


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
     * 获取与当前上下文关联的应用程序监听器数组。
     *
     * @return 一个对象数组，代表应用程序监听器。
     * 如果没有关联的监听器，则返回一个空数组。
     */
    Object[] getApplicationListeners();

    /**
     * 为当前上下文设置应用程序事件监听器。
     *
     * @param listeners 一个对象数组，表示要与上下文关联的应用程序事件监听器。
     */
    void setApplicationListeners(Object[] listeners);

    /**
     * 将应用程序事件监听器添加到当前上下文。
     *
     * @param listener 要添加的应用程序监听器。
     */
    void addApplicationListener(Object listener);

    /**
     * 返回为该应用程序配置的监听器类名集合。
     */
    String[] findApplicationListeners();

    /**
     * 获取与此上下文相关联的显示名称。
     *
     * @return 表示上下文显示名称的字符串。
     */
    String getDisplayName();

    /**
     * 查找给定的 servlet 名称或模式的 servlet 映射。
     *
     * @param s 要搜索映射的 servlet 名称或 URL 模式。
     * @return 与给定 servlet 名称或模式关联的 servlet 映射字符串，
     * 如果未找到映射，则返回 null。
     */
    String findServletMapping(String s);

    /**
     * 确定当前上下文是否可用。
     *
     * @return 如果上下文可用则返回 true，否则返回 false。
     */
    boolean getAvailable();

    /**
     * 设置上下文的可用性状态。
     *
     * @param available 一个布尔值，指示上下文是否应标记为可用或不可用。
     */
    void setAvailable(boolean available);

    /**
     * 返回此Web应用程序的上下文路径。
     */
    String getPath();

    /**
     * 设置此 web 应用程序的上下文路径。
     *
     * @param path 新的上下文路径
     */
    void setPath(String path);


}
