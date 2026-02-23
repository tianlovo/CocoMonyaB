package org.xlyo.cocomonyab.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MediaGroupMetrics 单元测试
 * 验证所有监控指标都被正确记录
 * 
 * 验证：需求 8.1, 8.2, 8.3, 8.4, 8.5
 */
class MediaGroupMetricsTest {
    
    private MeterRegistry meterRegistry;
    private MediaGroupMetrics mediaGroupMetrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        mediaGroupMetrics = new MediaGroupMetrics(meterRegistry);
    }
    
    /**
     * 测试媒体组缓冲区大小指标注册
     * 验证：需求 8.1
     */
    @Test
    void testRegisterBufferSizeGauge() {
        // Given: 缓冲区大小供应商
        AtomicInteger bufferSize = new AtomicInteger(10);
        
        // When: 注册 Gauge
        mediaGroupMetrics.registerBufferSizeGauge(bufferSize::get);
        
        // Then: 验证 Gauge 已注册并能读取值
        Gauge gauge = meterRegistry.find("media_group.buffer.size").gauge();
        assertNotNull(gauge, "缓冲区大小 Gauge 应该被注册");
        assertEquals(10.0, gauge.value(), "Gauge 应该返回正确的缓冲区大小");
        
        // When: 更新缓冲区大小
        bufferSize.set(20);
        
        // Then: Gauge 应该反映新值
        assertEquals(20.0, gauge.value(), "Gauge 应该实时反映缓冲区大小变化");
    }
    
    /**
     * 测试活跃媒体组数量指标注册
     * 验证：需求 8.1
     */
    @Test
    void testRegisterActiveMediaGroupCountGauge() {
        // Given: 活跃媒体组数量供应商
        AtomicInteger activeCount = new AtomicInteger(5);
        
        // When: 注册 Gauge
        mediaGroupMetrics.registerActiveMediaGroupCountGauge(activeCount::get);
        
        // Then: 验证 Gauge 已注册并能读取值
        Gauge gauge = meterRegistry.find("media_group.active.count").gauge();
        assertNotNull(gauge, "活跃媒体组数量 Gauge 应该被注册");
        assertEquals(5.0, gauge.value(), "Gauge 应该返回正确的活跃媒体组数量");
        
        // When: 更新活跃媒体组数量
        activeCount.set(15);
        
        // Then: Gauge 应该反映新值
        assertEquals(15.0, gauge.value(), "Gauge 应该实时反映活跃媒体组数量变化");
    }
    
    /**
     * 测试处理延迟指标记录
     * 验证：需求 8.2
     */
    @Test
    void testRecordProcessingDuration() {
        // Given: 媒体组键和处理时长
        String groupKey = "123:456";
        long duration = 150L;
        
        // When: 记录处理延迟
        mediaGroupMetrics.recordProcessingDuration(groupKey, duration);
        
        // Then: 验证 Timer 已记录
        Timer timer = meterRegistry.find("media_group.processing.duration")
            .tag("group_key", groupKey)
            .timer();
        
        assertNotNull(timer, "处理延迟 Timer 应该被注册");
        assertEquals(1, timer.count(), "Timer 应该记录一次");
        assertEquals(duration, timer.totalTime(TimeUnit.MILLISECONDS), 0.01, 
            "Timer 应该记录正确的处理时长");
    }
    
    /**
     * 测试多次处理延迟记录
     * 验证：需求 8.2
     */
    @Test
    void testRecordMultipleProcessingDurations() {
        // Given: 同一个媒体组的多次处理
        String groupKey = "123:456";
        
        // When: 记录多次处理延迟
        mediaGroupMetrics.recordProcessingDuration(groupKey, 100L);
        mediaGroupMetrics.recordProcessingDuration(groupKey, 200L);
        mediaGroupMetrics.recordProcessingDuration(groupKey, 300L);
        
        // Then: 验证 Timer 累计记录
        Timer timer = meterRegistry.find("media_group.processing.duration")
            .tag("group_key", groupKey)
            .timer();
        
        assertNotNull(timer, "处理延迟 Timer 应该被注册");
        assertEquals(3, timer.count(), "Timer 应该记录三次");
        assertEquals(600.0, timer.totalTime(TimeUnit.MILLISECONDS), 0.01, 
            "Timer 应该累计所有处理时长");
    }
    
    /**
     * 测试状态转换指标记录
     * 验证：需求 8.3
     */
    @Test
    void testRecordStateTransition() {
        // Given: 状态转换
        String from = "COLLECTING";
        String to = "PROCESSING";
        
        // When: 记录状态转换
        mediaGroupMetrics.recordStateTransition(from, to);
        
        // Then: 验证 Counter 已记录
        Counter counter = meterRegistry.find("media_group.state.transition")
            .tag("from", from)
            .tag("to", to)
            .counter();
        
        assertNotNull(counter, "状态转换 Counter 应该被注册");
        assertEquals(1.0, counter.count(), "Counter 应该记录一次状态转换");
    }
    
    /**
     * 测试多次状态转换记录
     * 验证：需求 8.3
     */
    @Test
    void testRecordMultipleStateTransitions() {
        // Given: 多次状态转换
        String from = "COLLECTING";
        String to = "PROCESSING";
        
        // When: 记录多次状态转换
        mediaGroupMetrics.recordStateTransition(from, to);
        mediaGroupMetrics.recordStateTransition(from, to);
        mediaGroupMetrics.recordStateTransition(from, to);
        
        // Then: 验证 Counter 累计记录
        Counter counter = meterRegistry.find("media_group.state.transition")
            .tag("from", from)
            .tag("to", to)
            .counter();
        
        assertNotNull(counter, "状态转换 Counter 应该被注册");
        assertEquals(3.0, counter.count(), "Counter 应该累计记录三次状态转换");
    }
    
    /**
     * 测试不同状态转换分别记录
     * 验证：需求 8.3
     */
    @Test
    void testRecordDifferentStateTransitions() {
        // When: 记录不同的状态转换
        mediaGroupMetrics.recordStateTransition("NONE", "COLLECTING");
        mediaGroupMetrics.recordStateTransition("COLLECTING", "PROCESSING");
        mediaGroupMetrics.recordStateTransition("PROCESSING", "COMPLETED");
        
        // Then: 验证每个状态转换都被单独记录
        Counter counter1 = meterRegistry.find("media_group.state.transition")
            .tag("from", "NONE")
            .tag("to", "COLLECTING")
            .counter();
        assertEquals(1.0, counter1.count(), "NONE -> COLLECTING 应该记录一次");
        
        Counter counter2 = meterRegistry.find("media_group.state.transition")
            .tag("from", "COLLECTING")
            .tag("to", "PROCESSING")
            .counter();
        assertEquals(1.0, counter2.count(), "COLLECTING -> PROCESSING 应该记录一次");
        
        Counter counter3 = meterRegistry.find("media_group.state.transition")
            .tag("from", "PROCESSING")
            .tag("to", "COMPLETED")
            .counter();
        assertEquals(1.0, counter3.count(), "PROCESSING -> COMPLETED 应该记录一次");
    }
    
    /**
     * 测试锁等待时间指标记录
     * 验证：需求 8.4
     */
    @Test
    void testRecordLockWaitTime() {
        // Given: 媒体组键和锁等待时间
        String groupKey = "123:456";
        long waitTime = 50L;
        
        // When: 记录锁等待时间
        mediaGroupMetrics.recordLockWaitTime(groupKey, waitTime);
        
        // Then: 验证 Timer 已记录
        Timer timer = meterRegistry.find("media_group.lock.wait")
            .tag("group_key", groupKey)
            .timer();
        
        assertNotNull(timer, "锁等待时间 Timer 应该被注册");
        assertEquals(1, timer.count(), "Timer 应该记录一次");
        assertEquals(waitTime, timer.totalTime(TimeUnit.MILLISECONDS), 0.01, 
            "Timer 应该记录正确的锁等待时间");
    }
    
    /**
     * 测试多次锁等待时间记录
     * 验证：需求 8.4
     */
    @Test
    void testRecordMultipleLockWaitTimes() {
        // Given: 同一个媒体组的多次锁等待
        String groupKey = "123:456";
        
        // When: 记录多次锁等待时间
        mediaGroupMetrics.recordLockWaitTime(groupKey, 10L);
        mediaGroupMetrics.recordLockWaitTime(groupKey, 20L);
        mediaGroupMetrics.recordLockWaitTime(groupKey, 30L);
        
        // Then: 验证 Timer 累计记录
        Timer timer = meterRegistry.find("media_group.lock.wait")
            .tag("group_key", groupKey)
            .timer();
        
        assertNotNull(timer, "锁等待时间 Timer 应该被注册");
        assertEquals(3, timer.count(), "Timer 应该记录三次");
        assertEquals(60.0, timer.totalTime(TimeUnit.MILLISECONDS), 0.01, 
            "Timer 应该累计所有锁等待时间");
    }
    
    /**
     * 测试数据库保存失败指标记录
     * 验证：需求 8.5
     */
    @Test
    void testRecordSaveFailure() {
        // Given: 失败原因
        String reason = "duplicate_key";
        
        // When: 记录数据库保存失败
        mediaGroupMetrics.recordSaveFailure(reason);
        
        // Then: 验证 Counter 已记录
        Counter counter = meterRegistry.find("message.save.failure")
            .tag("reason", reason)
            .counter();
        
        assertNotNull(counter, "数据库保存失败 Counter 应该被注册");
        assertEquals(1.0, counter.count(), "Counter 应该记录一次失败");
    }
    
    /**
     * 测试多次数据库保存失败记录
     * 验证：需求 8.5
     */
    @Test
    void testRecordMultipleSaveFailures() {
        // Given: 同一个失败原因
        String reason = "duplicate_key";
        
        // When: 记录多次数据库保存失败
        mediaGroupMetrics.recordSaveFailure(reason);
        mediaGroupMetrics.recordSaveFailure(reason);
        mediaGroupMetrics.recordSaveFailure(reason);
        
        // Then: 验证 Counter 累计记录
        Counter counter = meterRegistry.find("message.save.failure")
            .tag("reason", reason)
            .counter();
        
        assertNotNull(counter, "数据库保存失败 Counter 应该被注册");
        assertEquals(3.0, counter.count(), "Counter 应该累计记录三次失败");
    }
    
    /**
     * 测试不同失败原因分别记录
     * 验证：需求 8.5
     */
    @Test
    void testRecordDifferentSaveFailureReasons() {
        // When: 记录不同的失败原因
        mediaGroupMetrics.recordSaveFailure("duplicate_key");
        mediaGroupMetrics.recordSaveFailure("connection_timeout");
        mediaGroupMetrics.recordSaveFailure("serialization_error");
        
        // Then: 验证每个失败原因都被单独记录
        Counter counter1 = meterRegistry.find("message.save.failure")
            .tag("reason", "duplicate_key")
            .counter();
        assertEquals(1.0, counter1.count(), "duplicate_key 失败应该记录一次");
        
        Counter counter2 = meterRegistry.find("message.save.failure")
            .tag("reason", "connection_timeout")
            .counter();
        assertEquals(1.0, counter2.count(), "connection_timeout 失败应该记录一次");
        
        Counter counter3 = meterRegistry.find("message.save.failure")
            .tag("reason", "serialization_error")
            .counter();
        assertEquals(1.0, counter3.count(), "serialization_error 失败应该记录一次");
    }
    
    /**
     * 测试所有指标类型都被正确注册
     * 验证：需求 8.1, 8.2, 8.3, 8.4, 8.5
     */
    @Test
    void testAllMetricsAreRegistered() {
        // Given: 注册所有类型的指标
        AtomicInteger bufferSize = new AtomicInteger(10);
        AtomicInteger activeCount = new AtomicInteger(5);
        
        mediaGroupMetrics.registerBufferSizeGauge(bufferSize::get);
        mediaGroupMetrics.registerActiveMediaGroupCountGauge(activeCount::get);
        mediaGroupMetrics.recordProcessingDuration("123:456", 100L);
        mediaGroupMetrics.recordStateTransition("COLLECTING", "PROCESSING");
        mediaGroupMetrics.recordLockWaitTime("123:456", 50L);
        mediaGroupMetrics.recordSaveFailure("duplicate_key");
        
        // Then: 验证所有指标都存在
        assertNotNull(meterRegistry.find("media_group.buffer.size").gauge(), 
            "缓冲区大小 Gauge 应该存在");
        assertNotNull(meterRegistry.find("media_group.active.count").gauge(), 
            "活跃媒体组数量 Gauge 应该存在");
        assertNotNull(meterRegistry.find("media_group.processing.duration").timer(), 
            "处理延迟 Timer 应该存在");
        assertNotNull(meterRegistry.find("media_group.state.transition").counter(), 
            "状态转换 Counter 应该存在");
        assertNotNull(meterRegistry.find("media_group.lock.wait").timer(), 
            "锁等待时间 Timer 应该存在");
        assertNotNull(meterRegistry.find("message.save.failure").counter(), 
            "数据库保存失败 Counter 应该存在");
    }
}
