package erangel.webResource;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 封装web资源，变成流或者二进制的形式
 * @author LILINJIAN
 * @version 2025/2/26
 */
@Deprecated
@SuppressWarnings("unused")
public class Resource {
    //<editor-fold desc = "构造器">
    public Resource() {
    }
    public Resource(InputStream inputStream) {
        setContent(inputStream);
    }
    //</editor-fold>
    //<editor-fold desc = "attr">
    protected InputStream inputStream = null;
    protected byte[] binaryContent = null;
    //</editor-fold>
    //<editor-fold desc = "其他方法">
    //</editor-fold>
    //<editor-fold desc = "getter&setter">
    public InputStream getStreamContent() throws IOException {
        if (binaryContent != null) return new ByteArrayInputStream(binaryContent);
        return inputStream;
    }
    public byte[] getBinaryContent() {
        return binaryContent;
    }
    public void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    public void setContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
