package org.xlyo.cocomonyab.source.unread.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 未读消息来源生成器配置类
 * 
 * 使用 @ConfigurationProperties 绑定配置，支持配置启动行为、API 调用参数、
 * 重试策略和批处理参数
 * 
 * 验证需求：11.1, 11.2, 11.3, 11.4, 11.5
 */
@Configuration
@ConfigurationProperties(prefix = "message-source.unread")
@Data
@Validated
public class UnreadMessageSourceConfig {
    
    /**
     * 启动配置：是否在程序启动时自动检测未读消息
     * 需求：11.1
     */
    @NotNull
    private Boolean autoDetectOnStartup = true;
    
    /**
     * API 调用配置：单次 API 调用最大消息数
     * 需求：11.2
     */
    @Min(1)
    @NotNull
    private Integer maxMessagesPerFetch = 100;
    
    /**
     * API 调用配置：单个频道最大获取消息数
     * 需求：11.2, 2.5
     */
    @Min(1)
    @NotNull
    private Integer maxTotalMessages = 1000;
    
    /**
     * API 调用配置：API 调用之间的延迟（毫秒）
     * 需求：11.4
     */
    @Min(0)
    @NotNull
    private Long apiCallDelay = 1000L;
    
    /**
     * 重试配置：最大重试次数
     * 需求：11.5
     */
    @Min(0)
    @NotNull
    private Integer maxRetries = 3;
    
    /**
     * 重试配置：基础重试延迟（毫秒）
     * 需求：11.5
     */
    @Min(0)
    @NotNull
    private Long retryBaseDelay = 2000L;
    
    /**
     * 重试配置：最大重试延迟（毫秒）
     * 需求：11.5
     */
    @Min(0)
    @NotNull
    private Long retryMaxDelay = 60000L;
    
    /**
     * 批处理配置：每批处理的消息数
     * 需求：11.3
     */
    @Min(1)
    @NotNull
    private Integer batchSize = 10;
    
    /**
     * 批处理配置：批次之间的延迟（毫秒）
     * 需求：11.3
     */
    @Min(0)
    @NotNull
    private Long batchDelay = 500L;
    
    /**
     * 缓冲区配置：缓冲区 TTL（天）
     * 需求：13.7
     */
    @Min(1)
    @NotNull
    private Integer bufferTtlDays = 7;
}
