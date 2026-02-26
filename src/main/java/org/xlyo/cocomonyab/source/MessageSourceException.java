package org.xlyo.cocomonyab.source;

/**
 * 消息来源异常
 * <p>
 * 当消息来源在启动、停止或处理消息时发生错误时抛出此异常
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
public class MessageSourceException extends Exception {
    
    /**
     * 构造一个新的消息来源异常
     * 
     * @param message 异常消息
     */
    public MessageSourceException(String message) {
        super(message);
    }
    
    /**
     * 构造一个新的消息来源异常
     * 
     * @param message 异常消息
     * @param cause 原因
     */
    public MessageSourceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造一个新的消息来源异常
     * 
     * @param cause 原因
     */
    public MessageSourceException(Throwable cause) {
        super(cause);
    }
}
