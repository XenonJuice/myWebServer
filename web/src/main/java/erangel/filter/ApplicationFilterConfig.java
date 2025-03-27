package erangel.filter;

import erangel.core.DefaultContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

// 过滤器配置
public class ApplicationFilterConfig implements FilterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationFilterConfig.class);
    private final DefaultContext context;
    private javax.servlet.Filter filter = null;
    private FilterDef filterDef = null;

    public ApplicationFilterConfig(DefaultContext context, FilterDef filterDef) {
        this.context = context;
        setFilterDef(filterDef);
    }

    @Override
    public String getFilterName() {
        return filterDef.getFilterName();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public String getInitParameter(String name) {
        Map<String, String> params = filterDef.getParams();
        if (params.containsKey(name)) return params.get(name);
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        Map<String, String> params = filterDef.getParams();
        if (params == null) return Collections.emptyEnumeration();
        return Collections.enumeration(params.keySet());
    }

    public javax.servlet.Filter getFilter() throws ServletException {
        if (filter != null) return filter;
        String filterClass = filterDef.getFilterClass();
        ClassLoader cl = context.getLoader().getClassLoader();
        try {
            Class<?> clazz = cl.loadClass(filterClass);
            filter = (javax.servlet.Filter) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("context : {} start filter : {} failed",
                    context.getName(), filterClass, e);
        }
        filter.init(this);
        return filter;
    }

    public void destroy() {
        if (filter != null) {
            try {
                filter.destroy();
            } catch (Exception e) {
                logger.error("context : {} destroy filter : {} failed",
                        context.getName(), filterDef.getFilterName(), e);
            }
        }
    }

    public void setFilterDef(FilterDef filterDef) {
        this.filterDef = filterDef;
        if (filterDef == null) {
            if (filter != null) {
                this.filter.destroy();
                this.filter = null;
            }
        } else {
            if (filter == null) {
                try {
                    filter = getFilter();
                } catch (Exception e) {
                    logger.error("context : {} start filter : {} failed",
                            context.getName(), filterDef.getFilterName(), e);
                }
            }
        }
    }
}
