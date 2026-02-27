package org.xlyo.cocomonyab.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.service.SystemReadyService;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统状态REST控制器
 * <p>
 * 提供系统就绪状态查询接口
 * </p>
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemStatusController {
    
    private final SystemReadyService systemReadyService;
    
    /**
     * 获取系统就绪状态
     * GET /api/system/status
     *
     * @return 系统状态信息
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("ready", systemReadyService.getSystemReady().get());
        status.put("reason", systemReadyService.getNotReadyReason());
        status.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success(status);
    }
    
    /**
     * 健康检查端点
     * GET /api/system/health
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("OK");
    }
}
