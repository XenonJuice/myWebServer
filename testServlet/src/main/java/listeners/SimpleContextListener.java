package listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 简单的上下文监听器 - 测试用
 */
public class SimpleContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[SimpleContextListener] App started!");
        ServletContext ctx = sce.getServletContext();
        long currentTimeMillis = System.currentTimeMillis();
        ctx.setAttribute("startTime"
                , currentTimeMillis);
    }

    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[SimpleContextListener] App stopped!");
    }
}