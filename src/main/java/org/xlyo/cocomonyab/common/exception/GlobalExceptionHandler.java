package org.xlyo.cocomonyab.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.response.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回规范化的JSON响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
    
    /**
     * 标签唯一性冲突异常处理
     * 返回详细的冲突信息，包括冲突实体类型、ID和名称
     */
    @ExceptionHandler(TagUniquenessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleTagUniquenessException(TagUniquenessException e) {
        log.warn("标签唯一性冲突: type={}, id={}, value={}", 
                e.getConflictType(), e.getConflictId(), e.getConflictValue());
        
        String detailedMessage = String.format(
                "名称或别名已存在：冲突实体类型=%s, ID=%s, 名称=%s",
                e.getConflictType(),
                e.getConflictId(),
                e.getConflictValue()
        );
        
        return ApiResponse.error(ResponseCode.DATA_ALREADY_EXISTS, detailedMessage);
    }
    
    /**
     * 引用完整性异常处理
     * 返回详细的引用信息列表
     */
    @ExceptionHandler(ReferenceIntegrityException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Map<String, List<String>>> handleReferenceIntegrityException(ReferenceIntegrityException e) {
        log.warn("引用完整性冲突: message={}, references={}", e.getMessage(), e.getReferences());
        
        // 构建详细的错误消息
        StringBuilder messageBuilder = new StringBuilder(e.getMessage());
        if (e.getReferences() != null && !e.getReferences().isEmpty()) {
            messageBuilder.append("：");
            e.getReferences().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    messageBuilder.append(String.format("%s(%d个)", key, values.size()));
                    messageBuilder.append(", ");
                }
            });
            // 移除最后的逗号和空格
            if (messageBuilder.length() > 2) {
                messageBuilder.setLength(messageBuilder.length() - 2);
            }
        }
        
        return new ApiResponse<>(
                ResponseCode.OPERATION_FAILED.getCode(),
                messageBuilder.toString(),
                e.getReferences()
        );
    }
    
    /**
     * 参数校验异常处理 (MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errorMsg);
        return ApiResponse.error(ResponseCode.VALIDATION_ERROR, errorMsg);
    }
    
    /**
     * 参数绑定异常处理 (BindException)
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", errorMsg);
        return ApiResponse.error(ResponseCode.VALIDATION_ERROR, errorMsg);
    }
    
    /**
     * 缺少请求参数异常处理
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ApiResponse.error(ResponseCode.BAD_REQUEST, "缺少必需参数: " + e.getParameterName());
    }
    
    /**
     * 参数类型不匹配异常处理
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: name={}, value={}", e.getName(), e.getValue());
        return ApiResponse.error(ResponseCode.BAD_REQUEST, 
                String.format("参数类型错误: %s 应为 %s 类型", e.getName(), e.getRequiredType().getSimpleName()));
    }
    
    /**
     * HTTP消息不可读异常处理
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HTTP消息不可读: {}", e.getMessage());
        return ApiResponse.error(ResponseCode.BAD_REQUEST, "请求体格式错误");
    }
    
    /**
     * 请求方法不支持异常处理
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return ApiResponse.error(ResponseCode.METHOD_NOT_ALLOWED, 
                String.format("不支持 %s 请求方法", e.getMethod()));
    }
    
    /**
     * 资源未找到异常处理
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源未找到: {}", e.getResourcePath());
        return ApiResponse.error(ResponseCode.NOT_FOUND, "请求的资源不存在");
    }
    
    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(ResponseCode.INTERNAL_ERROR, "系统内部错误，请联系管理员");
    }
}
