package erangel.base;

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

    }
}
