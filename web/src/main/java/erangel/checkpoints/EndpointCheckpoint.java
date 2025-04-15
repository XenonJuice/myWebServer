package erangel.checkpoints;

import erangel.base.CheckpointContext;
import erangel.base.Context;
import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.core.DefaultContext;
import erangel.core.DefaultEndpoint;
import erangel.filter.ApplicationFilterChain;
import erangel.filter.ApplicationFilterConfig;
import erangel.filter.FilterDef;
import erangel.filter.FilterMap;
import erangel.log.BaseLogger;
import erangel.utils.Decoder;
import org.slf4j.Logger;

import javax.servlet.Servlet;
import javax.servlet.UnavailableException;
import java.util.function.Predicate;

import static erangel.base.Const.commonCharacters.*;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

public class EndpointCheckpoint extends CheckpointBase {
    //<editor-fold desc = "attr">
    private static final String info = "erangel.checkpoints.EndpointCheckpoint";
    private static final Logger logger = BaseLogger.getLogger(EndpointCheckpoint.class);
    // servlet配置的过滤器定抽象定义
    private FilterDef filterDef = null;

    //</editor-fold>
    //<editor-fold desc = "抽象方法实现">
    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void process(HttpRequest request, HttpResponse response, CheckpointContext context) throws Exception {
        logger.info("EndpointCheckpoint : process in {}", getVas().getName());
        boolean unavailable = false;
        Servlet servlet = null;
        Throwable t = null;
        DefaultEndpoint endpoint = (DefaultEndpoint) getVas();

        // 检查该context是否被标记为失效
        if (!((Context) endpoint.getParent()).getAvailable()) {
            unavailable = true;
            // 拒绝请求
            rejectRequest(request, response, SC_SERVICE_UNAVAILABLE);
        }
        // 这时候可能在中间时间点被标记为失效，再次检查
        if (!unavailable && endpoint.isUnavailable()) {
            logger.warn("EndpointCheckpoint : endpoint : {} is unavailable", endpoint.getName());
            if (response != null) {
                rejectRequest(request, response, SC_SERVICE_UNAVAILABLE);
            }
            unavailable = true;
        }
        // 分配一个servlet实例来处理请求
        try {
            if (!unavailable) servlet = endpoint.salloc();
        } catch (Throwable e) {
            logger.error("EndpointCheckpoint : allocate servlet error", e);
            t = e;
            handleException(request, response, t);
        }

        // 反馈响应
        try {
            assert response != null;
            response.sendAck();
        } catch (Throwable e) {
            logger.error("EndpointCheckpoint : send ack error", e);
            t = e;
            assert response != null;
            handleException(request, response, t);
            servlet = null;
        }

        // 调用servlet过滤器
        ApplicationFilterChain chain = createFilterChain(request, servlet);
        try {
            if (servlet != null) chain.doFilter(request, response);
        } catch (UnavailableException e) {
            logger.warn("EndpointCheckpoint : doFilter error , endpoint : {} is unavailable", endpoint.getName());
            endpoint.unavailable(e);
            long available = endpoint.getAvailable();
            if ((available > 0L) && available < Long.MAX_VALUE) {
                response.setDateHeader("Retry-After", available);
                rejectRequest(request, response, SC_SERVICE_UNAVAILABLE);
            }
        } catch (Throwable e) {
            logger.error("EndpointCheckpoint : doFilter error", e);
            t = e;
            handleException(request, response, t);
        } finally {
            // 清理过滤链
            try {
                chain.destroy();
            } catch (Throwable e) {
                logger.error("EndpointCheckpoint : destroy filter chain error", e);
                t = e;
                handleException(request, response, t);
            }
            // 将正在处理的请求数量-1
            try {
                if (servlet != null) endpoint.sfree();
            } catch (Throwable e) {
                logger.error("EndpointCheckpoint : free servlet error", e);
                t = e;
                handleException(request, response, t);
            }

            // 卸载已被标记为失效的servlet
            if (servlet != null && endpoint.getAvailable() == Long.MAX_VALUE) {
                try {
                    endpoint.unload();
                } catch (Throwable e) {
                    logger.error("EndpointCheckpoint : unload servlet error", e);
                    t = e;
                    handleException(request, response, t);
                }
            }
        }

    }

    //</editor-fold>
    //<editor-fold desc = "handle Exception">
    private void handleException(HttpRequest request, HttpResponse response, Throwable t) {
        request.setAttribute("ServletException", t);
        response.setStatus(SC_INTERNAL_SERVER_ERROR);
    }

