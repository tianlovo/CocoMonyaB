package org.xlyo.cocomonyab.actuator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.ApplicationReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;
import org.xlyo.cocomonyab.event.startup.StartupStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 启动健康检查指示器
 * <p>
 * 实现 Spring Boot Actuator 的 HealthIndicator 接口，提供应用启动状态的健康检查。
 * 在应用未完全启动时返回 OUT_OF_SERVICE 状态，启动完成后返回 UP 状态。
 * </p>
 * <p>
 * 功能：
 * <ul>
 *   <li>监听 ApplicationReadyEvent 事件，更新就绪状态</li>
 *   <li>未就绪时返回 OUT_OF_SERVICE 状态</li>
 *   <li>就绪后返回 UP 状态</li>
 *   <li>包含启动阶段信息和总耗时</li>
 * </ul>
 * </p>
 *
 * @see HealthIndicator
 * @see ApplicationReadyEvent
 * @see StartupProgressTracker
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupHealthIndicator implements HealthIndicator {
    
    private final StartupProgressTracker progressTracker;
    
    /**
     * 应用是否已就绪
     */
    private volatile boolean ready = false;
    
    /**
     * 当前启动阶段
     */
    private volatile String currentPhase = "未启动";
    
    /**
     * 监听应用就绪事件
     * <p>
     * 当应用完全启动后，更新就绪状态。
     * </p>
     *
     * @param event 应用就绪事件
     */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        this.ready = true;
        this.currentPhase = "应用就绪";
        log.info("✅ 应用健康检查状态更新为就绪");
    }
    
    /**
     * 更新当前启动阶段
     * <p>
     * 用于在启动过程中更新当前阶段信息。
     * </p>
     *
     * @param phase 当前阶段名称
     */
    public void updateCurrentPhase(String phase) {
        this.currentPhase = phase;
    }
    
    /**
     * 执行健康检查
     * <p>
     * 根据应用启动状态返回相应的健康状态：
     * <ul>
     *   <li>未就绪：返回 OUT_OF_SERVICE 状态，包含当前阶段信息</li>
     *   <li>已就绪：返回 UP 状态，包含总启动时间和阶段统计</li>
     * </ul>
     * </p>
     *
     * @return 健康状态
     */
    @Override
    public Health health() {
        if (!ready) {
            // 未就绪时返回 OUT_OF_SERVICE 状态
            return Health.outOfService()
                    .withDetail("phase", currentPhase)
                    .withDetail("ready", false)
                    .build();
        }
        
        // 就绪后返回 UP 状态，包含启动信息
        Map<String, Object> details = new HashMap<>();
        details.put("phase", currentPhase);
        details.put("ready", true);
        details.put("totalTime", progressTracker.getTotalTime());
        
        // 添加各阶段统计信息
        Map<String, Object> phases = new HashMap<>();
        progressTracker.getPhases().forEach((name, info) -> {
            Map<String, Object> phaseDetails = new HashMap<>();
            phaseDetails.put("status", info.getStatus().toString());
            phaseDetails.put("duration", info.getDuration());
            phases.put(name, phaseDetails);
        });
        details.put("phases", phases);
        
        return Health.up()
                .withDetails(details)
                .build();
    }
}
