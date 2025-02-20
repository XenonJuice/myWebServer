package erangel;

public interface Loader {

    /**
     * 获取与此加载器关联的 ClassLoader。
     *
     * @return 与此容器关联的 ClassLoader 实例，
     * 或者如果没有可用的 ClassLoader 则返回 null。
     */
    ClassLoader getClassLoader();

    Vas getVas();

    void setVas(Vas vas);

    /**
     * 检索与此加载器相关联的上下文。
     *
     * @return 与此加载器关联的上下文实例，如果没有设置上下文则返回null。
     */
    Context getContext();

    /**
     * 设置与此加载器实例关联的上下文。
     *
     * @param context 要与此加载器关联的上下文对象。
     */
    void setContext(Context context);

    /**
     * 获取加载器的委托状态。
     *
     * @return 如果该加载器是标准委托模式，则返回true，否则返回false。
     */
    boolean getDelegate();

    /**
     * 设置委托模式标志位
     *
     * @param delegate 要设置为该加载器的委托的对象
     */
    void setDelegate(boolean delegate);

    /**
     * 返回此加载器的可重新加载标志。
     */
    public boolean getReloadable();


    /**
     * 设置此加载器的可重新加载标志。
     *
     * @param reloadable 新的可重新加载标志
     */
    void setReloadable(boolean reloadable);

    /**
     * 将新仓库添加到此类加载器的仓库集合中。
     *
     * @param repository 要添加的仓库
     */
    void addRepository(String repository);


    /**
     * 返回为此类加载器定义的仓库集合。
     * 如果没有定义，则返回一个零长度的数组。
     */
    String[] findRepositories();


    /**
     * 与此加载器关联的内部仓库是否已被修改，
     * 以至于加载的类需要重新加载？
     */
    boolean modified();
}
