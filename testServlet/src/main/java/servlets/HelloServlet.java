package servlets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * HelloServlet
 */
public class HelloServlet extends HttpServlet {

    private String message;

    /**
     * 初始化方法
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // 从 web.xml 中读取初始化参数
        message = config.getInitParameter("message");
        if (message == null) {
            message = "Hello from Servlet";
        }
        System.out.println("servlets.HelloServlet initialized with message: " + message);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 使用 ByteArrayOutputStream 来精确计算字节
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter tempWriter = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        tempWriter.println("<html>");
        tempWriter.println("<head>");
        tempWriter.println("<title>Hello Servlet</title>");
        tempWriter.println("</head>");
        tempWriter.println("<body>");
        tempWriter.println("<h1>" + message + "</h1>");
        tempWriter.println("<p>Servlet version: " +
                getServletContext().getMajorVersion() + "." +
                getServletContext().getMinorVersion() + "</p>");
        tempWriter.println("<p>Server info: " + getServletContext().getServerInfo() + "</p>");
        tempWriter.println("<p>Request URI: " + request.getRequestURI() + "</p>");
        tempWriter.println("</body>");
        tempWriter.println("</html>");
        tempWriter.flush();

        byte[] content = baos.toByteArray();

        // 设置响应头
        response.setContentType("text/html; charset=UTF-8");
        // 写入内容
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }

    /**
     * 处理 POST 请求
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * 清理资源
     */
    public void destroy() {
        System.out.println("servlets.HelloServlet destroyed");
    }
}