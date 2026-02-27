package org.xlyo.cocomonyab.common.enums;

import lombok.Getter;

/**
 * 响应状态码枚举
 * 规则：成功为正数，错误为负数
 */
@Getter
public enum ResponseCode {
    // 成功响应
    SUCCESS(200, "操作成功"),
    
    // 客户端错误 (-400xx)
    BAD_REQUEST(-40000, "请求参数错误"),
    UNAUTHORIZED(-40001, "未授权"),
    FORBIDDEN(-40003, "禁止访问"),
    NOT_FOUND(-40004, "资源不存在"),
    METHOD_NOT_ALLOWED(-40005, "请求方法不允许"),
    VALIDATION_ERROR(-40006, "参数校验失败"),
    
    // 服务端错误 (-500xx)
    INTERNAL_ERROR(-50000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(-50003, "服务暂时不可用"),
    
    // 业务错误 (-600xx)
    BUSINESS_ERROR(-60000, "业务处理失败"),
    TELEGRAM_ERROR(-60001, "Telegram操作失败"),
    DATA_NOT_FOUND(-60002, "数据不存在"),
    DATA_ALREADY_EXISTS(-60003, "数据已存在"),
    OPERATION_FAILED(-60004, "操作失败");
    
    private final int code;
    private final String message;
    
    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
