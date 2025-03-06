package erangel.Resource;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 本地资源的抽象
 * @author LILINJIAN
 * @version 2025/2/26
 */

public interface LocalResource {
    boolean exists();

    byte[] getContent() throws IOException;

    String getName();

    long getLastModified() throws IOException;

    URL getURL() throws MalformedURLException;
}
