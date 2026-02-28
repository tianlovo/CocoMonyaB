package org.xlyo.cocomonyab.event.startup;

/**
 * 启动异常
 * <p>
 * 当启动过程中发生致命错误时抛出此异常，表示应用无法继续启动。
 * </p>
 */
public class StartupException extends RuntimeException {
    
    /**
     * 构造启动异常
     *
     * @param message 错误消息
     */
    public StartupException(String message) {
        super(message);
    }
    
    /**
     * 构造启动异常
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public StartupException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造启动异常
     *
     * @param cause 原因异常
     */
    public StartupException(Throwable cause) {
        super(cause);
    }
}
