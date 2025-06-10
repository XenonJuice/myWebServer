package livonia.base;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

/**
 * {@code Endpoint} 接口提供了管理Servlet的生命周期和配置的方法。
 * 扩展了 {@code Vas} 接口，继承了其层次容器结构和映射能力。
 */
public interface Endpoint extends Vas {
    /**
     * 加载并初始化该 servlet 的一个实例
     */
    void load() throws ServletException;

    /**
     * 卸载并销毁实例化的 servlet 实例。
     * 此方法通常用于释放 servlet 持有的任何资源，并在其被移除服务之前进行适当的清理。
     * 实现应确保所有资源都得到妥善释放，并且在 {@code load} 方法中完成的任何初始化都被逆转。
     *
     * @throws ServletException 如果在卸载过程中发生错误，
     *                          如清理资源或依赖项失败。
     */
    void unload() throws ServletException;

    /**
     * 为与此 {@code Endpoint} 关联的 servlet 分配资源。
     * 此过程可能涉及初始化 servlet 处理传入请求所需的必要组件。
     * 此操作应确保满足任何分配依赖项。
     *
     * @return
     * @throws ServletException 如果在资源分配过程中发生错误。
     */
    Servlet salloc() throws ServletException;

    /**
     * 释放与 servlet 关联的资源或执行清理任务。
     * 此方法被调用以释放特定于 servlet 的资源，如内存或对象引用，确保 servlet 被正确移除。
     * 子类或实现类应确保实现此方法以处理自定义资源的释放。
     *
     * @throws ServletException 如果在释放资源时发生错误。
     */
    void sfree() throws ServletException;

    /**
     * 设置与此 {@code Endpoint} 关联的 servlet 的类名。
     *
     * @param servletClass 要设置的 servlet 的完全限定类名
     */
    void setServletClass(String servletClass);

    /**
     * 根据提供的参数名称查找并检索初始化参数的值。
     *
     * @param name 要检索的初始化参数的名称
     * @return 与指定名称关联的初始化参数的值，
     * 如果该参数不存在，则返回 null
     */
    String findInitParam(String name);

    /**
     * 检索与 Endpoint 关联的初始化参数。
     * 此方法返回一个参数名称的数组，表示当前为此实例配置的所有初始化参数。
     *
     * @return 一个包含初始化参数名称的字符串数组。
     * 如果没有配置参数，则返回一个空数组。
     */
    String[] findInitParams();

    /**
     * 移除具有指定名称的初始化参数。
     * 如果没有具有给定名称的参数，则不执行任何操作。
     *
     * @param name 要移除的初始化参数的名称
     */
    void removeInitParam(String name);

    /**
     * 指示此Servlet当前是否不可用。
     *
     * @return {@code true} 如果不可用，
     * {@code false} 如果可用。
     */
    boolean isUnavailable();

    /**
     * 该方法将提供的 {@code UnavailableException} 设置为状态，
     * 表示相关的 servlet 或服务不可用。
     *
     * @param unavailable 存放关于服务不可用原因的 {@code UnavailableException} 对象
     **/
    void unavailable(UnavailableException unavailable);
}
