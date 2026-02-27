package org.xlyo.cocomonyab.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.service.SystemReadyService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SystemReadyInterceptor单元测试
 */
@ExtendWith(MockitoExtension.class)
class SystemReadyInterceptorTest {
    
    @Mock
    private SystemReadyService systemReadyService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    private ObjectMapper objectMapper;
    private SystemReadyInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        interceptor = new SystemReadyInterceptor(systemReadyService, objectMapper);
    }
    
    @Test
    void testPreHandle_SystemReady() throws Exception {
        // 模拟系统已就绪
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(true));
        
        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, new Object());
        
        // 验证返回true，允许请求继续
        assertTrue(result);
        
        // 验证没有设置响应
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).getWriter();
    }
    
    @Test
    void testPreHandle_SystemNotReady() throws Exception {
        // 模拟系统未就绪
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(false));
        when(systemReadyService.getNotReadyReason()).thenReturn("系统正在启动中...");
        
        // 模拟请求信息
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/channel/list");
        
        // 模拟响应写入
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, new Object());
        
        // 验证返回false，拦截请求
        assertFalse(result);
        
        // 验证设置了503状态码
        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 验证响应内容
        writer.flush();
        String responseBody = stringWriter.toString();
        assertNotNull(responseBody);
        
        ApiResponse<?> apiResponse = objectMapper.readValue(responseBody, ApiResponse.class);
        assertEquals(ResponseCode.SERVICE_UNAVAILABLE.getCode(), apiResponse.getCode());
        assertEquals("系统正在启动中...", apiResponse.getMsg());
        assertNull(apiResponse.getData());
    }
    
    @Test
    void testPreHandle_CustomNotReadyReason() throws Exception {
        // 模拟系统未就绪，自定义原因
        String customReason = "系统维护中，预计10分钟后恢复";
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(false));
        when(systemReadyService.getNotReadyReason()).thenReturn(customReason);
        
        // 模拟请求信息
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/channel");
        
        // 模拟响应写入
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, new Object());
        
        // 验证返回false
        assertFalse(result);
        
        // 验证响应内容包含自定义原因
        writer.flush();
        String responseBody = stringWriter.toString();
        ApiResponse<?> apiResponse = objectMapper.readValue(responseBody, ApiResponse.class);
        assertEquals(customReason, apiResponse.getMsg());
    }
    
    @Test
    void testPreHandle_SystemStatusApiNotIntercepted() throws Exception {
        // 模拟系统未就绪
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(false));
        when(systemReadyService.getNotReadyReason()).thenReturn("系统正在启动中...");
        
        // 模拟系统状态API请求
        when(request.getRequestURI()).thenReturn("/api/system/status");
        
        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, new Object());
        
        // 验证返回true，允许访问系统状态API
        assertTrue(result);
        
        // 验证没有设置响应（没有被拦截）
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).getWriter();
    }
    
    @Test
    void testPreHandle_SystemHealthApiNotIntercepted() throws Exception {
        // 模拟系统未就绪
        when(systemReadyService.getSystemReady()).thenReturn(new AtomicBoolean(false));
        
        // 模拟健康检查API请求
        when(request.getRequestURI()).thenReturn("/api/system/health");
        
        // 执行拦截器
        boolean result = interceptor.preHandle(request, response, new Object());
        
        // 验证返回true，允许访问健康检查API
        assertTrue(result);
        
        // 验证没有设置响应
        verify(response, never()).setStatus(anyInt());
    }
}
