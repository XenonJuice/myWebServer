package erangel.connector.http;

import erangel.log.BaseLogger;

import javax.servlet.ServletInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * HTTP解析器，用于解析HTTP请求。
 *
 * @author LILINJIAN
 * @version  $Date: 2024/12/19 15:24
 */
public class HttpProcessor extends BaseLogger {
    HttpRequest request;
    HttpResponse response;

    // 从请求中获得的 ServletInputStream
    public ServletInputStream servletInputStream;
    // 从请求中获得的字符编码
    public String characterEncoding;
    // 存储HTTP头的映射
    public Map<String, List<String>> headers = new HashMap<>();
    // 存储请求参数的映射
    public Map<String, List<String>> parameters = new HashMap<>();
    public String method;
    public String fullUri;
    public String protocol;
    public String uri;

    public HttpProcessor(HttpRequest request, HttpResponse response) throws IOException {
        this.request = request;
        this.response = response;
        this.servletInputStream = request.getInputStream();
        this.characterEncoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
        parseRequest();
        assembleRequest(request, method, uri, protocol, headers, parameters);
    }

    /**
     * 使用字节读取一行数据，直到遇到 \r\n 为止。
     * 如果到达流的末尾返回 null。
     */
    private String readLine(ServletInputStream in, String charset) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;
        boolean lastWasCR = false;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                lastWasCR = true;
            } else if (c == '\n' && lastWasCR) {
                break; // 读取到完整行
            } else {
                if (lastWasCR) {
                    // \r 后没有紧跟 \n，将 \r 也写入缓冲
                    buffer.write('\r');
                    lastWasCR = false;
                }
                buffer.write(c);
            }
        }

        // 如果没读到任何数据且已到流末尾，返回null表示无更多行可读
        if (c == -1 && buffer.size() == 0 && !lastWasCR) {
            return null;
        }

        return buffer.toString(charset);
    }

    /**
     * 解析HTTP请求
     */
    private void parseRequest() throws IOException {
        // 1. 读取并解析请求行
        String requestLine = readLine(servletInputStream, characterEncoding);
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request");
        }

        String[] requestLineParts = requestLine.split(" ", 3);
        if (requestLineParts.length != 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }
        method = requestLineParts[0];
        fullUri = requestLineParts[1];
        protocol = requestLineParts[2];

        logger.info("请求方法: {}", method);
        logger.info("完整URI: {}", fullUri);
        logger.info("协议版本: {}", protocol);

        // 解析URI和查询字符串
        if (fullUri.contains("?")) {
            String[] uriParts = fullUri.split("\\?", 2);
            uri = uriParts[0];
            parseParameters(uriParts[1]);
            logger.info("解析后的URI: {}", uri);
            logger.info("解析后的查询参数: {}", parameters);
        } else {
            uri = fullUri;
        }

        // 2. 解析请求头
        int contentLength = 0;
        String contentType = null;

        String headerLine;
        while ((headerLine = readLine(servletInputStream, characterEncoding)) != null && !headerLine.isEmpty()) {
            int separatorIndex = headerLine.indexOf(": ");
            if (separatorIndex == -1) {
                logger.info("格式错误的头部: {}", headerLine);
                continue;
            }
            String key = headerLine.substring(0, separatorIndex).trim();
            String value = headerLine.substring(separatorIndex + 2).trim();
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            logger.info("头部: {} = {}", key, value);

            if ("Content-Length".equalsIgnoreCase(key)) {
                try {
                    contentLength = Integer.parseInt(value);
                    logger.info("Content-Length: {}", contentLength);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid Content-Length value: " + value);
                }
            }

            if ("Content-Type".equalsIgnoreCase(key)) {
                contentType = value;
                String[] typeParts = value.split(";");
                if (typeParts.length > 1) {
                    String charsetPart = typeParts[1].trim();
                    if (charsetPart.startsWith("charset=")) {
                        String charset = charsetPart.substring("charset=".length());
                        response.setCharacterEncoding(charset);
                        this.characterEncoding = charset;
                    }
                }
            }
        }

        // 3. 根据Content-Length从流中按字节读取请求体
        if (contentLength > 0) {
            logger.info("开始读取请求体, 长度: {}", contentLength);
            byte[] body = new byte[contentLength];
            int bytesRead = 0;
            while (bytesRead < contentLength) {
                int readCount = servletInputStream.read(body, bytesRead, contentLength - bytesRead);
                if (readCount == -1) {
                    break;
                }
                bytesRead += readCount;
            }
            if (bytesRead != contentLength) {
                logger.warn("请求体不完整: 已读 {} 字节, 期望 {} 字节", bytesRead, contentLength);
                throw new IOException("Request body is incomplete");
            }
            logger.info("成功读取请求体");

            // 如果Content-Type是application/x-www-form-urlencoded，则对body进行解码并解析参数
            if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                String bodyStr = new String(body, characterEncoding);
                parseParameters(bodyStr);
                logger.info("解析后的请求体参数: {}", parameters);
            }
        }

        logger.info("HTTP请求解析完成");
    }

    /**
     * 解析查询字符串参数
     */
    private void parseParameters(String queryString) throws UnsupportedEncodingException {
        logger.info("解析参数: {}", queryString);
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], characterEncoding);
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], characterEncoding) : "";
            parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            logger.info("参数: {} = {}", key, value);
        }
    }

    /**
     * 组装请求对象
     */
    private void assembleRequest(HttpRequest request, String method, String uri, String protocol, Map<String, List<String>> headers, Map<String, List<String>> parameters) {
        request.setMethod(method);
        request.setUri(uri);
        request.setProtocol(protocol);
        request.setHeaders(headers);
        request.setParameters(parameters);
    }

    /**
     * 解析请求头
     */
    //TODO
    private void parseHeaders (Map<String, List<String>> headers) {
        if (headers.isEmpty()) return;
        if (headers.containsKey("")) {}
    }
}
