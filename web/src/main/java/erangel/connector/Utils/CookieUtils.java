package erangel.connector.Utils;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CookieUtils {
    /**
     * 将Cookie映射转换为List<Cookie>
     */
    public static ArrayList<Cookie> convertToCookieList(Map<String, String> cookiesMap) {
        ArrayList<Cookie> cookieList = new ArrayList<>();
        for (Map.Entry<String, String> entry : cookiesMap.entrySet()) {
            Cookie cookie = new Cookie(entry.getKey(), entry.getValue());
            cookieList.add(cookie);
        }
        return cookieList;
    }

    /**
     * 将List<Cookie>转换为Cookie[]
     */
    public static Cookie[] convertToCookieArray(List<String> cookieHeaders) {
        List<Cookie> cookies = new ArrayList<>();
        for (String header : cookieHeaders) {
            String[] pairs = header.split("; ");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    cookies.add(new Cookie(keyValue[0].trim(), keyValue[1].trim()));
                }
            }
        }
        return cookies.toArray(new Cookie[0]);
    }

}
