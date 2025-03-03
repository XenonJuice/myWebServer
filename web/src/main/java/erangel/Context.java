package erangel;

import javax.servlet.ServletContext;

/**
 * 代表一个Web程序上下文
 *
 * @author LILINJIAN
 * @version 2025/02/20
 */
public interface Context extends Vas {
    /**
     * 取得是否可重载标志位
     */
    boolean getReloadable();

    /**
     * 返回servlet上下文。
     */
    ServletContext getServletContext();

    /**
     * 重载
     */
    void reload();
}
