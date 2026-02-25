package org.xlyo.cocomonyab.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.response.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 单元测试
 * 测试各种异常场景的响应格式和错误码
 */
@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }
    
    @Test
    @DisplayName("处理业务异常 - 应返回正确的错误码和消息")
    void handleBusinessException_shouldReturnCorrectResponse() {
        // Given
        BusinessException exception = new BusinessException(
                ResponseCode.BUSINESS_ERROR,
                "测试业务异常"
        );
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleBusinessException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.BUSINESS_ERROR.getCode(), response.getCode());
        assertEquals("测试业务异常", response.getMsg());
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理标签唯一性冲突异常 - 应返回详细的冲突信息")
    void handleTagUniquenessException_shouldReturnDetailedConflictInfo() {
        // Given
        TagUniquenessException exception = new TagUniquenessException(
                "AUTHOR",
                "author123",
                "张三"
        );
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleTagUniquenessException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.DATA_ALREADY_EXISTS.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("AUTHOR"));
        assertTrue(response.getMsg().contains("author123"));
        assertTrue(response.getMsg().contains("张三"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理引用完整性异常 - 应返回详细的引用列表")
    void handleReferenceIntegrityException_shouldReturnDetailedReferenceInfo() {
        // Given
        Map<String, List<String>> references = new HashMap<>();
        references.put("角色", List.of("char1(角色1)", "char2(角色2)"));
        references.put("过滤配置", List.of("config1", "config2"));
        
        ReferenceIntegrityException exception = new ReferenceIntegrityException(
                "无法删除：该作者被引用",
                references
        );
        
        // When
        ApiResponse<Map<String, List<String>>> response = 
                exceptionHandler.handleReferenceIntegrityException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.OPERATION_FAILED.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("无法删除"));
        assertTrue(response.getMsg().contains("角色(2个)"));
        assertTrue(response.getMsg().contains("过滤配置(2个)"));
        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals(2, response.getData().get("角色").size());
        assertEquals(2, response.getData().get("过滤配置").size());
    }
    
    @Test
    @DisplayName("处理引用完整性异常 - 空引用列表")
    void handleReferenceIntegrityException_withEmptyReferences_shouldReturnBasicMessage() {
        // Given
        Map<String, List<String>> references = new HashMap<>();
        
        ReferenceIntegrityException exception = new ReferenceIntegrityException(
                "无法删除",
                references
        );
        
        // When
        ApiResponse<Map<String, List<String>>> response = 
                exceptionHandler.handleReferenceIntegrityException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.OPERATION_FAILED.getCode(), response.getCode());
        assertEquals("无法删除", response.getMsg());
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }
    
    @Test
    @DisplayName("处理参数校验异常 - 应返回所有校验失败信息")
    void handleMethodArgumentNotValidException_shouldReturnAllValidationErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = 
                mock(org.springframework.validation.BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("object", "field1", "字段1不能为空"),
                new FieldError("object", "field2", "字段2格式错误")
        ));
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleMethodArgumentNotValidException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("字段1不能为空"));
        assertTrue(response.getMsg().contains("字段2格式错误"));
        assertTrue(response.getMsg().contains(";"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理参数绑定异常 - 应返回所有绑定错误")
    void handleBindException_shouldReturnAllBindingErrors() {
        // Given
        BindException exception = new BindException(new Object(), "object");
        exception.addError(new FieldError("object", "field1", "绑定错误1"));
        exception.addError(new FieldError("object", "field2", "绑定错误2"));
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleBindException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("绑定错误1"));
        assertTrue(response.getMsg().contains("绑定错误2"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理缺少请求参数异常 - 应返回参数名称")
    void handleMissingServletRequestParameterException_shouldReturnParameterName() {
        // Given
        MissingServletRequestParameterException exception = 
                new MissingServletRequestParameterException("userId", "String");
        
        // When
        ApiResponse<Void> response = 
                exceptionHandler.handleMissingServletRequestParameterException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("userId"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理参数类型不匹配异常 - 应返回类型信息")
    void handleMethodArgumentTypeMismatchException_shouldReturnTypeInfo() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismException.class);
        when(exception.getName()).thenReturn("age");
        when(exception.getValue()).thenReturn("abc");
        when(exception.getRequiredType()).thenReturn(Integer.class);
        
        // When
        ApiResponse<Void> response = 
                exceptionHandler.handleMethodArgumentTypeMismatchException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("age"));
        assertTrue(response.getMsg().contains("Integer"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理HTTP消息不可读异常 - 应返回格式错误提示")
    void handleHttpMessageNotReadableException_shouldReturnFormatError() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");
        
        // When
        ApiResponse<Void> response = 
                exceptionHandler.handleHttpMessageNotReadableException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), response.getCode());
        assertEquals("请求体格式错误", response.getMsg());
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理请求方法不支持异常 - 应返回方法信息")
    void handleHttpRequestMethodNotSupportedException_shouldReturnMethodInfo() {
        // Given
        HttpRequestMethodNotSupportedException exception = 
                new HttpRequestMethodNotSupportedException("POST");
        
        // When
        ApiResponse<Void> response = 
                exceptionHandler.handleHttpRequestMethodNotSupportedException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("POST"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理通用异常 - 应返回系统内部错误")
    void handleException_shouldReturnInternalError() {
        // Given
        Exception exception = new RuntimeException("未预期的错误");
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.INTERNAL_ERROR.getCode(), response.getCode());
        assertEquals("系统内部错误，请联系管理员", response.getMsg());
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理数据不存在异常 - 应返回正确的错误码")
    void handleDataNotFoundException_shouldReturnCorrectCode() {
        // Given
        BusinessException exception = new BusinessException(
                ResponseCode.DATA_NOT_FOUND,
                "作者不存在: author123"
        );
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleBusinessException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), response.getCode());
        assertTrue(response.getMsg().contains("author123"));
        assertNull(response.getData());
    }
    
    @Test
    @DisplayName("处理操作失败异常 - 应返回正确的错误码")
    void handleOperationFailedException_shouldReturnCorrectCode() {
        // Given
        BusinessException exception = new BusinessException(
                ResponseCode.OPERATION_FAILED,
                "删除操作失败"
        );
        
        // When
        ApiResponse<Void> response = exceptionHandler.handleBusinessException(exception);
        
        // Then
        assertNotNull(response);
        assertEquals(ResponseCode.OPERATION_FAILED.getCode(), response.getCode());
        assertEquals("删除操作失败", response.getMsg());
        assertNull(response.getData());
    }
}
