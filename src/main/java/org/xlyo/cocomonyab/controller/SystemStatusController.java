package org.xlyo.cocomonyab.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.config.version.VersionInfo;
import org.xlyo.cocomonyab.domain.vo.SystemInfoVO;
import org.xlyo.cocomonyab.domain.vo.SystemStatusVO;
import org.xlyo.cocomonyab.service.SystemReadyService;

/**
 * 系统状态REST控制器
 * <p>
 * 提供系统就绪状态查询、健康检查和版本信息接口
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
     * <p>
     * 根据系统就绪状态返回不同的HTTP状态码：
     * - 未就绪时返回 HTTP 503 和 "OUT_OF_SERVICE" 状态
     * - 就绪后返回 HTTP 200 和 "UP" 状态
     * </p>
     *
     * @return 系统状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<@NonNull SystemStatusVO> getSystemStatus() {
        boolean isReady = systemReadyService.getSystemReady().get();
        
        SystemStatusVO vo = SystemStatusVO.builder()
            .ready(isReady)
            .status(systemReadyService.getCurrentStatus())
            .reason(systemReadyService.getNotReadyReason())
            .timestamp(System.currentTimeMillis())
            .progress(systemReadyService.getProgress())
            .currentPhase(systemReadyService.getCurrentPhase())
            .build();
        
        // 根据就绪状态返回不同的HTTP状态码
        if (isReady) {
            return ResponseEntity.ok(vo);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(vo);
        }
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
    
    /**
     * 获取系统版本信息
     * GET /api/system/info
     *
     * @return 版本信息
     */
    @GetMapping("/info")
    public ApiResponse<SystemInfoVO> getSystemInfo() {
        SystemInfoVO vo = new SystemInfoVO(
            VersionInfo.PROJECT_NAME,
            VersionInfo.VERSION,
            VersionInfo.GROUP,
            VersionInfo.DESCRIPTION,
            VersionInfo.BUILD_TIME,
            VersionInfo.JAVA_VERSION,
            VersionInfo.GRADLE_VERSION,
            VersionInfo.getFullVersionInfo()
        );
        return ApiResponse.success(vo);
    }
}

