package erangel.base;

/**
 * LifecycleException 表示一个与组件生命周期相关的通用异常，
 * 当组件在启动、停止或运行过程出现致命错误，通常会抛出此异常。
 * 出现本异常时，是不可恢复的严重问题。
 *
 * <p>示例用法：
 * <pre>
 *     try {
 *         someLifecycleComponent.start();
 *     } catch (LifecycleException e) {
 *         // 处理启动异常，例如记录日志或中止程序
 *     }
 * </pre>
 *
 * @author LILINJIAN
 * @version 2025/02/04
 */
public class LifecycleException extends Exception {

    /**
     * 保存异常信息的字段（如果存在）
     */
    protected String message;

    /**
     * 引发本异常的真实异常对象（如果存在）
     */
    protected Throwable throwable;

    /**
     * 无参构造方法，不携带任何信息。
     */
    public LifecycleException() {
        this(null, null);
    }

    /**
     * 携带异常信息的构造方法。
     *
     * @param message 异常的描述信息
     */
    public LifecycleException(String message) {
        this(message, null);
    }

    /**
     * 携带底层异常的构造方法。
     *
     * @param throwable 触发本异常的底层异常
     */
    public LifecycleException(Throwable throwable) {
        this(null, throwable);
    }

    /**
     * 同时携带异常信息和底层异常的构造方法。
     *
     * @param message   异常的描述信息
     * @param throwable 触发本异常的底层异常
     */
    public LifecycleException(String message, Throwable throwable) {
        super();
        this.message = message;
        this.throwable = throwable;
    }

    /**
     * 获取异常的描述信息。
     *
     * @return 异常信息字符串
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 获取触发本异常的底层异常。
     *
     * @return Throwable 对象或 null
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 返回包含异常信息和底层异常信息的字符串。
     *
     * @return 完整描述异常原因的字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LifecycleException: ");
        if (message != null) {
            sb.append(message);
            if (throwable != null) {
                sb.append(": ");
            }
        }
        if (throwable != null) {
            sb.append(throwable);
        }
        return sb.toString();
    }

}
