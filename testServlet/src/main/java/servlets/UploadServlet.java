package servlets;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 处理上传的Servlet - 测试InputStream和chunk传输
 */
public class UploadServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("[UploadServlet] Processing upload...");

        // 读取输入流
        ServletInputStream in = request.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        int totalBytes = 0;

        while ((bytesRead = in.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
            totalBytes += bytesRead;
            System.out.println("[UploadServlet] Read chunk: " + bytesRead + " bytes");
        }

        // 获取接收到的内容
        String content = baos.toString(StandardCharsets.UTF_8);

        System.out.println("[UploadServlet] Total bytes received: " + totalBytes);
        System.out.println("[UploadServlet] Content received:");
        System.out.println("-------- START OF CONTENT --------");
        System.out.println(content);
        System.out.println("-------- END OF CONTENT --------");

        // 回传响应 - 返回HTML页面
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>Upload Result</title></head>");
        out.println("<body>");
        out.println("<h2>Upload successful!</h2>");
        out.println("<p>Bytes received: " + totalBytes + "</p>");
        out.println("<h3>Content echo:</h3>");
        out.println("<pre>" + content + "</pre>");
        out.println("<br><a href='upload'>Upload another file</a>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<form method='post' action='upload'>");
        out.println("<textarea name='data' rows='10' cols='50'></textarea><br>");
        out.println("<input type='submit' value='Upload'>");
        out.println("</form>");
        out.close();
    }
}