package org.xlyo.cocomonyab.source.unread.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 未读消息指标收集器
 * <p>
 * 使用 Micrometer 收集未读消息来源生成器的运行指标，包括：
 * <ul>
 *   <li>计数器：扫描的频道数、检测到的消息数、处理的消息数、失败的消息数、API 调用次数、速率限制错误次数</li>
 *   <li>计时器：检测耗时、API 调用耗时、消息处理耗时</li>
 *   <li>仪表：待处理缓冲区大小、失败缓冲区大小</li>
 * </ul>
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Component
@Slf4j
public class UnreadMessageMetrics {
    
    private final MeterRegistry registry;
    
    // 计数器
    private final Counter channelsScanned;
    private final Counter messagesDetected;
    private final Counter messagesProcessed;
    private final Counter messagesFailed;
    private final Counter apiCalls;
    private final Counter rateLimitErrors;
    
    // 计时器
    private final Timer detectionTimer;
    private final Timer apiCallTimer;
    private final Timer processingTimer;
    
    // 仪表
    private final AtomicInteger pendingBufferSize;
    private final AtomicInteger failedBufferSize;
    
    /**
     * 构造函数，初始化所有指标
     * 
     * @param registry Micrometer 指标注册表
     */
    public UnreadMessageMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // 初始化计数器
        this.channelsScanned = Counter.builder("unread_source.channels.scanned")
            .description("扫描的频道总数")
            .register(registry);
        
        this.messagesDetected = Counter.builder("unread_source.messages.detected")
            .description("检测到的未读消息总数")
            .register(registry);
        
        this.messagesProcessed = Counter.builder("unread_source.messages.processed")
            .description("成功处理的消息总数")
            .register(registry);
        
        this.messagesFailed = Counter.builder("unread_source.messages.failed")
            .description("处理失败的消息总数")
            .register(registry);
        
        this.apiCalls = Counter.builder("unread_source.api.calls")
            .description("API 调用总次数")
            .register(registry);
        
        this.rateLimitErrors = Counter.builder("unread_source.api.rate_limit_errors")
            .description("速率限制错误总次数")
            .register(registry);
        
        // 初始化计时器
        this.detectionTimer = Timer.builder("unread_source.detection.duration")
            .description("检测未读消息的耗时")
            .register(registry);
        
        this.apiCallTimer = Timer.builder("unread_source.api.duration")
            .description("API 调用的耗时")
            .register(registry);
        
        this.processingTimer = Timer.builder("unread_source.processing.duration")
            .description("消息处理的耗时")
            .register(registry);
        
        // 初始化仪表
        this.pendingBufferSize = registry.gauge(
            "unread_source.buffer.pending",
            new AtomicInteger(0)
        );
        
        this.failedBufferSize = registry.gauge(
            "unread_source.buffer.failed",
            new AtomicInteger(0)
        );
        
        log.info("未读消息指标收集器初始化完成");
    }
    
    /**
     * 记录扫描的频道数
     */
    public void recordChannelScanned() {
        channelsScanned.increment();
    }
    
    /**
     * 记录检测到的消息数
     * 
     * @param count 消息数量
     */
    public void recordMessagesDetected(int count) {
        messagesDetected.increment(count);
    }
    
    /**
     * 记录成功处理的消息
     */
    public void recordMessageProcessed() {
        messagesProcessed.increment();
    }
    
    /**
     * 记录失败的消息
     */
    public void recordMessageFailed() {
        messagesFailed.increment();
    }
    
    /**
     * 记录 API 调用
     */
    public void recordApiCall() {
        apiCalls.increment();
    }
    
    /**
     * 记录速率限制错误
     */
    public void recordRateLimitError() {
        rateLimitErrors.increment();
    }
    
    /**
     * 计时检测操作
     * 
     * @param operation 要计时的操作
     * @param <T> 操作返回类型
     * @return 操作结果
     */
    public <T> T timeDetection(Supplier<T> operation) {
        return detectionTimer.record(operation);
    }
    
    /**
     * 计时 API 调用操作
     * 
     * @param operation 要计时的操作
     * @param <T> 操作返回类型
     * @return 操作结果
     */
    public <T> T timeApiCall(Supplier<T> operation) {
        return apiCallTimer.record(operation);
    }
    
    /**
     * 计时消息处理操作
     * 
     * @param operation 要计时的操作
     * @param <T> 操作返回类型
     * @return 操作结果
     */
    public <T> T timeProcessing(Supplier<T> operation) {
        return processingTimer.record(operation);
    }
    
    /**
     * 更新缓冲区大小指标
     * 
     * @param pending 待处理消息数量
     * @param failed 失败消息数量
     */
    public void updateBufferSizes(int pending, int failed) {
        pendingBufferSize.set(pending);
        failedBufferSize.set(failed);
    }
}
