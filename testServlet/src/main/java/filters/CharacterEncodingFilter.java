package filters;

import javax.servlet.*;
import java.io.IOException;

/**
 * 字符编码过滤器
 */
public class CharacterEncodingFilter implements Filter {

    private String encoding = "UTF-8";
    private boolean forceEncoding = false;

    public void init(FilterConfig filterConfig) throws ServletException {
        String encodingParam = filterConfig.getInitParameter("encoding");
        if (encodingParam != null) {
            encoding = encodingParam;
        }

        String forceParam = filterConfig.getInitParameter("force");
        if ("true".equalsIgnoreCase(forceParam)) {
            forceEncoding = true;
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        // 设置请求编码
        if (forceEncoding || request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(encoding);
        }

        // 设置响应编码
        response.setCharacterEncoding(encoding);

        // 继续过滤器链
        chain.doFilter(request, response);
    }

    public void destroy() {
        // 清理资源
    }
}