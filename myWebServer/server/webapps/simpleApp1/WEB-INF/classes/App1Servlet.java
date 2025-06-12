import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class App1Servlet extends HttpServlet {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html>");
        out.println("<head><title>Simple App 1</title></head>");
        out.println("<body>");
        out.println("<h1 style='color: blue;'>Hello from Simple App 1!</h1>");
        out.println("<p>This is a simple webapp for testing multiple apps.</p>");
        out.println("<p>Current time: " + new java.util.Date() + "</p>");
        out.println("</body>");
        out.println("</html>");
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}