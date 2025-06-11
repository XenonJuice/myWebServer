package filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Servlet 2.3 版本的日志过滤器
 */
public class LoggingFilter implements Filter {

    private FilterConfig filterConfig;

    /**
     * 初始化过滤器
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        System.out.println("[testServlet - LoggingFilter] 初始化完成");
        System.out.println("  过滤器名称: " + filterConfig.getFilterName());
        System.out.println("  Servlet上下文: " + filterConfig.getServletContext().getServletContextName());
    }

    /**
     * 执行过滤
     */
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        // 转换为 HTTP 请求/响应
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 记录请求信息
        String remoteAddr = httpRequest.getRemoteAddr();
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String contentType = httpRequest.getContentType();
        String userAgent = httpRequest.getHeader("User-Agent");

        System.out.println("[testServlet - LoggingFilter] ========== 请求开始 ==========");
        System.out.println("  时间: " + new Date());
        System.out.println("  方法: " + method);
        System.out.println("  URI: " + uri);
        System.out.println("  查询字符串: " + (queryString != null ? queryString : "无"));
        System.out.println("  远程地址: " + remoteAddr);
        System.out.println("  Content-Type: " + (contentType != null ? contentType : "无"));
        System.out.println("  User-Agent: " + (userAgent != null ? userAgent : "无"));
        System.out.println("  Session ID: " + (httpRequest.getSession(false) != null ? httpRequest.getSession().getId() : "无会话"));

        // 继续过滤器链
        chain.doFilter(request, response);

        // 记录响应信息
        long duration = System.currentTimeMillis() - startTime;
        int status = httpResponse.getStatus();
        String responseContentType = httpResponse.getContentType();
        
        System.out.println("[testServlet - LoggingFilter] ========== 响应完成 ==========");
        System.out.println("  时间: " + new Date());
        System.out.println("  状态码: " + status);
        System.out.println("  Content-Type: " + (responseContentType != null ? responseContentType : "无"));
        System.out.println("  耗时: " + duration + "ms");
        System.out.println("");
    }

    /**
     * 销毁过滤器
     */
    public void destroy() {
        System.out.println("[testServlet - LoggingFilter] 销毁");
        this.filterConfig = null;
    }
}