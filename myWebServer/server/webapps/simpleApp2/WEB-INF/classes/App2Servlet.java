import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class App2Servlet extends HttpServlet {
    
    private int visitCount = 0;
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        synchronized(this) {
            visitCount++;
        }
        
        out.println("<html>");
        out.println("<head><title>Simple App 2</title></head>");
        out.println("<body style='background-color: #f0f0f0;'>");
        out.println("<h1 style='color: green;'>Welcome to Simple App 2!</h1>");
        out.println("<p>This is the second test webapp.</p>");
        out.println("<p>Visit count: " + visitCount + "</p>");
        out.println("<p>Request URI: " + request.getRequestURI() + "</p>");
        out.println("<p>Server time: " + new java.util.Date() + "</p>");
        out.println("</body>");
        out.println("</html>");
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}