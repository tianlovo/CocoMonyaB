package org.xlyo.cocomonyab.source.unread.exception;

import org.drinkless.tdlib.TdApi;

/**
 * 永久错误异常
 * <p>
 * 当遇到永久性错误（如 4xx 客户端错误）时抛出此异常
 * 这类错误通常不应该重试，需要修正请求参数或配置
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
public class PermanentErrorException extends UnreadMessageFetchException {
    
    private final TdApi.Error error;
    
    public PermanentErrorException(TdApi.Error error) {
        super("永久错误: " + error.message);
        this.error = error;
    }
    
    public TdApi.Error getError() {
        return error;
    }
    
    public int getErrorCode() {
        return error.code;
    }
}
