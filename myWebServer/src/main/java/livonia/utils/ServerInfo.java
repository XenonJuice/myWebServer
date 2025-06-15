package livonia.utils;

import java.io.InputStream;
import java.util.Properties;

public final class ServerInfo {
    private static String SERVER_INFO = null;

    static {
        try {
            InputStream in = ServerInfo.class
                    .getResourceAsStream("/livonia/utils/ServerInfo.properties");
            Properties props = new Properties();
            props.load(in);
            if (in != null) {
                in.close();
            }
            SERVER_INFO = props.getProperty("server.info");
        } catch (Exception _) {
        } finally {
            if (SERVER_INFO == null) SERVER_INFO = "Livonia/1.0";
        }
    }

    public static String getServerInfo() {
        return SERVER_INFO;
    }
}
