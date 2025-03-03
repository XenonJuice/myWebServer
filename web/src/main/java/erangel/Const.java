package erangel;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static erangel.Const.PunctuationMarks.CRLF;

public final class Const {
    /**
     * 常用字符
     */
    public static class commonCharacters {
        public static final String EMPTY = "";
        public static final String BLANK = " ";
        public static final String TAB = "\t";
        public static final String DOT = ".";
        public static final String SOLIDUS = "/";
    }

    /**
     * webApp关联
     */
    public static class webApp {
        public static final String CLASSES = "/WEB-INF/classes";
        public static final String LIB = "/WEB-INF/lib";
        public static final String WEB_XML = "/WEB-INF/web.xml";
        public static final String JAR = ".jar";
        public static final String WAR = ".war";
        public static final SimpleDateFormat[] DATE_FORMATS = {
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
                new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US),
                new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
                new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
        };
    }

    /**
     * 描述信息
     */
    public static class Package {
        public static final String Package = "llj.erangel.connector.http";
    }

    /**
     * 解析器常见属性
     */
    public static class Processor {
        public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
        public static final int PROCESSOR_IDLE = 0;
        public static final int PROCESSOR_ACTIVE = 1;
    }

    /**
     * 常见的ContentType
     */
    public static class ContentType {
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_HTML = "text/html";
        public static final String TEXT_XML = "text/xml";
        public static final String TEXT_JSON = "text/json";
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String APPLICATION_X_WWW_FORM_MULTIPART = "multipart/form-data";
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_XML = "application/xml";
    }

    /**
     * 连接器使用的常量
     */
    public static class ConnectorConstants {
        public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    }

    /**
     * HTTP协议版本
     */
    public static class HttpProtocol {
        public static final String HTTP_1_1 = "HTTP/1.1";
        public static final String HTTP_1_0 = "HTTP/1.0";
        public static final String HTTP_0_9 = "HTTP/0.9";
    }

    /**
     * 临时响应
     */
    public static class Ack {
        public static final String ACK = "HTTP/1.1 100 Continue" + CRLF + CRLF;
        public static final String EXCEPT_ACKNOWLEDGEMENT = "100-continue";
    }

    public static class PunctuationMarks {
        public static final String CRLF = "\r\n";
        public static final String CR = "\r";
        public static final String LF = "\n";
        public static final String EMPTY = "";
        public static final String SPACE = " ";
        public static final String COLON = ":";
        public static final String SEMICOLON = ";";
        public static final String COMMA = ",";
        public static final String COLON_SPACE = ": ";
        public static final String LEFT_BRACKET = "[";
        public static final String RIGHT_BRACKET = "]";
    }

    public static class CharPunctuationMarks {
        public static final char CR = '\r';
        public static final char LF = '\n';
        public static final char SPACE = ' ';
        public static final char COLON = ':';
        public static final char SEMICOLON = ';';
        public static final char COMMA = ',';
    }

    public static class Header {
        public static final String CLOSE = "close";
        public static final String SESSION_ID = "JSESSIONID";
        public static final String AUTHORIZATION = "Authorization";
        public static final String HOST = "Host";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String CONTENT_LENGTH = "Content-Length";
        public static final String ACCEPT_LANGUAGE = "Content-Language";
        public static final String CONNECTION = "Connection";
        public static final String KEEP_ALIVE = "Keep-Alive";
        public static final String SERVER = "Server";
        public static final String DATE = "Date";
        public static final String COOKIE = "Cookie";
        public static final String SET_COOKIE = "Set-Cookie";
        public static final String CONTENT_ENCODING = "Content-Encoding";
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        public static final String CHUNKED = "chunked";
        public static final String LOCATION = "Location";
    }
}
