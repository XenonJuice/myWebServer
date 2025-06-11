package filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 字符编码过滤器
 */
public class CharacterEncodingFilter implements Filter {

    private String encoding = "UTF-8";
    private boolean forceEncoding = false;
    private String filterName;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterName = filterConfig.getFilterName();
        String encodingParam = filterConfig.getInitParameter("encoding");
        if (encodingParam != null) {
            encoding = encodingParam;
        }

        String forceParam = filterConfig.getInitParameter("force");
        if ("true".equalsIgnoreCase(forceParam)) {
            forceEncoding = true;
        }
        
        System.out.println("[testServlet - CharacterEncodingFilter] 初始化完成: encoding=" + encoding + ", forceEncoding=" + forceEncoding);
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        
        // 记录请求前的编码
        String originalEncoding = request.getCharacterEncoding();
        System.out.println("[testServlet - CharacterEncodingFilter] 处理请求: " + uri);
        System.out.println("  原始编码: " + (originalEncoding != null ? originalEncoding : "null"));
        
        // 设置请求编码
        if (forceEncoding || request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(encoding);
            System.out.println("  设置请求编码为: " + encoding);
        }

        // 设置响应编码
        response.setCharacterEncoding(encoding);
        System.out.println("  设置响应编码为: " + encoding);

        // 继续过滤器链
        chain.doFilter(request, response);
        
        System.out.println("[testServlet - CharacterEncodingFilter] 请求处理完成: " + uri);
    }

    public void destroy() {
        System.out.println("[testServlet - CharacterEncodingFilter] 销毁");
    }
}