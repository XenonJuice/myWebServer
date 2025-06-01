package livonia.mapper;

import livonia.base.Context;
import livonia.base.Endpoint;
import livonia.base.Mapper;
import livonia.base.Vas;
import livonia.connector.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static livonia.base.Const.CharPunctuationMarks.CHAR_SOLIDUS;
import static livonia.base.Const.commonCharacters.SOLIDUS;

public class ContextMapper implements Mapper {
    private static final Logger logger = LoggerFactory.getLogger(ContextMapper.class);
    //<editor-fold desc = "attr">
    private Context context = null;

    //</editor-fold>
    //<editor-fold desc = "override">
    @Override
    public Vas getVas() {
        return context;
    }

    @Override
    public void setVas(Vas vas) {
        context = (Context) vas;
    }

    @Override
    public Vas map(HttpRequest request, boolean writeRequest) {
        // 获取上下文路径，例如 http：//local：1111/LLJ/LLJ—HOME 则上下文路径为/LLJ
        String contextPath = request.getContextPath();
        // 获取请求URI 按照上面的例子则为 /LLJ/LLJ-HOME
        String requestURI = request.getRequestURI();
        // 获取相对路径  按照上面的例子则为 /LLJ-HOME
        String relativeURI = requestURI.substring(contextPath.length());
        logger.debug("contextPath:{},requestURI:{},relativeURI:{}", contextPath, requestURI, relativeURI);

        Endpoint endpoint = null;
        String servletPath = null;
        String pathInfo = null;
        // 按照优先顺序依次执行匹配
        endpoint = exactMatch(relativeURI);
        if (endpoint != null) {
            servletPath = relativeURI;
        } else {
            endpoint = prefixMatch(relativeURI);
            if (endpoint != null) {
                servletPath = matchedServletPath(relativeURI);
                pathInfo = matchedPathInfo(relativeURI, servletPath);
            } else {
                endpoint = extensionMatch(relativeURI);
                if (endpoint != null) {
                    servletPath = relativeURI;
                } else {
                    endpoint = defaultMatch();
                    if (endpoint != null) {
                        servletPath = relativeURI;
                    }
                }
            }
        }

        // 若请求update，更新Request对象
        if (writeRequest && endpoint != null) {
            request.setEndpoint(endpoint);
            request.setServletPath(servletPath);
            request.setPathInfo(pathInfo);
        }

        return endpoint;
    }

    // 精确匹配方法
    private Endpoint exactMatch(String uri) {
        if (!SOLIDUS.equals(uri)) {
            String name = context.findServletMapping(uri);
            if (name != null) {
                return (Endpoint) context.findChild(name);
            }
        }
        return null;
    }

    // 前缀匹配方法
    private Endpoint prefixMatch(String uri) {
        String servletPath = uri;
        while (!servletPath.isEmpty()) {
            String name = context.findServletMapping(servletPath + "/*");
            if (name != null) {
                return (Endpoint) context.findChild(name);
            }
            int slash = servletPath.lastIndexOf(CHAR_SOLIDUS);
            if (slash < 0) break;
            servletPath = servletPath.substring(0, slash);
        }
        return null;
    }

    // 获取前缀匹配成功的servletPath
    private String matchedServletPath(String uri) {
        String servletPath = uri;
        while (!servletPath.isEmpty()) {
            String name = context.findServletMapping(servletPath + "/*");
            if (name != null) {
                return servletPath;
            }
            int slash = servletPath.lastIndexOf(CHAR_SOLIDUS);
            if (slash < 0) break;
            servletPath = servletPath.substring(0, slash);
        }
        return null;
    }

    // 获取前缀匹配成功的pathInfo
    private String matchedPathInfo(String uri, String servletPath) {
        String pathInfo = uri.substring(servletPath.length());
        return pathInfo.isEmpty() ? null : pathInfo;
    }

    // 扩展名匹配方法
    private Endpoint extensionMatch(String uri) {
        int slash = uri.lastIndexOf('/');
        int period = uri.lastIndexOf('.');
        if (period > slash) {
            String pattern = "*" + uri.substring(period);
            String name = context.findServletMapping(pattern);
            if (name != null) {
                return (Endpoint) context.findChild(name);
            }
        }
        return null;
    }

    // 默认匹配方法
    private Endpoint defaultMatch() {
        String name = context.findServletMapping("/");
        if (name != null) {
            return (Endpoint) context.findChild(name);
        }
        return null;
    }
    //</editor-fold>

}