    //</editor-fold>
    //<editor-fold desc = "创造过滤链">
    private ApplicationFilterChain createFilterChain(HttpRequest request, Servlet servlet) {
        ApplicationFilterChain filterChain = new ApplicationFilterChain();
        DefaultEndpoint endpoint = (DefaultEndpoint) getVas();
        DefaultContext context = (DefaultContext) endpoint.getParent();
        // 为过滤链设置对应的servlet
        filterChain.setServlet(servlet);

        // 从context中获取过滤器映射
        FilterMap[] filterMaps = context.findFilterMaps();
        // 若不存在过滤器配置，直接返回初始状态的过滤链
        if (filterMaps == null || filterMaps.length == 0) return filterChain;
        logger.info("EndpointCheckpoint : createFilterChain_filterMaps.length : {}", filterMaps.length);

        // 获取具体的请求的servlet名
        String contextPath = request.getContextPath();
        if (contextPath == null) contextPath = BLANK;
        String requestURI = Decoder.decode(request.getRequestURI());
        String requestPath = (requestURI.length() >= contextPath.length())
                ? requestURI.substring(contextPath.length()) : null;
        String servletName = endpoint.getName();
        // 通过URLPattern模式匹配，将匹配到的过滤器添加到过滤链中
        addFiltersToChain(
                filterMaps,
                fm -> matchFilterURL(fm, requestPath),
                filterChain,
                context
        );

        // 通过直接servlet名模式匹配，将匹配到的过滤器添加到过滤链中
        addFiltersToChain(
                filterMaps,
                fm -> matchFilterServlet(fm, servletName),
                filterChain,
                context
        );

        return filterChain;
    }
    //</editor-fold>
    //<editor-fold desc = "过滤器匹配方法">
    // toDO 制作几个以下规则的filter来测试

    /**
     * 将提供的请求路径与给定 FilterMap 中定义的 URL 模式进行匹配。
     * <p>
     * 该方法比较请求路径与过滤器的 URL 模式，以确定是否存在匹配。
     * 它支持精确匹配、路径模式匹配（例如，“/*”）和扩展名匹配（例如，“*.ext”）。
     *
     * @param filterMap   包含要匹配的 URL 模式的 FilterMap
     * @param requestPath 要检查与过滤器 URL 模式的传入请求路径
     * @return 如果请求路径与过滤器的 URL 模式匹配则返回 true，否则返回 false
     */
    private boolean matchFilterURL(FilterMap filterMap, String requestPath) {
        String filterPattern = filterMap.getUrlPattern();
        if (requestPath == null || filterPattern == null) return false;
        // 精确匹配
        if (filterPattern.equals(requestPath)) return true;
        // 路径匹配 ("/.../*")
        if (filterPattern.equals("/*")) return true;
        // 如以 "/*" 结尾，循环判断更深层的路径
        if (filterPattern.endsWith("/*")) {
            String path = requestPath;
            while (true) {
                // 当拼接路径和过滤器匹配路径一致时，返回 true
                if (filterPattern.equals(path + "/*")) return true;
                // 找出最后一个斜杠位置
                int slashIndex = path.lastIndexOf(SOLIDUS);
                // 如果没有找到斜杠，跳出循环
                if (slashIndex < 0) break;
                path = path.substring(0, slashIndex);
            }
            // 没有找到的话 返回false
            return false;
        }
        // 扩展名匹配
        if (filterPattern.startsWith("*.")) {
            // 找到请求路径中最后一个斜杠和最后一个点的位置
            int slashIndex = requestPath.lastIndexOf(SOLIDUS);
            int dotIndex = requestPath.lastIndexOf(DOT);
            // 当路径中存在点并且点的位置在最后一个斜杠之后时，尝试匹配扩展名
            if ((slashIndex >= 0) && (dotIndex > slashIndex)) {
                // 判断过滤器模式是否与请求路径的扩展名相匹配
                return filterPattern.equals(
                        "*." + requestPath.substring(dotIndex + 1));
            }
        }
        // 以上都没有找到的话 返回false
        return false;
    }

    /**
     * 确定给定的 Servlet 名称是否与提供的 FilterMap 中指定的 Servlet 名称匹配。
     *
     * @param filterMap   包含要匹配的 Servlet 名称的 FilterMap
     * @param servletName 要检查匹配的 Servlet 名称
     * @return 如果 FilterMap 中的 Servlet 名称与给定的 Servlet 名称匹配，则返回 true；否则返回 false。
     */
    private boolean matchFilterServlet(FilterMap filterMap, String servletName) {
        if (filterMap.getServletName() == null) return false;
        return filterMap.getServletName().equals(servletName);
    }


    //</editor-fold>
    //<editor-fold desc = "add">
    private void addFiltersToChain(
            FilterMap[] filterMaps,
            Predicate<FilterMap> matchCondition,
            ApplicationFilterChain filterChain,
            DefaultContext context) {
        for (FilterMap filterMap : filterMaps) {
            if (!matchCondition.test(filterMap)) {
                continue;
            }
            ApplicationFilterConfig filterConfig =
                    (ApplicationFilterConfig) context.findFilterConfig(filterMap.getFilterName());
            if (filterConfig == null) {
                logger.warn("EndpointCheckpoint : context : {} has no filter config : {} ，something may went wrong when server starting",
                        context.getName(), filterMap.getFilterName());
                continue;
            }
            filterChain.addFilter(filterConfig);
        }
    }
    //</editor-fold>
}
