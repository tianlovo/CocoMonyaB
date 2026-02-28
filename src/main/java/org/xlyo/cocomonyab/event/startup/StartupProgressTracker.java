package org.xlyo.cocomonyab.event.startup;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动进度跟踪器
 * <p>
 * 负责跟踪应用启动过程中各阶段的进度、耗时和状态。
 * 提供阶段开始、完成、失败的跟踪方法，以及耗时统计和日志输出功能。
 * </p>
 * <p>
 * 功能：
 * <ul>
 *   <li>跟踪每个启动阶段的开始时间、结束时间和状态</li>
 *   <li>记录阶段开始和完成的日志（需求 9.1, 9.2）</li>
 *   <li>计算每个阶段的耗时</li>
 *   <li>提供总启动时间统计</li>
 *   <li>输出启动阶段统计信息（需求 9.5）</li>
 * </ul>
 * </p>
 *
 * @see StartupEvent
 * @see StartupStatus
 */
@Component
@Slf4j
public class StartupProgressTracker {
    
    /**
     * 存储所有阶段的信息
     * 使用 LinkedHashMap 保持插入顺序
     */
    private final Map<String, PhaseInfo> phases = new LinkedHashMap<>();
    
    /**
     * 应用启动的开始时间
     */
    private final long startTime = System.currentTimeMillis();
    
    /**
     * 开始一个启动阶段
     * <p>
     * 记录阶段的开始时间，并输出开始日志。
     * </p>
     *
     * @param phaseName 阶段名称
     */
    public void startPhase(String phaseName) {
        PhaseInfo info = new PhaseInfo(phaseName);
        info.setStartTime(System.currentTimeMillis());
        info.setStatus(PhaseStatus.IN_PROGRESS);
        phases.put(phaseName, info);
        
        log.info("▶️ 开始阶段: {}", phaseName);
    }
    
    /**
     * 完成一个启动阶段
     * <p>
     * 记录阶段的结束时间，计算耗时，并输出完成日志。
     * </p>
     *
     * @param phaseName 阶段名称
     */
    public void completePhase(String phaseName) {
        PhaseInfo info = phases.get(phaseName);
        if (info != null) {
            info.setEndTime(System.currentTimeMillis());
            info.setStatus(PhaseStatus.COMPLETED);
            
            long duration = info.getDuration();
            log.info("✅ 完成阶段: {} (耗时: {} ms)", phaseName, duration);
        }
    }
    
    /**
     * 标记一个启动阶段失败
     * <p>
     * 记录阶段的结束时间、失败状态和错误信息，并输出失败日志。
     * </p>
     *
     * @param phaseName    阶段名称
     * @param errorMessage 错误信息
     */
    public void failPhase(String phaseName, String errorMessage) {
        PhaseInfo info = phases.get(phaseName);
        if (info != null) {
            info.setEndTime(System.currentTimeMillis());
            info.setStatus(PhaseStatus.FAILED);
            info.setErrorMessage(errorMessage);
            
            long duration = info.getDuration();
            log.error("❌ 失败阶段: {} (耗时: {} ms) - {}", phaseName, duration, errorMessage);
        }
    }
    
    /**
     * 获取总启动时间
     * <p>
     * 计算从应用启动到当前时刻的总耗时。
     * </p>
     *
     * @return 总启动时间（毫秒）
     */
    public long getTotalTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 输出启动阶段统计信息
     * <p>
     * 输出所有阶段的名称、耗时和状态。
     * 用于启动完成后的统计报告（需求 9.5）。
     * </p>
     */
    public void printStatistics() {
        log.info("启动阶段统计:");
        phases.forEach((name, info) -> {
            log.info("  {} - {} ms - {}", name, info.getDuration(), info.getStatus());
        });
    }
    
    /**
     * 获取所有阶段信息
     * <p>
     * 用于测试和监控目的。
     * </p>
     *
     * @return 阶段信息映射
     */
    public Map<String, PhaseInfo> getPhases() {
        return phases;
    }
    
    /**
     * 阶段信息
     * <p>
     * 存储单个启动阶段的详细信息，包括名称、时间、状态和错误信息。
     * </p>
     */
    @Getter
    @Setter
    public static class PhaseInfo {
        /**
         * 阶段名称
         */
        private final String name;
        
        /**
         * 开始时间（毫秒时间戳）
         */
        private long startTime;
        
        /**
         * 结束时间（毫秒时间戳）
         */
        private long endTime;
        
        /**
         * 阶段状态
         */
        private PhaseStatus status = PhaseStatus.IN_PROGRESS;
        
        /**
         * 错误信息（仅在失败时有值）
         */
        private String errorMessage;
        
        /**
         * 构造函数
         *
         * @param name 阶段名称
         */
        public PhaseInfo(String name) {
            this.name = name;
        }
        
        /**
         * 获取阶段耗时
         * <p>
         * 如果阶段尚未结束（endTime为0），则返回当前时刻的耗时。
         * 否则返回结束时间与开始时间的差值。
         * </p>
         *
         * @return 耗时（毫秒）
         */
        public long getDuration() {
            if (endTime == 0) {
                return System.currentTimeMillis() - startTime;
            }
            return endTime - startTime;
        }
    }
    
    /**
     * 阶段状态枚举
     * <p>
     * 定义启动阶段的三种状态：进行中、已完成、失败。
     * </p>
     */
    public enum PhaseStatus {
        /**
         * 进行中
         */
        IN_PROGRESS,
        
        /**
         * 已完成
         */
        COMPLETED,
        
        /**
         * 失败
         */
        FAILED
    }
}
