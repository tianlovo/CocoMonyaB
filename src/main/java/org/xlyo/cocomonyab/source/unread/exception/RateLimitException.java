package org.xlyo.cocomonyab.source.unread.exception;

import it.tdlight.jni.TdApi;

/**
 * 速率限制异常
 * <p>
 * 当遇到 Telegram API 速率限制（429 错误）时抛出此异常
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
public class RateLimitException extends UnreadMessageFetchException {
    
    private final TdApi.Error error;
    
    public RateLimitException(TdApi.Error error) {
        super("API 速率限制: " + error.message);
        this.error = error;
    }
    
    public TdApi.Error getError() {
        return error;
    }
    
    public int getErrorCode() {
        return error.code;
    }
}
