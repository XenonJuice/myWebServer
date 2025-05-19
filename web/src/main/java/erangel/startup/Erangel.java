package erangel.startup;

import static erangel.base.Const.confInfo.*;

public class Erangel {
    //<editor-fold desc = "配置二进制目录和实例运行目录">

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
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
    //<editor-fold desc = "XXXXXXX">
    //</editor-fold>
}
