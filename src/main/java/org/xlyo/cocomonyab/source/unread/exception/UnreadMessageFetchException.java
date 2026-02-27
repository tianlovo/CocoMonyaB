package org.xlyo.cocomonyab.source.unread.exception;

/**
 * 未读消息获取异常
 * <p>
 * 当获取未读消息失败时抛出此异常
 * 
 * @author tianluoqaq
 * @since 1.0
 */
public class UnreadMessageFetchException extends RuntimeException {
    
    public UnreadMessageFetchException(String message) {
        super(message);
    }
    
    public UnreadMessageFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
