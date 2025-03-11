package erangel.resource;


import java.io.InputStream;
import java.net.URL;

/**
 * 本地资源的抽象
 * @author LILINJIAN
 * @version 2025/2/26
 */

public interface LocalResource {
    boolean exists();

    boolean isDirectory();

    boolean canRead();

    byte[] getContent() ;

    String getName();

    long getLastModified();

    URL getURL() ;

    InputStream getInputStream();
}
