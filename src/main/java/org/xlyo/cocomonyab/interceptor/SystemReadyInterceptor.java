package org.xlyo.cocomonyab.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.service.SystemReadyService;

import java.nio.charset.StandardCharsets;

/**
 * 系统就绪状态拦截器
 * <p>
 * 在系统完全启动完成前，拦截所有API请求并返回503错误
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemReadyInterceptor implements HandlerInterceptor {
    
    private final SystemReadyService systemReadyService;
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        
        // 白名单：系统状态API不拦截，允许在任何时候查询系统状态
        if (requestUri.startsWith("/api/system/")) {
            return true;
        }
        
        // 检查系统是否就绪
        if (!systemReadyService.getSystemReady().get()) {
            String reason = systemReadyService.getNotReadyReason();
            log.warn("系统未就绪，拒绝请求: {} {}, 原因: {}", 
                    request.getMethod(), requestUri, reason);
            
            // 设置响应状态码为503 Service Unavailable
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            
            // 返回统一格式的错误响应
            ApiResponse<Void> result = ApiResponse.error(ResponseCode.SERVICE_UNAVAILABLE, reason);
            String jsonResponse = objectMapper.writeValueAsString(result);
            response.getWriter().write(jsonResponse);
            
            return false;
        }
        
        return true;
    }
}
