package erangel.webResource;


import java.io.InputStream;

/**
 * 封装web资源，变成流的形式
 * @author LILINJIAN
 * @version 2025/2/26
 */
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
    //</editor-fold>
    //<editor-fold desc = "其他方法">
    public InputStream getContent() {
        return inputStream;
    }
    public void setContent(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
