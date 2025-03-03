package erangel.log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供日志记录功能。
 */
public abstract class BaseLogger {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

}
