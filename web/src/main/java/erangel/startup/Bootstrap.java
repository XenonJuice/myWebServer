package erangel.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import static erangel.base.Const.confInfo.*;
import static erangel.base.Const.webApp.CLASSES_ONLY;
import static java.io.File.separator;

public class Bootstrap {
    //<editor-fold desc = "attr">]
    // logger
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    //</editor-fold>
    //<editor-fold desc = "MAIN">
    public static void main(String[] args) {
        // 用于加载服务器和webapp共享的类的loder，例如加载javax servlet
        ClassLoader commonLoader = null;
        // 只加载服务器的loader
        ClassLoader coreLoader = null;
        // 配置部署目录
        configureDeploymentDir();
        // 创建类加载器
        ClassLoader[] commonAndCore = allocateClassLoader();
        commonLoader = commonAndCore[0];
        coreLoader = commonAndCore[1];

    }


    //</editor-fold>
    //<editor-fold desc = "获取核心目录和部署目录">

    /**
     * 配置webAPP执行的部署目录的位置。
     * <p>
     * 如果系统属性尚未设置部署目录，
     * 此方法将使用 {@code getCoreDir} 方法返回的值初始化它。
     * 这确保在进一步操作之前，部署目录被正确建立。
     */
    private static void configureDeploymentDir() {
        if (System.getProperty(DEPLOY_DIR) == null)
            System.setProperty(DEPLOY_DIR, getCoreDir());
    }

    /**
     * 取得服务器核心包的位置
     */
    private static String getCoreDir() {
        return System.getProperty(CORE_DIR, System.getProperty(USER_DIR));
    }

    //</editor-fold>
    //<editor-fold desc = "分配类加载器">
    private static ClassLoader[] allocateClassLoader() {
        ClassLoader[] ret = new ClassLoader[2];
        try {
            // 指向共有资源的File对象
            File[] commonRes = new File[2];
            File common = new File(getCoreDir(), COMMON + separator + LIB_LOWERCASE_ONLY_);
            File unpackedCommon = new File(getCoreDir(), COMMON + separator + CLASSES_ONLY);// 方便测试
            commonRes[0] = common;
            commonRes[1] = unpackedCommon;
            // 创建类加载器 TODO
            // ClassLoaderFactory.createClassLoader();
            // 指向服务器核心类的File对象
            File[] coreRes = new File[2];
            File core = new File(getCoreDir(), CORE + separator + LIB_LOWERCASE_ONLY_);
            File unpackedCore = new File(getCoreDir(), CORE + separator + CLASSES_ONLY);// 方便测试
            coreRes[0] = core;
            coreRes[1] = unpackedCore;
            // 创建类加载器 TODO

        } catch (Exception e) {
            logger.error("allocateClassLoader error", e);
            System.exit(1);
        }
        return ret;
    }

    //</editor-fold>
    //<editor-fold desc = "">
    //</editor-fold>
    //<editor-fold desc = "">
    //</editor-fold>
    //<editor-fold desc = "内部类">
    public static class ClassLoaderFactory {
        private static boolean isValidDirectory(File dir) {
            return dir != null && dir.exists() && dir.isDirectory() && dir.canRead();
        }

        public static ClassLoader createClassLoader(File[] packed, File[] unpacked, ClassLoader parent) {
            ArrayList<String> classPath = new ArrayList<>();

            // 构建测试用的 未打包的class的class path
            if (unpacked != null) {
                for (File dir : unpacked) {
                    if (!isValidDirectory(dir)) continue;
                    logger.info("unpacked class path : {}", dir.getAbsolutePath());
                    URL url = null;
                    try {
                        url = dir.getCanonicalFile().toURI().toURL();
                    } catch (Exception e) {
                        logger.error("createClassLoader error", e);
                    }
                }

            }
            return null;
        }
    }
    //</editor-fold>
}
