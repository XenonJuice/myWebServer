package livonia.startup;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import static java.io.File.separator;
import static livonia.base.Const.confInfo.*;
import static livonia.base.Const.webApp.CLASSES_ONLY;
import static livonia.base.Const.webApp.DOTJAR;

// JVM 的启动入口，负责构建启动环境
public class Bootstrap {
    //<editor-fold desc = "attr">
    // 服务器真正的启动类
    private static final String LIVONIA = "livonia.startup.Livonia";
    private static final String PROCESS = "process";

    //</editor-fold>
    //<editor-fold desc = "MAIN">
    public static void main(String[] args) {
        // 检查是否是停止命令
        boolean isStopCommand = false;
        for (String arg : args) {
            if ("-stop".equals(arg)) {
                isStopCommand = true;
                break;
            }
        }

        // 只在启动时显示banner，停止时不显示
        if (!isStopCommand) {
            // 可以通过系统属性控制是否显示动画效果
            boolean showAnimation = Boolean.parseBoolean(System.getProperty("livonia.banner.animation", "true"));

            if (showAnimation) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 打印启动 Banner
            printBanner();

            if (showAnimation) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // 用于加载服务器和webapp共享的类的loder，例如加载javax servlet
        ClassLoader commonLoader = null;
        // 只加载服务器的loader
        ClassLoader coreLoader;
        // 配置运行实例目录
        getInstanceDir(isStopCommand);
        // 创建类加载器
        ClassLoader[] commonAndCore = allocateClassLoader(isStopCommand);
        if (commonAndCore == null) throw new RuntimeException("allocate class loader error");
        commonLoader = commonAndCore[0];
        coreLoader = commonAndCore[1];

        // 将当前上下文的类加载器切换到已经做好的核心类加载器
        Thread.currentThread().setContextClassLoader(coreLoader);
        try {
            // 通过类加载器加载启动类
            Class<?> livonia = coreLoader.loadClass(LIVONIA);
            Object livoInstance = livonia.getDeclaredConstructor().newInstance();
            if (!isStopCommand) {
                System.out.println("=== Bootstrap : livonia core class loaded ===");
            }

            // 存储启动时的命令的类型
            Class<?>[] paramTypes = new Class[1];
            // 存储启动时命令的具体值
            Object[] paramValues = new Object[1];
            // 要调用的方法
            Method method;
            System.out.println("=== Bootstrap : set Linovia's parent class loader ===");
            // 调用Livonia的设置父类加载器的方法
            String methodName = "setParentClassLoader";
            // 将livonia的父类加载器设置为commonLoader
            paramTypes[0] = ClassLoader.class;
            paramValues[0] = commonLoader;
            method = livoInstance.getClass().getMethod(methodName, paramTypes);
            method.invoke(livoInstance, paramValues);
            System.out.println("=== Bootstrap : set Linovia's parent class loader success ===");
            System.out.println("=== Bootstrap : invoke Linovia's process method ===");
            paramTypes[0] = args.getClass();
            paramValues[0] = args;
            method = livoInstance.getClass().getMethod(PROCESS, paramTypes);
            method.invoke(livoInstance, paramValues);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(2);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
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
    private static void getInstanceDir(boolean isStopCommand) {
        if (System.getProperty(DEPLOY_DIR) == null)
            System.setProperty(DEPLOY_DIR, getCoreDir(isStopCommand));
    }

    /**
     * 取得服务器核心包的位置
     */
    private static String getCoreDir(boolean isStopCommand) {
        String coreDir = System.getProperty(CORE_DIR);
        if (coreDir == null) {
            // 查找当前目录下的 server 目录
            File currentDir = new File(System.getProperty(USER_DIR));
            File serverDir = new File(currentDir, "server");
            if (!serverDir.exists() || !serverDir.isDirectory()) {
                // 如果直接子目录没有，查找下一级
                File[] subdirs = currentDir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        serverDir = new File(subdir, "server");
                        if (serverDir.exists() && serverDir.isDirectory()) {
                            break;
                        }
                    }
                }
            }
            coreDir = serverDir.exists() ? serverDir.getAbsolutePath() : System.getProperty(USER_DIR);
            // 只在非停止命令时显示
            if (!isStopCommand) {
                System.out.println("=== Bootstrap : CORE_DIR set to: " + coreDir + " ===");
            }
        }
        return coreDir;
    }

    //</editor-fold>
    //<editor-fold desc = "分配类加载器">
    private static ClassLoader[] allocateClassLoader(boolean isStopCommand) {
        if (!isStopCommand) {
            System.out.println("=== Bootstrap : try to allocate class loader... ===");
        }
        ClassLoader[] ret = new ClassLoader[2];
        try {
            File[] unpacked = new File[1];
            File[] packed = new File[1];
            // 指向servlet包的File对象
            File common = new File(getCoreDir(isStopCommand), COMMON + separator + LIB_LOWERCASE_ONLY_);
            File unpackedCommon = new File(getCoreDir(isStopCommand), COMMON + separator + CLASSES_ONLY);// 方便测试
            unpacked[0] = unpackedCommon;
            packed[0] = common;
            // 创建类加载器
            ClassLoader commonLoader = ClassLoaderFactory.createClassLoader(packed, unpacked, null, isStopCommand);
            // 指向服务器核心类的File对象
            File core = new File(getCoreDir(isStopCommand), CORE + separator + LIB_LOWERCASE_ONLY_);
            File unpackedCore = new File(getCoreDir(isStopCommand), CORE + separator + CLASSES_ONLY);// 方便测试
            unpacked[0] = unpackedCore;
            packed[0] = core;
            // 创建类加载器
            ClassLoader coreLoader = ClassLoaderFactory.createClassLoader(packed, unpacked, commonLoader, isStopCommand);
            ret[0] = commonLoader;
            ret[1] = coreLoader;
            if (!isStopCommand) {
                System.out.println("=== Bootstrap : allocateClassLoader success ===");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("=== Bootstrap : allocateClassLoader error ===");
            System.exit(1);
        }
        if (ret[1] != null && ret[0] != null) return ret;
        else return null;
    }

    //</editor-fold>
    //<editor-fold desc = "">
    //</editor-fold>
    //<editor-fold desc = "内部类">
    public static class ClassLoaderFactory {
        private static boolean isValidDirectory(File dir) {
            return dir != null && dir.exists() && dir.isDirectory() && dir.canRead();
        }

        public static ClassLoader createClassLoader(File[] packed, File[] unpacked, ClassLoader parent, boolean isStopCommand) {
            // 用于收集要被loader加载的路径
            Set<URL> classPathList = new HashSet<>();

            // 构建测试用的 未打包的class的class path
            if (unpacked != null) {
                // 将每一个未打包class作为File对象
                for (File dir : unpacked) {
                    // 验证正常性
                    if (isValidDirectory(dir)) continue;
                    if (!isStopCommand) {
                        System.out.println("=== Bootstrap : try to collect classes... === ");
                        System.out.println("unpacked class path : " + dir.getAbsolutePath());
                    }
                    URL url;
                    try {
                        url = dir.getCanonicalFile().toURI().toURL();
                        classPathList.add(url);
                    } catch (Exception e) {
                        System.out.println("createClassLoader error");
                        e.printStackTrace();
                    }
                }

            }

            // 构建一般的压缩的class 的class path
            if (packed != null) {
                for (File dir : packed) {
                    if (!isValidDirectory(dir)) continue;
                    if (!isStopCommand) {
                        System.out.println("packed class path : " + dir.getAbsolutePath());
                    }
                    String[] fileNames = dir.list();
                    if (fileNames == null) continue;
                    for (String fileName : fileNames) {
                        if (fileName.endsWith(DOTJAR)) {
                            File jarFile = new File(dir, fileName);
                            if (!isStopCommand) {
                                System.out.println("packed jar file : " + jarFile.getAbsolutePath());
                            }
                            URL url;
                            try {
                                url = jarFile.getCanonicalFile().toURI().toURL();
                                classPathList.add(url);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("createClassLoader error");
                            }
                        }
                    }
                }
            }
            if (classPathList.isEmpty()) return null;
            URL[] classPathArray = classPathList.toArray(new URL[0]);
            if (parent == null) return new URLClassLoader(classPathArray);
            else return new URLClassLoader(classPathArray, parent);
        }
    }
    //</editor-fold>

    /**
     * 打印启动 Banner
     */
    private static void printBanner() {
        String[] bannerParts = getBannerParts();
        String largeBanner = bannerParts[0];
        String smallText = ":: Livonia Web Server ::" + bannerParts[1];
        // 全部使用绿色
        System.out.print("\033[32m" + largeBanner);
        System.out.println(smallText);

        // 输出系统信息
        System.out.println("\n☕ Java Version: " + System.getProperty("java.version"));
        System.out.println("   Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("   OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("   Available Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("   Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        System.out.println("=====================================\n\033[0m");
    }

    private static String[] getBannerParts() {
        String banner = """
                
                ╔══════════════════════════════════════════════════════╗
                ║ ██╗     ██╗██╗   ██╗ ██████╗ ███╗   ██╗ ██╗ █████╗   ║
                ║ ██║     ██║██║   ██║██╔═══██╗████╗  ██║ ██║██╔══██╗  ║
                ║ ██║     ██║██║   ██║██║   ██║██╔██╗ ██║ ██║███████║  ║
                ║ ██║     ██║╚██╗ ██╔╝██║   ██║██║╚██╗██║ ██║██╔══██║  ║
                ║ ███████╗██║ ╚████╔╝ ╚██████╔╝██║ ╚████║ ██║██║  ██║  ║
                ║ ╚══════╝╚═╝  ╚═══╝   ╚═════╝ ╚═╝  ╚═══╝ ╚═╝╚═╝  ╚═╝  ║
                ╚══════════════════════════════════════════════════════╝
                
                :: Livonia Web Server ::        (v1.0)
                :: Java Servlet Container ::
                """;
        return banner.split(":: Livonia Web Server ::");
    }
}