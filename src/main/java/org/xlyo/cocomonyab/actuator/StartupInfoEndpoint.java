package org.xlyo.cocomonyab.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.ApplicationReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动信息端点
 * <p>
 * 实现自定义 Actuator 端点 /actuator/startup，提供启动阶段信息和耗时统计。
 * </p>
 * <p>
 * 功能：
 * <ul>
 *   <li>返回所有启动阶段的详细信息（名称、状态、开始时间、结束时间、耗时）</li>
 *   <li>返回总启动时间</li>
 *   <li>返回当前启动状态</li>
 * </ul>
 * </p>
 *
 * @see Endpoint
 * @see StartupProgressTracker
 */
@Component
@Endpoint(id = "startup")
@RequiredArgsConstructor
public class StartupInfoEndpoint {
    
    private final StartupProgressTracker progressTracker;
    
    /**
     * 应用是否已就绪
     */
    private volatile boolean ready = false;
    
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
    }
    
    /**
     * 获取启动信息
     * <p>
     * 返回启动阶段信息和耗时统计。
     * 响应格式：
     * <pre>
     * {
     *   "phases": [
     *     {
     *       "name": "配置初始化",
     *       "status": "COMPLETED",
     *       "startTime": 1708588800000,
     *       "endTime": 1708588801000,
     *       "duration": 1000
     *     }
     *   ],
     *   "totalDuration": 15234,
     *   "status": "READY"
     * }
     * </pre>
     * </p>
     *
     * @return 启动信息映射
     */
    @ReadOperation
    public Map<String, Object> startupInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 构建阶段信息列表
        List<Map<String, Object>> phases = new ArrayList<>();
        progressTracker.getPhases().forEach((name, phaseInfo) -> {
            Map<String, Object> phase = new HashMap<>();
            phase.put("name", name);
            phase.put("status", phaseInfo.getStatus().toString());
            phase.put("startTime", phaseInfo.getStartTime());
            phase.put("endTime", phaseInfo.getEndTime());
            phase.put("duration", phaseInfo.getDuration());
            
            // 如果有错误信息，添加到响应中
            if (phaseInfo.getErrorMessage() != null) {
                phase.put("errorMessage", phaseInfo.getErrorMessage());
            }
            
            phases.add(phase);
        });
        
        info.put("phases", phases);
        info.put("totalDuration", progressTracker.getTotalTime());
        info.put("status", ready ? "READY" : "STARTING");
        
        return info;
    }
}
