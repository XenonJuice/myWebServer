package livonia.filter;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ApplicationFilterChain implements FilterChain {
    //<editor-fold desc = "attr">
    private Servlet servlet = null;
    private Iterator<ApplicationFilterConfig> filterIterator = null;
    private final List<ApplicationFilterConfig> filters = new ArrayList<>();

    //</editor-fold>
    //<editor-fold desc = "接口方法实现">
    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (filterIterator == null) {
            filterIterator = filters.iterator();
        }
        if (filterIterator.hasNext()) {
            ApplicationFilterConfig filterConfig = filterIterator.next();
            Filter filter = filterConfig.getFilter();
            filter.doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }

    //</editor-fold>
    //<editor-fold desc = "其他方法">
    public void addFilter(ApplicationFilterConfig filterConfig) {
        this.filters.add(filterConfig);
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    public void destroy() {
        this.filters.clear();
        this.servlet = null;
        this.filterIterator = null;
    }
    //</editor-fold>

}
