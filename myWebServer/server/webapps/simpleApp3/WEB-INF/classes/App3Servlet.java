import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class App3Servlet extends HttpServlet {
    
    private long startTime;
    
    public void init() throws ServletException {
        startTime = System.currentTimeMillis();
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        
        if ("/api".equals(request.getServletPath())) {
            // Return JSON response
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            
            long uptime = System.currentTimeMillis() - startTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            String json = "{\n" +
                "  \"appName\": \"Simple App 3\",\n" +
                "  \"status\": \"running\",\n" +
                "  \"serverTime\": \"" + sdf.format(new Date()) + "\",\n" +
                "  \"uptime\": " + uptime + ",\n" +
                "  \"uptimeReadable\": \"" + (uptime / 1000) + " seconds\",\n" +
                "  \"requestInfo\": {\n" +
                "    \"method\": \"" + request.getMethod() + "\",\n" +
                "    \"uri\": \"" + request.getRequestURI() + "\",\n" +
                "    \"remoteAddr\": \"" + request.getRemoteAddr() + "\"\n" +
                "  }\n" +
                "}";
            
            out.print(json);
            out.flush();
        } else {
            // Return HTML response
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            
            out.println("<html>");
            out.println("<head><title>Simple App 3</title></head>");
            out.println("<body style='background-color: #ffe6e6;'>");
            out.println("<h1 style='color: red;'>Simple App 3 - API Demo</h1>");
            out.println("<p>This webapp demonstrates different response types.</p>");
            out.println("<p>Try accessing <a href='/app3/api'>/app3/api</a> for JSON response!</p>");
            out.println("<hr>");
            out.println("<p>App has been running for: " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}