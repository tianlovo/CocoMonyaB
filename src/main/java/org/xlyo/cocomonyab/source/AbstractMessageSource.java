package org.xlyo.cocomonyab.source;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象消息来源基类
 * <p>
 * 提供消息来源的通用实现，包括：
 * - 运行状态管理
 * - 消息统计
 * - 健康状态监控
 * - 错误处理
 * <p>
 * 子类只需实现 doStart() 和 doStop() 方法即可
 * <p>
 * <b>注意：</b>start()、stop()、isRunning() 方法不使用 final 修饰符，
 * 以允许 Spring AOP 进行代理。如果使用 final，会导致 CGLIB 代理警告。
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Slf4j
@Getter
public abstract class AbstractMessageSource implements MessageSource {
    
    /**
     * 运行状态标志
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 启动时间
     */
    private volatile LocalDateTime startTime;
    
    /**
     * 最后一条消息接收时间
     */
    private volatile LocalDateTime lastMessageTime;
    
    /**
     * 已接收消息总数
     */
    private final AtomicLong totalMessagesReceived = new AtomicLong(0);
    
    /**
     * 已处理消息总数
     */
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    
    /**
     * 失败消息总数
     */
    private final AtomicLong totalMessagesFailed = new AtomicLong(0);
    
    /**
     * 最后一次错误消息
     */
    private volatile String lastError;
    
    /**
     * 最后一次错误时间
     */
    private volatile LocalDateTime lastErrorTime;
    
    @Override
    public void start() throws MessageSourceException {
        if (running.get()) {
            log.warn("消息来源已在运行: sourceId={}", getSourceId());
            return;
        }
        
        log.info("启动消息来源: sourceId={}, name={}", getSourceId(), getSourceName());
        
        try {
            doStart();
            running.set(true);
            startTime = LocalDateTime.now();
            log.info("消息来源启动成功: sourceId={}", getSourceId());
        } catch (Exception e) {
            log.error("消息来源启动失败: sourceId={}", getSourceId(), e);
            recordError("启动失败: " + e.getMessage());
            throw new MessageSourceException("启动消息来源失败", e);
        }
    }
    
    @Override
    public void stop() throws MessageSourceException {
        if (!running.get()) {
            log.warn("消息来源未运行: sourceId={}", getSourceId());
            return;
        }
        
        log.info("停止消息来源: sourceId={}, name={}", getSourceId(), getSourceName());
        
        try {
            doStop();
            running.set(false);
            log.info("消息来源停止成功: sourceId={}", getSourceId());
        } catch (Exception e) {
            log.error("消息来源停止失败: sourceId={}", getSourceId(), e);
            recordError("停止失败: " + e.getMessage());
            throw new MessageSourceException("停止消息来源失败", e);
        }
    }
    
    @Override
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public MessageSourceHealth getHealth() {
        MessageSourceHealth.Status status;
        String message;
        
        if (!running.get()) {
            status = MessageSourceHealth.Status.UNHEALTHY;
            message = "消息来源未运行";
        } else if (lastError != null) {
            status = MessageSourceHealth.Status.DEGRADED;
            message = "最近发生错误: " + lastError;
        } else {
            status = MessageSourceHealth.Status.HEALTHY;
            message = "消息来源运行正常";
        }
        
        MessageSourceHealth health = MessageSourceHealth.builder()
            .status(status)
            .message(message)
            .lastCheckTime(LocalDateTime.now())
            .startTime(startTime)
            .totalMessagesReceived(totalMessagesReceived.get())
            .totalMessagesProcessed(totalMessagesProcessed.get())
            .totalMessagesFailed(totalMessagesFailed.get())
            .lastMessageTime(lastMessageTime)
            .build();
        
        // 添加自定义指标
        if (lastError != null) {
            health.addMetric("last_error", lastError);
            health.addMetric("last_error_time", lastErrorTime);
        }
        
        // 子类可以添加额外的健康指标
        addCustomHealthMetrics(health);
        
        return health;
    }
    
    /**
     * 启动消息来源的具体实现
     * <p>
     * 子类需要实现此方法来初始化消息来源
     * 
     * @throws Exception 如果启动失败
     */
    protected abstract void doStart() throws Exception;
    
    /**
     * 停止消息来源的具体实现
     * <p>
     * 子类需要实现此方法来清理资源
     * 
     * @throws Exception 如果停止失败
     */
    protected abstract void doStop() throws Exception;
    
    /**
     * 添加自定义健康指标
     * <p>
     * 子类可以重写此方法来添加特定于消息来源的健康指标
     * 
     * @param health 健康状态对象
     */
    protected void addCustomHealthMetrics(MessageSourceHealth health) {
        // 默认不添加额外指标
    }
    
    /**
     * 记录接收到的消息
     * <p>
     * 子类在接收到消息时应调用此方法
     */
    protected void recordMessageReceived() {
        totalMessagesReceived.incrementAndGet();
        lastMessageTime = LocalDateTime.now();
    }
    
    /**
     * 记录处理成功的消息
     * <p>
     * 子类在成功处理消息时应调用此方法
     */
    protected void recordMessageProcessed() {
        totalMessagesProcessed.incrementAndGet();
    }
    
    /**
     * 记录处理失败的消息
     * <p>
     * 子类在处理消息失败时应调用此方法
     * 
     * @param errorMessage 错误消息
     */
    protected void recordMessageFailed(String errorMessage) {
        totalMessagesFailed.incrementAndGet();
        recordError(errorMessage);
    }
    
    /**
     * 记录错误
     * 
     * @param errorMessage 错误消息
     */
    protected void recordError(String errorMessage) {
        this.lastError = errorMessage;
        this.lastErrorTime = LocalDateTime.now();
    }
    
    /**
     * 清除错误状态
     * <p>
     * 子类可以在错误恢复后调用此方法
     */
    protected void clearError() {
        this.lastError = null;
        this.lastErrorTime = null;
    }
}
