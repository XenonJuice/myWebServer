package erangel.core;

import erangel.base.Const;
import erangel.base.Context;
import erangel.base.Host;
import erangel.log.BaseLogger;
import erangel.resource.ResourceManager;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static erangel.base.Const.commonCharacters.DOT;
import static erangel.base.Const.commonCharacters.SOLIDUS;

/**
 * 表示一个web程序的上下文环境
 *
 * @author LILINJIAN
 * @version 2025/02/25
 */
public class WebApplicationContext implements ServletContext {
    //<editor-fold dest="attr">
    // logger
    private static final Logger logger = BaseLogger.getLogger(WebApplicationContext.class);
    // 用于处理过时方法
    private final Enumeration<Servlet> emptyEnumeration = Collections.emptyEnumeration();
    private final Enumeration<String> emptyStringEnumeration = Collections.emptyEnumeration();

    /**
     * 一个存储WebApplicationContext上下文中的属性的映射。
     * 键是类型为String的属性名称，值是相应的对象。
     * 该映射用于动态管理应用特定的属性
     */
    private final Map<String, Object> attr = new ConcurrentHashMap<>();

    /**
     * 一个在只读上下文中保存属性的映射，用于web应用程序。
     * 键是表示属性名称的字符串，值是相应的对象。
     * 该映射用于存储在应用上下文生命周期内不应被修改的不可变属性。
     */
    private final Map<String, Object> readOnlyAttr = new ConcurrentHashMap<>();

    /**
     * 关联的Context
     */
    private final Context context;

    /**
     * 存储与web应用程序上下文相关的参数的键值对映射。
     */
    private final Map<String, String> param = new ConcurrentHashMap<>();

    /**
     * 表示文件系统或目录结构的基本路径。
     */
    private String basePath = "";

    //</editor-fold>
    //<editor-fold dest="构造器">
    public WebApplicationContext(String bath, Context context) {
        this.basePath = bath;
        this.context = context;
    }

