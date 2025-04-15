package erangel.base;

import erangel.filter.FilterDef;
import erangel.filter.FilterMap;
import erangel.resource.ResourceManager;

import javax.servlet.FilterConfig;
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
    void addApplicationListener(String listener);

    /**
     * 返回为该应用程序配置的监听器类名集合。
     */
    String[] findApplicationListeners();


    /**
     * 从当前上下文中移除一个webAPp监听器。
     *
     * @param listener 要移除的监听器的名称。
     */
    void removeApplicationListener(String listener);

    /**
     * 获取与此上下文相关联的显示名称。
     *
     * @return 表示上下文显示名称的字符串。
     */
    String getDisplayName();

    /**
     * 设置与此上下文相关联的显示名称。
     *
     * @param displayName 可读名称
     */
    void setDisplayName(String displayName);

    /**
     * 查找给定的 servlet 名称或模式的 servlet 映射。
     *
     * @param s 要搜索映射的 servlet 名称或 URL 模式。
     * @return 与给定 servlet 名称或模式关联的 servlet 映射字符串，
     * 如果未找到映射，则返回 null。
     */
    String findServletMapping(String s);

    String[] findServletMappings();

    /**
     * 将指定的URL模式与当前上下文中的一个servlet名称进行映射。
     *
     * @param urlPattern  要映射到servlet的URL。
     * @param servletName 将处理与指定URL模式匹配的请求的servlet名称。
     */
    void addServletMapping(String urlPattern, String servletName);

    /**
     * 移除与指定的servlet名称或URL模式相关的servlet映射。
     *
     * @param urlPattern 要移除映射的servlet其URL
     */
    void removeServletMapping(String urlPattern);

    /**
     * 检索与指定过滤器名称相关的 {@code FilterConfig} 实例。
     *
     * @param name 要检索其配置的过滤器名称。
     *             这应该与过滤器配置中定义的名称匹配。
     * @return 与指定过滤器名称关联的 {@code FilterConfig} 对象，
     * 如果未找到这样的配置，则返回 {@code null}。
     */
    FilterConfig findFilterConfig(String name);

    /**
     * 搜索并检索与指定过滤器名称关联的 {@code FilterDef} 实例。
     *
     * @param filterName 要搜索的过滤器名称。这应该与过滤器配置中定义的名称相对应。
     * @return 与提供的过滤器名称关联的 {@code FilterDef}，如果未找到这样的 {@code FilterDef}，则返回 {@code null}。
     */
    FilterDef findFilterDef(String filterName);

    /**
     * 检索与当前上下文关联的所有 {@code FilterDef} 实例。
     * 每个 {@code FilterDef} 表示一个过滤器的定义，包括其名称、
     * 类以及任何相关的初始化参数。
     *
     * @return 一个 {@code FilterDef} 对象数组，表示上下文中定义的所有过滤器。
     * 如果没有定义过滤器，则返回一个空数组。
     */
    FilterDef[] findFilterDefs();

    /**
     * 添加过滤器映射到数组中
     *
     * @param filterMap 要添加到过滤器映射
     */
    void addFilterMap(FilterMap filterMap);

    /**
     * 返回与当前context关联的所有过滤器映射的集合
     *
     * @return 与当前context关联的所有过滤器映射的集合
     */
    FilterMap[] findFilterMaps();

    /**
     * 将一个 {@code FilterDef} 实例添加到当前上下文中。
     *
     * @param filterDef 要添加的 {@code FilterDef} 对象。它代表了过滤器的定义，包括其相关的类、名称和参数。
     */
    void addFilterDef(FilterDef filterDef);

    /**
     * 从上下文中移除指定的 {@code FilterDef} 实例。
     * {@code FilterDef} 代表过滤器的定义，包括其名称、相关类和初始化参数。
     *
     * @param filterDef 要移除的 {@code FilterDef}。该参数应包含要从当前上下文中解除链接的过滤器配置。
     */
    void removeFilterDef(FilterDef filterDef);

    /**
     * 从当前上下文中移除给定的 {@code FilterMap}。
     * {@code FilterMap} 表示过滤器与相关的 URL 模式或 Servlet 名称之间的映射。
     *
     * @param filterMap 要移除的 {@code FilterMap} 实例。
     *                  它指定要移除的映射的过滤器名称、URL 模式
     *                  和/或 Servlet 名称。
     */
    void removeFilterMap(FilterMap filterMap);

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

    /**
     * 创建并返回一个与此上下文关联的新{@code Endpoint}实例。
     * {@code Endpoint}用于管理作为Web应用程序上下文一部分的servlet生命周期和配置。
     *
     * @return 一个用于管理servlet生命周期和配置的新{@code Endpoint}实例
     */
    Endpoint createEndpoint();

    /**
     * 设置此上下文的配置状态。
     *
     * @param configured 一个布尔值，指示此上下文是否已被配置
     */
    void setConfigured(boolean configured);
}
