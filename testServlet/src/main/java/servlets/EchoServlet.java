package servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * Echo Servlet - 回显请求信息
 */
public class EchoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        out.println("=== Request Information ===");
        out.println("Method: " + request.getMethod());
        out.println("URI: " + request.getRequestURI());
        out.println("Protocol: " + request.getProtocol());
        out.println("Remote Address: " + request.getRemoteAddr());
        out.println();

        out.println("=== Headers ===");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            out.println(name + ": " + value);
        }
        out.println();

        out.println("=== Parameters ===");
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String[] values = request.getParameterValues(name);
            for (String value : values) {
                out.println(name + " = " + value);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        // 先输出基本信息
        out.println("=== Request Information ===");
        out.println("Method: " + request.getMethod());
        out.println("Content-Type: " + request.getContentType());
        out.println("Content-Length: " + request.getContentLength());
        out.println();

        // 处理不同类型的内容
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
            // 表单数据 - 使用 getParameter
            out.println("=== Form Parameters ===");
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String name = paramNames.nextElement();
                String[] values = request.getParameterValues(name);
                for (String value : values) {
                    out.println(name + " = " + value);
                }
            }
        } else {
            // 其他类型 - 读取原始数据
            out.println("=== POST Body ===");
            int contentLength = request.getContentLength();
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                BufferedReader reader = request.getReader();
                int totalRead = 0;
                while (totalRead < contentLength) {
                    int read = reader.read(buffer, totalRead, contentLength - totalRead);
                    if (read == -1) break;
                    totalRead += read;
                }
                out.println(new String(buffer, 0, totalRead));
            }
        }
    }
}