package filters;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
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
        System.out.println("LoggingFilter initialized");
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

        System.out.println("[" + new Date() + "] " + method + " " + uri +
                (queryString != null ? "?" + queryString : "") +
                " from " + remoteAddr);

        // 继续过滤器链
        chain.doFilter(request, response);

        // 记录响应信息
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("[" + new Date() + "] Response completed in " +
                duration + "ms");
    }

    /**
     * 销毁过滤器
     */
    public void destroy() {
        System.out.println("LoggingFilter destroyed");
        this.filterConfig = null;
    }
}