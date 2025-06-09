package servlets;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 处理登录的 Servlet
 */
public class LoginServlet extends HttpServlet {

    /**
     * 显示登录表单
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 先构建完整的 HTML 内容
        StringBuilder html = new StringBuilder();
        html.append("<html>\n");
        html.append("<head>李林健</head>\n");
        html.append("<head><title>Login</title></head>\n");
        html.append("<body>\n");
        html.append("<h1>Login</h1>\n");

        // 检查是否有错误消息
        String error = request.getParameter("error");
        if (error != null) {
            html.append("<p style='color:red'>Invalid username or password!</p>\n");
        }

        html.append("<form method='post' action='login'>\n");
        html.append("Username: <input type='text' name='username'><br>\n");
        html.append("Password: <input type='password' name='password'><br>\n");
        html.append("<input type='submit' value='Login'>\n");
        html.append("</form>\n");
        html.append("</body>\n");
        html.append("</html>");

        // 转换为字节数组以获得准确的长度
        byte[] content = html.toString().getBytes("UTF-8");

        // 设置响应头
        response.setContentType("text/html; charset=UTF-8");

        // 使用 OutputStream 写入
        ServletOutputStream out = response.getOutputStream();
        out.write(content);
        out.flush();
        // test
        out.close();
    }

    /**
     * 处理登录请求
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // 简单验证
        if ("admin".equals(username) && "admin".equals(password)) {
            // 重定向到欢迎页面
            response.sendRedirect("hello");
        } else {
            // 登录失败，重定向回登录页面
            response.sendRedirect("login?error=1");
        }
    }
}