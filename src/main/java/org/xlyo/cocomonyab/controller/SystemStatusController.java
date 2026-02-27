package org.xlyo.cocomonyab.controller;

import lombok.RequiredArgsConstructor;
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
     *
     * @return 系统状态信息
     */
    @GetMapping("/status")
    public ApiResponse<SystemStatusVO> getSystemStatus() {
        SystemStatusVO vo = new SystemStatusVO(
            systemReadyService.getSystemReady().get(),
            systemReadyService.getNotReadyReason(),
            System.currentTimeMillis()
        );
        return ApiResponse.success(vo);
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