    //</editor-fold>
    //<editor-fold dest="getter & setter">
    public String getBasePath() {
        return this.basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    //</editor-fold>
    //<editor-fold dest="实现一些基本方法">

    /**
     * 返回web应用程序的上下文路径。
     *
     * <p>上下文路径是请求URI中用于选择请求上下文的部分。上下文路径总是在请求URI中首先出现。
     * 如果该上下文是位于Web服务器URL命名空间根部的“根”上下文，则该路径将为空字符串。
     * 否则，如果上下文不是根服务器命名空间的根，则该路径以/字符开头，但不以/字符结尾。
     *
     * <p>可能一个servlet容器会通过多个上下文路径匹配一个上下文。
     * 在这种情况下，{@link HttpServletRequest#getContextPath()}将返回请求实际使用的上下文路径，
     * 这可能与本方法返回的路径不同。本方法返回的上下文路径应视为应用程序的主要或首选上下文路径。
     *
     * @return web应用程序的上下文路径，根上下文返回""
     * @see HttpServletRequest#getContextPath()
     * @since Servlet 2.5
     */
    @Override
    public String getContextPath() {
        return this.context.getBasePath();
    }

    /**
     * 返回一个 <code>ServletContext</code> 对象，该对象对应于服务器上的指定 URL。
     *
     * <p>此方法允许 servlet 访问服务器的各个部分的上下文，并根据需要从上下文中获取 {@link RequestDispatcher} 对象。
     * 给定的路径必须以 <tt>/</tt> 开头，相对于服务器的文档根进行解释，并与此容器中托管的其他 Web 应用程序的上下文根进行匹配。
     *
     * <p>在安全敏感的环境中，servlet 容器可能会为给定的 URL 返回 <code>null</code>。
     *
     * @param uripath 一个 <code>String</code>，指定容器中另一个 Web 应用程序的上下文路径。
     * @return 对应于命名 URL 的 <code>ServletContext</code> 对象；如果不存在或容器希望限制此访问，则返回 null。
     * @see RequestDispatcher
     */
    @Override
    public ServletContext getContext(String uripath) {
        if (uripath == null || !uripath.startsWith(SOLIDUS)) {
            return null;
        }
        String path = context.getBasePath();
        if (!path.endsWith(SOLIDUS)) path = path + SOLIDUS;
        if (uripath.startsWith(path)) return this;
        Host host = (Host) context.getParent();
        Context child = host.map(uripath);
        if (child != null) {
            return child.getServletContext();
        } else {
            return null;
        }
    }
    //<editor-fold desc = "获取版本">

    /**
     * Returns the major version of the Servlet API that this
     * servlet container supports. All implementations that comply
     * with Version 4.0 must have this method return the integer 4.
     *
     * @return 4
     */
    @Override
    public int getMajorVersion() {
        return 3;
    }

    /**
     * Returns the minor version of the Servlet API that this
     * servlet container supports. All implementations that comply
     * with Version 4.0 must have this method return the integer 0.
     *
     * @return 0
     */
    @Override
    public int getMinorVersion() {
        return 3;
    }

    /**
     * 获取由此 ServletContext 表示的应用程序所基于的 Servlet 规范的主要版本。
     *
     * <p>返回的值可能与 {@link #getMajorVersion} 不同，后者返回 Servlet 容器所支持的 Servlet 规范的主要版本。
     *
     * @return 此 ServletContext 表示的应用程序所基于的 Servlet 规范的主要版本
     * @throws UnsupportedOperationException 如果此 ServletContext 被传递给未在
     *                                       <code>web.xml</code> 或 <code>web-fragment.xml</code> 中声明的
     *                                       {@link ServletContextListener#contextInitialized} 方法，
     *                                       或未用 {@link WebListener} 注释的 {@link ServletContextListener}。
     * @since Servlet 3.0
     */
    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    /**
     * Gets the minor version of the Servlet specification that the
     * application represented by this ServletContext is based on.
     *
     * <p>The value returned may be different from {@link #getMinorVersion},
     * which returns the minor version of the Servlet specification
     * supported by the Servlet container.
     *
     * @return the minor version of the Servlet specification that the
     * application represented by this ServletContext is based on
     * @throws UnsupportedOperationException if this ServletContext was
     *                                       passed to the {@link ServletContextListener#contextInitialized} method
     *                                       of a {@link ServletContextListener} that was neither declared in
     *                                       <code>web.xml</code> or <code>web-fragment.xml</code>, nor annotated
     *                                       with {@link WebListener}
     * @since Servlet 3.0
     */
    @Override
    public int getEffectiveMinorVersion() {
        return 3;
    }
    //</editor-fold>

    /**
     * 返回指定文件的MIME类型，如果未知则返回<code>null</code>。
     * MIME类型由 servlet 容器的配置决定，并且可以在web应用程序部署描述符中指定。
     * 常见的MIME类型包括<code>text/html</code>和<code>image/gif</code>。
     *
     * @param file 一个<code>String</code>，指定文件的名称
     * @return 一个<code>String</code>，指定文件的MIME类型
     */
    @Override
    public String getMimeType(String file) {
        if (file == null) return null;
        if (file.isEmpty()) return null;
        int i = file.lastIndexOf(DOT);
        if (i < 0) return null;
        String ext = file.substring(i + 1);
        if (ext.isEmpty()) return null;
        return context.findMimeMapping(ext);
    }

    /**
     * 返回一个类似目录的列表，其中包含所有资源的路径
     * 在 web 应用程序中，其最长的子路径与
     * 提供的路径参数匹配。
     *
     * <p>表示子目录路径的路径以 <tt>/</tt> 结尾。
     *
     * <p>返回的路径相对于 web
     * 应用程序的根目录，或相对于 <tt>/META-INF/resources</tt>
     * 目录中的 JAR 文件，位于 web 应用程序的
     * <tt>/WEB-INF/lib</tt> 目录，并且有一个前导 <tt>/</tt>。
     *
     * <p>返回的集合未由 {@code ServletContext} 对象支持，
     * 因此返回集合中的更改不会反映在 {@code ServletContext} 对象中，反之亦然。</p>
     *
     * <p>例如，对于包含以下内容的 web 应用程序：
     *
     * <pre>{@code
     *   /welcome.html
     *   /catalog/index.html
     *   /catalog/products.html
     *   /catalog/offers/books.html
     *   /catalog/offers/music.html
     *   /customer/login.jsp
     *   /WEB-INF/web.xml
     *   /WEB-INF/classes/com.acme.OrderServlet.class
     *   /WEB-INF/lib/catalog.jar!/META-INF/resources/catalog/moreOffers/books.html
     * }</pre>
     *
     * <tt>getResourcePaths("/")</tt> 将返回
     * <tt>{"/welcome.html", "/catalog/", "/customer/", "/WEB-INF/"}</tt>，
     * 而 <tt>getResourcePaths("/catalog/")</tt> 将返回
     * <tt>{"/catalog/index.html", "/catalog/products.html",
     * "/catalog/offers/", "/catalog/moreOffers/"}</tt>。
     *
     * @param path 用于匹配资源的部分路径，必须以<tt>/</tt>开头
     * @return 返回一个包含目录列表的集合，如果在
     * web 应用程序中没有路径以提供的路径
     * 开头的资源，则返回 null。
     * @since Servlet 2.3
     */
    @Override
    public Set<String> getResourcePaths(String path) {
        if (path == null) return null;
        ResourceManager resource = context.getResources();
        if (resource != null) {
            return resource.listResourcePaths(path);
        }
        return null;
    }

    /**
     * 返回与给定路径映射的资源的 URL。
     *
     * <p>路径必须以 <tt>/</tt> 开头，并被解释为相对于当前上下文根，
     * 或相对于web应用程序的 <tt>/WEB-INF/lib</tt>
     * 目录中 JAR 文件的 <tt>/META-INF/resources</tt> 目录。
     * 该方法将首先在web应用程序的文档根目录中搜索请求的资源，然后再搜索 <tt>/WEB-INF/lib</tt> 中的任何 JAR 文件。
     * 对于 <tt>/WEB-INF/lib</tt> 中的 JAR 文件的搜索顺序是未定义的。
     *
     * <p>此方法允许 servlet 容器从任何来源为 servlets 提供资源。资源可以位于本地或远程文件系统中、数据库中或在 <code>.war</code> 文件中。
     *
     * <p>Servlet 容器必须实现访问资源所需的 URL 处理程序和 <code>URLConnection</code> 对象。
     *
     * <p>如果没有资源映射到此路径名，则此方法返回 <code>null</code>。
     *
     * <p>某些容器可能允许使用 URL 类的方法向此方法返回的 URL 写入数据。
     *
     * <p>资源内容直接返回，因此请注意请求 <code>.jsp</code> 页面会返回 JSP 源代码。请改用 <code>RequestDispatcher</code> 来包含执行的结果。
     *
     * <p>此方法与 <code>java.lang.Class.getResource</code> 的目的不同，后者根据类加载器查找资源。此方法不使用类加载器。
     *
     * @param path 一个 <code>String</code>，指定资源的路径
     * @return 位于指定路径的资源，
     * 或者在该路径没有资源时返回 <code>null</code>
     */
    @Override
    public URL getResource(String path) {
        ResourceManager resource = context.getResources();
        if (resource != null) {
            return resource.getResource(path).getURL();
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        if (path == null) return null;
        ResourceManager resource = context.getResources();
        if (resource != null) {
            return resource.getResource(path).getInputStream();
        }
        return null;
    }

    @Override
    public void log(String msg) {
        logger.info("{}" + Const.PunctuationMarks.COLON + "{}", context.getDisplayName(), msg);
    }

    @Override
    public void log(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        File file = new File(basePath, path);
        return (file.getAbsolutePath());
    }

    @Override
    public String getServerInfo() {
        return "myWebServer/1.0";
    }

    @Override
    public String getInitParameter(String name) {
        return param.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(new HashSet<>(param.keySet()));
    }

    @Override
    public Object getAttribute(String name) {
        return attr.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> names = new LinkedHashSet<>(attr.keySet());
        return Collections.enumeration(names);
    }

    @Override
    public void setAttribute(String name, Object object) {
        if (object == null) attr.remove(name);
    }

    @Override
    public void removeAttribute(String name) {
        if (readOnlyAttr.containsKey(name)) return;
        Object value = attr.get(name);
        if (value == null) return;
        value = attr.remove(name);
        if (value == null) return;
        Object[] listeners = context.getApplicationListeners();
        if (listeners == null || listeners.length == 0) return;
        ServletContextAttributeEvent event = new ServletContextAttributeEvent(context.getServletContext(), name, value);
        for (Object l : listeners) {
            if (!(l instanceof ServletContextAttributeListener listener)) {
                continue;
            }
            try {
                listener.attributeRemoved(event);
            } catch (Throwable t) {
                return;
            }
        }
    }

    @Override
    public String getServletContextName() {
        return context.getDisplayName();
    }

    //</editor-fold>
    //<editor-fold desc = "NOOP">
    @Deprecated
    @Override
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }

    @Deprecated
    @Override
    public Enumeration<Servlet> getServlets() {
        return emptyEnumeration;
    }

    @Deprecated
    @Override
    public Enumeration<String> getServletNames() {
        return emptyStringEnumeration;
    }

    @Deprecated
    @Override
    public void log(Exception exception, String msg) {
        throw new UnsupportedOperationException("log(Exception exception, String msg) method is not supported in Servlet 3.0");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Map.of();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Map.of();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Set.of();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Set.of();
    }

    @Override
    public void addListener(String className) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {

    }


    @Override
    public String getVirtualServerName() {
        return "";
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return "";
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return "";
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {

    }
    //</editor-fold>


}
