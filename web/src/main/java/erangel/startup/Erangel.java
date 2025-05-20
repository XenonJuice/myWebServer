package erangel.startup;

import java.io.File;

import static erangel.base.Const.confInfo.*;

public class Erangel {
    //<editor-fold desc = "配置二进制目录和实例运行目录">

    private static final String HELP_MESSAGE =
            """
                    Usage: Erangel
                      -c,--config     Server.XML file path
                      -h,--help       Show this help message
                      -d,--debug      Debug mode
                      -v,--version    Show version information
                      start           Start the server
                      stop            Stop the server""";
    //</editor-fold>
    //<editor-fold desc = "attr">
    private boolean debugMode = false;
    private String serverXMLPath = null;

    /**
     * 通过设置 {@code CORE_DIR} 系统属性来配置应用程序的核心目录。
     * <p>
     * 如果 {@code CORE_DIR} 系统属性尚未设置，此方法会将其初始化为 {@code USER_DIR} 系统属性的值，该值通常指向用户的工作目录。
     */
    private void setCoreDir() {
        if (System.getProperty(CORE_DIR) == null) {
            System.setProperty(CORE_DIR, System.getProperty(USER_DIR));
        }
    }

    /**
     * 设置实例部署目录的系统属性。
     * <p>
     * 如果标识为 {@code DEPLOY_DIR} 的系统属性尚未设置，
     * 此方法将其初始化为 {@code USER_DIR} 系统属性的值，
     * 该属性通常表示用户当前的工作目录。
     */
    private void setInstanceDir() {
        if (System.getProperty(DEPLOY_DIR) == null) {
            System.setProperty(DEPLOY_DIR, System.getProperty(USER_DIR));
        }
    }

    //</editor-fold>
    //<editor-fold desc = "运行">
    public void process(String[] args) {
        // 设置服务器核心目录
        setCoreDir();
        // 设置实例部署目录
        setInstanceDir();
    }

    //</editor-fold>
    //<editor-fold desc = "读取输入的命令以设置flag">
    private boolean readCommand(String[] args) {
        if (args == null || args.length == 0) help();
        return false;
    }

    //</editor-fold>
    //<editor-fold desc = "说明信息>
    private void help() {
        System.out.println(HELP_MESSAGE);
    }
    //</editor-fold>

    //<editor-fold desc = "服务器配置文件">
    private File serverXML() {
        File file = new File(serverXMLPath);
        if (!file.exists() || !file.isFile() || !file.canRead()
                || !file.isAbsolute()) {
            file = new File(System.getProperty(DEPLOY_DIR), serverXMLPath);
        }
        return file;
    }
    //</editor-fold>
}

