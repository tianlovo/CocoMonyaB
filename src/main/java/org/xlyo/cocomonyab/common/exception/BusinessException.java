package org.xlyo.cocomonyab.common.exception;

import lombok.Getter;
import org.xlyo.cocomonyab.common.enums.ResponseCode;

/**
 * 业务异常基类
 * 所有业务异常都应继承此类
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private final Integer code;
    
    /**
     * 错误消息
     */
    private final String message;
    
    /**
     * 使用响应码枚举构造
     */
    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }
    
    /**
     * 使用响应码枚举和自定义消息构造
     */
    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
        this.message = message;
    }
    
    /**
     * 使用自定义错误码和消息构造
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    /**
     * 使用响应码枚举和原因构造
     */
    public BusinessException(ResponseCode responseCode, Throwable cause) {
        super(responseCode.getMessage(), cause);
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }
    
    /**
     * 使用响应码枚举、自定义消息和原因构造
     */
    public BusinessException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.code = responseCode.getCode();
        this.message = message;
    }
}
