package org.xlyo.cocomonyab.source.unread.exception;

import org.drinkless.tdlib.TdApi;

/**
 * 临时错误异常
 * <p>
 * 当遇到临时性错误（如 5xx 服务器错误）时抛出此异常
 * 这类错误通常可以通过重试解决
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
public class TemporaryErrorException extends UnreadMessageFetchException {
    
    private final TdApi.Error error;
    
    public TemporaryErrorException(TdApi.Error error) {
        super("临时错误: " + error.message);
        this.error = error;
    }
    
    public TdApi.Error getError() {
        return error;
    }
    
    public int getErrorCode() {
        return error.code;
    }
}
