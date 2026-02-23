package org.xlyo.cocomonyab.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 媒体组监控指标组件
 * 负责记录媒体组处理相关的监控指标
 */
@Slf4j
@Component
public class MediaGroupMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public MediaGroupMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("MediaGroupMetrics initialized with MeterRegistry: {}", meterRegistry.getClass().getSimpleName());
    }
    
    /**
     * 记录媒体组缓冲区大小指标
     * 使用 Gauge 实时反映当前缓冲区大小
     * 
     * @param sizeSupplier 提供缓冲区大小的供应商
     */
    public void registerBufferSizeGauge(Supplier<Number> sizeSupplier) {
        Gauge.builder("media_group.buffer.size", sizeSupplier)
            .description("媒体组缓冲区大小")
            .baseUnit("messages")
            .register(meterRegistry);
        
        log.debug("已注册媒体组缓冲区大小指标");
    }
    
    /**
     * 记录活跃媒体组数量指标
     * 使用 Gauge 实时反映当前活跃的媒体组数量
     * 
     * @param countSupplier 提供活跃媒体组数量的供应商
     */
    public void registerActiveMediaGroupCountGauge(Supplier<Number> countSupplier) {
        Gauge.builder("media_group.active.count", countSupplier)
            .description("活跃媒体组数量")
            .baseUnit("groups")
            .register(meterRegistry);
        
        log.debug("已注册活跃媒体组数量指标");
    }
    
    /**
     * 记录媒体组处理延迟
     * 使用 Timer 记录处理时间
     * 
     * @param groupKey 媒体组键
     * @param duration 处理时长（毫秒）
     */
    public void recordProcessingDuration(String groupKey, long duration) {
        Timer.builder("media_group.processing.duration")
            .tag("group_key", groupKey)
            .description("媒体组处理延迟")
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
        
        log.debug("记录媒体组处理延迟: groupKey={}, duration={}ms", groupKey, duration);
    }
    
    /**
     * 记录状态转换次数
     * 使用 Counter 累计状态转换次数
     * 
     * @param from 源状态
     * @param to 目标状态
     */
    public void recordStateTransition(String from, String to) {
        Counter.builder("media_group.state.transition")
            .tag("from", from)
            .tag("to", to)
            .description("状态转换次数")
            .register(meterRegistry)
            .increment();
        
        log.debug("记录状态转换: {} -> {}", from, to);
    }
    
    /**
     * 记录锁等待时间
     * 使用 Timer 记录锁获取等待时间
     * 
     * @param groupKey 媒体组键
     * @param waitTime 等待时长（毫秒）
     */
    public void recordLockWaitTime(String groupKey, long waitTime) {
        Timer.builder("media_group.lock.wait")
            .tag("group_key", groupKey)
            .description("锁等待时间")
            .register(meterRegistry)
            .record(waitTime, TimeUnit.MILLISECONDS);
        
        log.debug("记录锁等待时间: groupKey={}, waitTime={}ms", groupKey, waitTime);
    }
    
    /**
     * 记录数据库保存失败次数
     * 使用 Counter 累计失败次数
     * 
     * @param reason 失败原因
     */
    public void recordSaveFailure(String reason) {
        Counter.builder("message.save.failure")
            .tag("reason", reason)
            .description("数据库保存失败次数")
            .register(meterRegistry)
            .increment();
        
        log.debug("记录数据库保存失败: reason={}", reason);
    }
}
