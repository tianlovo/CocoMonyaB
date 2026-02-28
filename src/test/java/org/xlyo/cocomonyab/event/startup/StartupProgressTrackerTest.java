package org.xlyo.cocomonyab.event.startup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 启动进度跟踪器单元测试
 * <p>
 * 验证 StartupProgressTracker 的基本功能。
 * </p>
 */
class StartupProgressTrackerTest {
    
    private StartupProgressTracker tracker;
    
    @BeforeEach
    void setUp() {
        tracker = new StartupProgressTracker();
    }
    
    @Test
    void testStartPhase_ShouldRecordPhaseAsInProgress() {
        // 执行
        tracker.startPhase("测试阶段");
        
        // 验证
        assertThat(tracker.getPhases()).containsKey("测试阶段");
        assertThat(tracker.getPhases().get("测试阶段").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.IN_PROGRESS);
        assertThat(tracker.getPhases().get("测试阶段").getStartTime()).isGreaterThan(0);
    }
    
    @Test
    void testCompletePhase_ShouldRecordPhaseAsCompleted() throws InterruptedException {
        // 准备
        tracker.startPhase("测试阶段");
        Thread.sleep(10);
        
        // 执行
        tracker.completePhase("测试阶段");
        
        // 验证
        assertThat(tracker.getPhases().get("测试阶段").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        assertThat(tracker.getPhases().get("测试阶段").getEndTime()).isGreaterThan(0);
        assertThat(tracker.getPhases().get("测试阶段").getDuration()).isGreaterThanOrEqualTo(10);
    }
    
    @Test
    void testFailPhase_ShouldRecordPhaseAsFailed() throws InterruptedException {
        // 准备
        tracker.startPhase("测试阶段");
        Thread.sleep(10);
        
        // 执行
        tracker.failPhase("测试阶段", "测试错误");
        
        // 验证
        assertThat(tracker.getPhases().get("测试阶段").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.FAILED);
        assertThat(tracker.getPhases().get("测试阶段").getErrorMessage()).isEqualTo("测试错误");
        assertThat(tracker.getPhases().get("测试阶段").getDuration()).isGreaterThanOrEqualTo(10);
    }
    
    @Test
    void testGetTotalTime_ShouldReturnPositiveValue() {
        // 执行
        long totalTime = tracker.getTotalTime();
        
        // 验证
        assertThat(totalTime).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testMultiplePhases_ShouldTrackAllPhases() throws InterruptedException {
        // 执行
        tracker.startPhase("阶段1");
        Thread.sleep(10);
        tracker.completePhase("阶段1");
        
        tracker.startPhase("阶段2");
        Thread.sleep(10);
        tracker.completePhase("阶段2");
        
        tracker.startPhase("阶段3");
        Thread.sleep(10);
        tracker.failPhase("阶段3", "失败原因");
        
        // 验证
        assertThat(tracker.getPhases()).hasSize(3);
        assertThat(tracker.getPhases().get("阶段1").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        assertThat(tracker.getPhases().get("阶段2").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        assertThat(tracker.getPhases().get("阶段3").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.FAILED);
    }
    
    @Test
    void testPrintStatistics_ShouldNotThrowException() {
        // 准备
        tracker.startPhase("阶段1");
        tracker.completePhase("阶段1");
        
        // 执行和验证（不应抛出异常）
        tracker.printStatistics();
    }
    
    @Test
    void testInProgressPhaseDuration_ShouldCalculateCurrentDuration() throws InterruptedException {
        // 准备
        tracker.startPhase("测试阶段");
        Thread.sleep(50);
        
        // 执行
        long duration = tracker.getPhases().get("测试阶段").getDuration();
        
        // 验证：进行中的阶段应该能够计算当前耗时
        assertThat(duration).isGreaterThanOrEqualTo(50);
        assertThat(tracker.getPhases().get("测试阶段").getEndTime()).isEqualTo(0);
    }
}
