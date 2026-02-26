package org.xlyo.cocomonyab.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息来源健康状态
 * <p>
 * 用于监控消息来源的运行状态和性能指标
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSourceHealth {
    
    /**
     * 健康状态枚举
     */
    public enum Status {
        /** 健康 - 正常运行 */
        HEALTHY,
        
        /** 降级 - 部分功能受限但仍可用 */
        DEGRADED,
        
        /** 不健康 - 无法正常工作 */
        UNHEALTHY,
        
        /** 未知 - 无法确定状态 */
        UNKNOWN
    }
    
    /**
     * 健康状态
     */
    private Status status;
    
    /**
     * 状态描述
     */
    private String message;
    
    /**
     * 最后检查时间
     */
    private LocalDateTime lastCheckTime;
    
    /**
     * 启动时间
     */
    private LocalDateTime startTime;
    
    /**
     * 已接收消息总数
     */
    private Long totalMessagesReceived;
    
    /**
     * 已处理消息总数
     */
    private Long totalMessagesProcessed;
    
    /**
     * 失败消息总数
     */
    private Long totalMessagesFailed;
    
    /**
     * 最后一条消息接收时间
     */
    private LocalDateTime lastMessageTime;
    
    /**
     * 额外的健康指标
     * <p>
     * 可以存储特定于消息来源的自定义指标，如：
     * - "connection_status": "connected"
     * - "latency_ms": "150"
     * - "queue_size": "42"
     */
    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();
    
    /**
     * 创建一个健康状态
     * 
     * @param status 状态
     * @param message 消息
     * @return 健康状态对象
     */
    public static MessageSourceHealth of(Status status, String message) {
        return MessageSourceHealth.builder()
            .status(status)
            .message(message)
            .lastCheckTime(LocalDateTime.now())
            .build();
    }
    
    /**
     * 创建一个健康状态
     * 
     * @param status 状态
     * @return 健康状态对象
     */
    public static MessageSourceHealth healthy() {
        return of(Status.HEALTHY, "消息来源运行正常");
    }
    
    /**
     * 创建一个降级状态
     * 
     * @param message 消息
     * @return 健康状态对象
     */
    public static MessageSourceHealth degraded(String message) {
        return of(Status.DEGRADED, message);
    }
    
    /**
     * 创建一个不健康状态
     * 
     * @param message 消息
     * @return 健康状态对象
     */
    public static MessageSourceHealth unhealthy(String message) {
        return of(Status.UNHEALTHY, message);
    }
    
    /**
     * 创建一个未知状态
     * 
     * @return 健康状态对象
     */
    public static MessageSourceHealth unknown() {
        return of(Status.UNKNOWN, "无法确定消息来源状态");
    }
    
    /**
     * 添加自定义指标
     * 
     * @param key 指标名称
     * @param value 指标值
     * @return 当前对象（支持链式调用）
     */
    public MessageSourceHealth addMetric(String key, Object value) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }
        this.metrics.put(key, value);
        return this;
    }
}
