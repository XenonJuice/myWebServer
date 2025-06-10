package livonia.base;

import java.io.IOException;
import java.net.URL;

public interface Host extends Vas {
    /**
     * 将指定的资源 URI 映射到相应的 Web 应用程序上下文。
     *
     * @param uri 要映射的资源 URI。它应该表示要与上下文关联的 Web 应用程序资源的位置或路径。
     * @return 与指定 URI 关联的 {@code Context}。如果不存在映射，此方法返回 {@code null}
     */
    Context map(String uri);

    /**
     * 检索此主机的应用程序的目录
     *
     * @return 应用程序目录路径的字符串。
     */
    String getAppBase();

    /**
     * 设置此主机的应用程序基础目录。
     *
     * @param appBase 要设置的应用程序目录。
     */
    void setAppBase(String appBase);

    interface Deployer {

        /**
         * 根据指定路径安装Web应用程序
         *
         * @param contextPath 要安装Web应用程序的上下文路径。
         *                    这应该是一个有效的非空字符串，代表部署的路径。
         *                    并且以斜杠开头。
         * @throws Exception 如果在安装过程中发生错误，例如
         *                   无效的上下文路径或部署程序失败。
         */
        void install(String contextPath, URL webApp) throws Exception;

        /**
         * 查找并检索与指定上下文路径关联的已部署Web应用程序。
         *
         * @param contextPath 要检索的已部署Web应用程序的上下文路径。
         *                    这应该是一个有效的、非空的字符串，以斜杠开头。
         * @return 代表与指定上下文路径关联的已部署应用程序的 {@code Context} 对象，
         * 如果未找到应用程序则返回 {@code null}。
         */
        Context findDeployedApp(String contextPath);

        /**
         * 检索所有已部署的WEB程序的列表。
         *
         * @return 一个数组，列出已部署的WEB应用程序。
         */
        String[] findDeployedApps();

        /**
         * 删除与指定上下文路径关联的Web应用程序。
         *
         * @param contextPath 要删除的Web应用程序的上下文路径。
         *                    它应该是一个有效的、非空的字符串，以正斜杠开头。
         * @throws IOException 如果在删除过程中发生I/O错误。
         */
        void remove(String contextPath) throws IOException;

        /**
         * 启动与指定上下文路径关联的web应用程序。
         *
         * @param contextPath 要启动的web应用程序的上下文路径。
         *                    这必须是一个有效的、非空的字符串，且以正斜杠开头。
         * @throws IOException 如果在启动操作过程中发生I/O错误。
         */
        void start(String contextPath) throws IOException;

        /**
         * 停止与指定上下文路径关联的Web应用程序。
         *
         * @param contextPath 要停止的Web应用程序的上下文路径。
         *                    应该是一个有效的、非空的字符串，以正斜杠开头。
         * @throws IOException 如果在停止过程中发生I/O错误。
         */
        void stop(String contextPath) throws IOException;
    }
}
