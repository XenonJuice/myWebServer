import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DynamicServlet extends HttpServlet {
    
    private String deployTime;
    
    public void init() throws ServletException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        deployTime = sdf.format(new Date());
        System.out.println("[DynamicApp] Servlet初始化 - 部署时间: " + deployTime);
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("[DynamicApp] 处理请求: " + request.getRequestURI());
        
        out.println("<html>");
        out.println("<head><title>Dynamic App</title></head>");
        out.println("<body style='background-color: #e6ffe6;'>");
        out.println("<h1 style='color: purple;'>动态部署成功！</h1>");
        out.println("<p>这个应用是在服务器运行过程中动态添加的。</p>");
        out.println("<p>部署时间: " + deployTime + "</p>");
        out.println("<p>当前时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "</p>");
        out.println("<p>请求路径: " + request.getRequestURI() + "</p>");
        out.println("<hr>");
        out.println("<p>如果你能看到这个页面，说明服务器成功检测并部署了新的webapp！</p>");
        out.println("</body>");
        out.println("</html>");
    }
    
    public void destroy() {
        System.out.println("[DynamicApp] Servlet销毁");
    }
}