package listeners;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单的请求监听器 - 跟踪每个请求的生命周期
 */
public class SimpleRequestListener implements ServletRequestListener {
    
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    
    /**
     * 请求初始化时调用
     */
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        int requestId = requestCounter.incrementAndGet();
        
        // 将请求ID保存到请求属性中
        request.setAttribute("requestId", requestId);
        
        System.out.println("[testServlet - SimpleRequestListener] 请求初始化");
        System.out.println("  请求ID: #" + requestId);
        System.out.println("  URI: " + request.getRequestURI());
        System.out.println("  方法: " + request.getMethod());
        System.out.println("  协议: " + request.getProtocol());
        System.out.println("  远程主机: " + request.getRemoteHost() + ":" + request.getRemotePort());
        System.out.println("  服务器名: " + request.getServerName() + ":" + request.getServerPort());
    }
    
    /**
     * 请求销毁时调用
     */
    public void requestDestroyed(ServletRequestEvent sre) {
        HttpServletRequest request = (HttpServletRequest) sre.getServletRequest();
        Integer requestId = (Integer) request.getAttribute("requestId");
        
        System.out.println("[testServlet - SimpleRequestListener] 请求销毁");
        System.out.println("  请求ID: #" + (requestId != null ? requestId : "未知"));
        System.out.println("  URI: " + request.getRequestURI());
        System.out.println("  总请求数: " + requestCounter.get());
    }
}