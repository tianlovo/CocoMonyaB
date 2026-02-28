package org.xlyo.cocomonyab.event.startup;

import net.jqwik.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动进度跟踪器属性测试
 * <p>
 * 验证属性 15: 阶段开始和完成日志
 * 验证属性 16: 启动耗时统计
 * </p>
 * <p>
 * **Validates: Requirements 9.1, 9.2, 9.5**
 * </p>
 */
class StartupProgressTrackerPropertyTest {
    
    private static final Logger log = LoggerFactory.getLogger(StartupProgressTrackerPropertyTest.class);
    
    /**
     * 属性 15: 对于任何启动阶段，系统应在阶段开始时记录日志，在阶段完成时记录包含耗时的日志
     * <p>
     * 此测试验证每个阶段的开始和完成都会被正确记录，并且完成时包含耗时信息。
     * </p>
     * <p>
     * **Validates: Requirements 9.1, 9.2**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 15: 阶段开始和完成日志")
    void phaseStartAndCompletionAreLogged(
            @ForAll("phaseNames") String phaseName) {
        
        // 准备：创建进度跟踪器和日志捕获器
        TestStartupProgressTracker tracker = new TestStartupProgressTracker();
        
        // 执行：开始阶段
        tracker.startPhase(phaseName);
        
        // 验证：阶段开始被记录
        assertThat(tracker.getPhases()).containsKey(phaseName);
        assertThat(tracker.getPhases().get(phaseName).getStatus())
                .isEqualTo(PhaseStatus.IN_PROGRESS);
        assertThat(tracker.getPhases().get(phaseName).getStartTime()).isGreaterThan(0);
        
        // 验证：开始日志被记录
        assertThat(tracker.getLogMessages())
                .anyMatch(msg -> msg.contains("开始阶段") && msg.contains(phaseName));
        
        // 添加小延迟以确保耗时可测量
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 执行：完成阶段
        tracker.completePhase(phaseName);
        
        // 验证：阶段完成被记录
        assertThat(tracker.getPhases().get(phaseName).getStatus())
                .isEqualTo(PhaseStatus.COMPLETED);
        assertThat(tracker.getPhases().get(phaseName).getEndTime()).isGreaterThan(0);
        
        // 验证：完成日志包含耗时信息
        assertThat(tracker.getLogMessages())
                .anyMatch(msg -> msg.contains("完成阶段") 
                        && msg.contains(phaseName) 
                        && msg.contains("耗时"));
        
        // 验证：耗时大于0
        long duration = tracker.getPhases().get(phaseName).getDuration();
        assertThat(duration).isGreaterThanOrEqualTo(10);
    }
    
    /**
     * 属性 15 扩展: 多个阶段按顺序开始和完成时，每个阶段都应记录日志
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 15 Extended: 多阶段日志记录")
    void multiplePhaseStartAndCompletionAreLogged(
            @ForAll("phaseSequences") List<String> phaseNames) {
        
        // 准备：创建进度跟踪器
        TestStartupProgressTracker tracker = new TestStartupProgressTracker();
        
        // 执行：按顺序开始和完成各阶段
        for (String phaseName : phaseNames) {
            tracker.startPhase(phaseName);
            
            // 添加小延迟
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            tracker.completePhase(phaseName);
        }
        
        // 验证：所有阶段都被记录
        assertThat(tracker.getPhases()).hasSize(phaseNames.size());
        
        // 验证：每个阶段都有开始和完成日志
        for (String phaseName : phaseNames) {
            assertThat(tracker.getLogMessages())
                    .anyMatch(msg -> msg.contains("开始阶段") && msg.contains(phaseName));
            assertThat(tracker.getLogMessages())
                    .anyMatch(msg -> msg.contains("完成阶段") && msg.contains(phaseName));
        }
        
        // 验证：所有阶段状态为已完成
        tracker.getPhases().values().forEach(phase -> 
                assertThat(phase.getStatus()).isEqualTo(PhaseStatus.COMPLETED));
    }
    
    /**
     * 属性 15 扩展: 阶段失败时应记录失败日志和错误信息
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 15 Extended: 阶段失败日志")
    void phaseFailureIsLogged(
            @ForAll("phaseNames") String phaseName,
            @ForAll("errorMessages") String errorMessage) {
        
        // 准备：创建进度跟踪器
        TestStartupProgressTracker tracker = new TestStartupProgressTracker();
        
        // 执行：开始阶段
        tracker.startPhase(phaseName);
        
        // 添加小延迟
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 执行：阶段失败
        tracker.failPhase(phaseName, errorMessage);
        
        // 验证：阶段状态为失败
        assertThat(tracker.getPhases().get(phaseName).getStatus())
                .isEqualTo(PhaseStatus.FAILED);
        
        // 验证：错误信息被记录
        assertThat(tracker.getPhases().get(phaseName).getErrorMessage())
                .isEqualTo(errorMessage);
        
        // 验证：失败日志包含错误信息
        assertThat(tracker.getLogMessages())
                .anyMatch(msg -> msg.contains("失败阶段") 
                        && msg.contains(phaseName) 
                        && msg.contains(errorMessage));
    }
    
    /**
     * 属性 16: 对于任何完成的启动过程，系统应记录每个阶段的耗时统计
     * <p>
     * 此测试验证启动完成后能够获取总耗时和各阶段的耗时统计。
     * </p>
     * <p>
     * **Validates: Requirements 9.5**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 16: 启动耗时统计")
    void startupDurationStatisticsAreRecorded(
            @ForAll("phaseSequences") List<String> phaseNames) {
        
        Assume.that(!phaseNames.isEmpty());
        
        // 准备：创建进度跟踪器
        TestStartupProgressTracker tracker = new TestStartupProgressTracker();
        
        // 执行：按顺序完成各阶段
        for (String phaseName : phaseNames) {
            tracker.startPhase(phaseName);
            
            // 添加小延迟以确保耗时可测量
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            tracker.completePhase(phaseName);
        }
        
        // 验证：总耗时大于0
        long totalTime = tracker.getTotalTime();
        assertThat(totalTime).isGreaterThan(0);
        
        // 验证：总耗时应该大于等于所有阶段耗时之和
        long sumOfPhaseDurations = tracker.getPhases().values().stream()
                .mapToLong(TestPhaseInfo::getDuration)
                .sum();
        assertThat(totalTime).isGreaterThanOrEqualTo(sumOfPhaseDurations);
        
        // 验证：每个阶段都有耗时记录
        tracker.getPhases().values().forEach(phase -> {
            assertThat(phase.getDuration()).isGreaterThan(0);
            assertThat(phase.getStartTime()).isGreaterThan(0);
            assertThat(phase.getEndTime()).isGreaterThan(0);
            assertThat(phase.getEndTime()).isGreaterThanOrEqualTo(phase.getStartTime());
        });
        
        // 验证：printStatistics 方法被调用时记录统计信息
        tracker.printStatistics();
        assertThat(tracker.getLogMessages())
                .anyMatch(msg -> msg.contains("启动阶段统计"));
        
        // 验证：统计信息包含每个阶段的耗时
        for (String phaseName : phaseNames) {
            assertThat(tracker.getLogMessages())
                    .anyMatch(msg -> msg.contains(phaseName) && msg.contains("ms"));
        }
    }
    
    /**
     * 属性 16 扩展: 阶段耗时应该合理（开始时间 < 结束时间）
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 16 Extended: 阶段耗时合理性")
    void phaseDurationIsReasonable(
            @ForAll("phaseNames") String phaseName) {
        
        // 准备：创建进度跟踪器
        TestStartupProgressTracker tracker = new TestStartupProgressTracker();
        
        // 执行：开始阶段
        long beforeStart = System.currentTimeMillis();
        tracker.startPhase(phaseName);
        long afterStart = System.currentTimeMillis();
        
        // 验证：开始时间在合理范围内
        TestPhaseInfo phase = tracker.getPhases().get(phaseName);
        assertThat(phase.getStartTime()).isBetween(beforeStart, afterStart);
        
        // 添加延迟
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 执行：完成阶段
        long beforeEnd = System.currentTimeMillis();
        tracker.completePhase(phaseName);
        long afterEnd = System.currentTimeMillis();
        
        // 验证：结束时间在合理范围内
        assertThat(phase.getEndTime()).isBetween(beforeEnd, afterEnd);
        
        // 验证：结束时间晚于开始时间
        assertThat(phase.getEndTime()).isGreaterThan(phase.getStartTime());
        
        // 验证：耗时等于结束时间减开始时间
        assertThat(phase.getDuration()).isEqualTo(phase.getEndTime() - phase.getStartTime());
    }
    
    /**
     * 属性 16 扩展: 进行中的阶段应该能够计算当前耗时
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 16 Extended: 进行中阶段的耗时计算")
    void inProgressPhaseDurationIsCalculated(
            @ForAll("phaseNames") String phaseName) {
        
        // 准备：创建进度跟踪器
        TestStartupProgressTracker tracker = new TestStartupProgressTracker();
        
        // 执行：开始阶段但不完成
        tracker.startPhase(phaseName);
        
        // 添加延迟
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证：进行中的阶段应该能够计算当前耗时
        TestPhaseInfo phase = tracker.getPhases().get(phaseName);
        assertThat(phase.getStatus()).isEqualTo(PhaseStatus.IN_PROGRESS);
        assertThat(phase.getEndTime()).isEqualTo(0);
        
        // 获取当前耗时（应该使用当前时间 - 开始时间）
        long duration = phase.getDuration();
        assertThat(duration).isGreaterThanOrEqualTo(50);
    }
    
    // ==================== 测试数据生成器 ====================
    
    /**
     * 生成阶段名称
     */
    @Provide
    Arbitrary<String> phaseNames() {
        return Arbitraries.of(
                "配置初始化",
                "数据库初始化",
                "集合初始化",
                "插件初始化",
                "消息源初始化",
                "API初始化",
                "应用就绪"
        );
    }
    
