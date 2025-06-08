package livonia.startup;

import livonia.XMLParse.*;
import livonia.base.Server;
import livonia.base.Vas;
import livonia.lifecycle.Lifecycle;
import livonia.lifecycle.LifecycleException;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import static livonia.base.Const.commonCharacters.SOLIDUS;
import static livonia.base.Const.confInfo.*;
import static livonia.utils.ServerInfo.getServerInfo;

public class Livonia {
    //<editor-fold desc = "helpMessage">
    private static final String HELP_MESSAGE =
            """
                    Usage: Livonia
                      -c,--config     Server.XML file path
                      -h,--help       Show this help message
                      -d,--debug      Debug mode
                      -v,--version    Show version information
                      start           Start the server
                      stop            Stop the server""";
    private final ClassLoader parentClassLoader = null;
    //</editor-fold>
    //<editor-fold desc = "attr">
    private boolean debugMode = false;
    private String serverXMLPath = null;
    private boolean isStarting = false;
    private boolean isStopping = false;
    private Thread shutdownHook = null;
    public Server server = null;
    //</editor-fold>
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
    //<editor-fold desc = "运行">
    public void process(String[] args) {
        // 设置服务器核心目录
        setCoreDir();
        // 设置实例部署目录
        setInstanceDir();
        // 检测命令输入内容以判断是否启动服务
        try {
            if (readCommand(args)) work();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private void work() {
        if (isStarting) start();
        else if (isStopping) stop();
    }

    //</editor-fold>
    //<editor-fold desc = "读取输入的命令以设置flag">
    private boolean readCommand(String[] args) {
        boolean isServerXMLPath = false;
        if (args == null || args.length == 0) {
            help();
            return false;
        }
        for (String arg : args) {
            if (isServerXMLPath) {
                serverXMLPath = arg;
                isServerXMLPath = false;
            } else if (arg.equals("-c") || arg.equals("--config")) {
                isServerXMLPath = true;
            } else if (arg.equals("-h") || arg.equals("--help")) {
                help();
                return false;
            } else if (arg.equals("-d") || arg.equals("--debug")) {
                debugMode = true;
                System.setProperty("log.level", "DEBUG");
            } else if (arg.equals("-v") || arg.equals("--version")) {
                System.out.println(getServerInfo());
                return false;
            } else if (arg.equals("-start") ) {
                isStarting = true;
            } else if (arg.equals("-stop")) {
                isStopping = true;
            }
        }
        return true;
    }

    //</editor-fold>
    //<editor-fold desc = "说明信息">
    private void help() {
        System.out.println(HELP_MESSAGE);
    }

    //</editor-fold>]
    //<editor-fold desc = "服务器配置文件">
    private File serverXML() {
        File file = new File(serverXMLPath);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            file = new File(System.getProperty(DEPLOY_DIR), serverXMLPath);
        }
        return file;
    }

    //</editor-fold>
    //<editor-fold desc = "生命周期">
    private void start() {
        // 开始解析serverXML
        MiniDigester d = parseXMLStarting();
        // 获取XML引用
        File serverXML = serverXML();
        try (FileInputStream fileInputStream = new FileInputStream(serverXML)) {
            d.push(this);
            d.parse(fileInputStream);
        } catch (Exception e) {
            System.out.println("Exception occurred when starting  : " + e);
            e.printStackTrace();
            System.exit(1);
        }
        shutdownHook = new ShutdownHook();
        // 启动服务器
        try {
            server.initialize();
            ((Lifecycle) server).start();
            try {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                System.out.println("Exception occurred when Shutdown hook  added");
            }
            server.waitForShutdown();
        } catch (LifecycleException e) {
            System.out.println("Exception occurred when starting : " + e);
            e.printStackTrace();
        }
        // 意外情况下会通过关闭钩子关闭
        // ============================
        // 关闭服务器
        try {
            try {
                // 移除关闭钩子
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                System.out.println("Exception occurred when Shutdown hook  removed");
            }
            ((Lifecycle) server).stop();
        } catch (LifecycleException e) {
            System.out.println("Exception occurred when stopping : " + e);
            e.printStackTrace();
        }


    }

    // 正常的指令关闭
    private void stop() {
        MiniDigester d = parseXMLStopping();
        File serverXML = serverXML();
        try (FileInputStream fileInputStream = new FileInputStream(serverXML)) {
            d.push(this);
            d.parse(fileInputStream);
        } catch (Exception e) {
            System.out.println("Exception occurred when stopping  : " + e);
            e.printStackTrace();
            System.exit(1);
        }
        try {
            // 获取回环地址对象
            InetAddress shutDownHost = InetAddress.getLoopbackAddress();
            // 创建Socket连接到回环地址
            Socket closeSocket = new Socket(shutDownHost, server.getShutdownPort());
            OutputStream outputStream = closeSocket.getOutputStream();
            String shutdownCommand = server.getShutdownCommand();
            for (int i = 0; i < shutdownCommand.length(); i++) {
                outputStream.write(shutdownCommand.charAt(i));
            }
            outputStream.flush();
            outputStream.close();
            closeSocket.close();
        } catch (Exception e) {
            System.out.println("Exception occurred when stopping : " + e);
            e.printStackTrace();
        }
    }
    //</editor-fold>

    //<editor-fold desc = "XML解析">
    private MiniDigester parseXMLStarting() {
        MiniDigester d = new MiniDigester();
        d.setNamespaceAware(true);
        d.addRuleSet(new ServerRuleSet());
        d.addRuleSet(new EngineRuleSet(SERVER + SOLIDUS + SERVICE));
        d.addRuleSet(new HostRuleSet(SERVER + SOLIDUS + SERVICE + SOLIDUS + ENGINE));
        d.addRuleSet(new ContextRuleSet(SERVER + SOLIDUS + SERVICE + SOLIDUS + ENGINE + SOLIDUS + HOST));
        d.addRule(SERVER + SOLIDUS + SERVICE + SOLIDUS + ENGINE, new SetTopParentClassLoaderRule(d, parentClassLoader));
        return d;
    }

    private MiniDigester parseXMLStopping() {
        MiniDigester d = new MiniDigester();
        d.setNamespaceAware(true);
        d.addRuleSet(new ShutDownServerRuleSet());
        return d;
    }

    //</editor-fold>
    //<editor-fold desc = "栈顶类加载器设置">
    static final class SetTopParentClassLoaderRule implements Rule {
        private final MiniDigester digester;
        private final ClassLoader parentClassLoader;

        public SetTopParentClassLoaderRule(MiniDigester digester, ClassLoader parent) {
            this.parentClassLoader = parent;
            this.digester = digester;
        }

        @Override
        public void begin(String path, Attributes attrs, MiniDigester d) {
            Vas vas = digester.peek();
            vas.setParentClassLoader(parentClassLoader);
        }

    }

    //</editor-fold>

    //<editor-fold desc = "关闭钩子">
    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            if (server != null) {
                try {
                    ((Lifecycle) server).stop();
                } catch (LifecycleException e) {
                    System.out.println("Exception when stopping : " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    //</editor-fold>

    public void setServer(Server server) {
        System.out.println("Livonia.setServer called with: " + server);
        System.out.println("Server class: " + (server != null ? server.getClass().getName() : "null"));

        this.server = server;
    }
}