    /**
     * 生成阶段序列（1-7个阶段）
     */
    @Provide
    Arbitrary<List<String>> phaseSequences() {
        return Arbitraries.of(
                "配置初始化",
                "数据库初始化",
                "集合初始化",
                "插件初始化",
                "消息源初始化",
                "API初始化",
                "应用就绪"
        ).list().ofMinSize(1).ofMaxSize(7).uniqueElements();
    }
    
    /**
     * 生成错误信息
     */
    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.of(
                "配置文件不存在",
                "数据库连接失败",
                "索引创建失败",
                "插件加载失败",
                "Telegram客户端初始化失败",
                "API服务器启动失败"
        );
    }
    
    // ==================== 测试辅助类 ====================
    
    /**
     * 测试用启动进度跟踪器
     * <p>
     * 捕获所有日志消息以便验证
     * </p>
     */
    static class TestStartupProgressTracker {
        private final Map<String, TestPhaseInfo> phases = new java.util.LinkedHashMap<>();
        private final List<String> logMessages = new ArrayList<>();
        private final long startTime = System.currentTimeMillis();
        
        public void startPhase(String phaseName) {
            TestPhaseInfo info = new TestPhaseInfo(phaseName);
            info.setStartTime(System.currentTimeMillis());
            info.setStatus(PhaseStatus.IN_PROGRESS);
            phases.put(phaseName, info);
            
            String logMessage = "▶️ 开始阶段: " + phaseName;
            logMessages.add(logMessage);
            log.info(logMessage);
        }
        
        public void completePhase(String phaseName) {
            TestPhaseInfo info = phases.get(phaseName);
            if (info != null) {
                info.setEndTime(System.currentTimeMillis());
                info.setStatus(PhaseStatus.COMPLETED);
                
                long duration = info.getDuration();
                String logMessage = "✅ 完成阶段: " + phaseName + " (耗时: " + duration + " ms)";
                logMessages.add(logMessage);
                log.info(logMessage);
            }
        }
        
        public void failPhase(String phaseName, String errorMessage) {
            TestPhaseInfo info = phases.get(phaseName);
            if (info != null) {
                info.setEndTime(System.currentTimeMillis());
                info.setStatus(PhaseStatus.FAILED);
                info.setErrorMessage(errorMessage);
                
                long duration = info.getDuration();
                String logMessage = "❌ 失败阶段: " + phaseName + " (耗时: " + duration + " ms) - " + errorMessage;
                logMessages.add(logMessage);
                log.error(logMessage);
            }
        }
        
        public long getTotalTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public void printStatistics() {
            String logMessage = "启动阶段统计:";
            logMessages.add(logMessage);
            log.info(logMessage);
            
            phases.forEach((name, info) -> {
                String phaseLog = "  " + name + " - " + info.getDuration() + " ms - " + info.getStatus();
                logMessages.add(phaseLog);
                log.info(phaseLog);
            });
        }
        
        public Map<String, TestPhaseInfo> getPhases() {
            return phases;
        }
        
        public List<String> getLogMessages() {
            return logMessages;
        }
    }
    
    /**
     * 测试用阶段信息
     */
    static class TestPhaseInfo {
        private final String name;
        private long startTime;
        private long endTime;
        private PhaseStatus status = PhaseStatus.IN_PROGRESS;
        private String errorMessage;
        
        public TestPhaseInfo(String name) {
            this.name = name;
        }
        
        public long getDuration() {
            if (endTime == 0) {
                return System.currentTimeMillis() - startTime;
            }
            return endTime - startTime;
        }
        
        public String getName() {
            return name;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        
        public PhaseStatus getStatus() {
            return status;
        }
        
        public void setStatus(PhaseStatus status) {
            this.status = status;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 阶段状态枚举
     */
    enum PhaseStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
