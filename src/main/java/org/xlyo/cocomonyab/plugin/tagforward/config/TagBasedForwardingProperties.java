package org.xlyo.cocomonyab.plugin.tagforward.config;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * 基于标签的消息转发插件的配置属性
 * 
 * <p>此类从application.yaml绑定前缀为"plugin.tag-based-forwarding"的配置属性，
 * 并提供对所有插件配置选项的类型安全访问
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "plugin.tag-based-forwarding")
public class TagBasedForwardingProperties {
    
    /**
     * 插件是否启用
     * 默认值: true
     */
    private Boolean enabled = true;
    
    /**
     * 转发消息的目标频道ID
     * 必须是负数（Telegram频道ID格式）
     * 这是必需的配置项
     */
    @NotNull(message = "Target channel ID must be configured")
    @Negative(message = "Target channel ID must be negative")
    private Long targetChannelId;
    
    /**
     * 添加到所有标签前的前缀
     * 默认值: "#"
     */
    private String tagPrefix = "#";
    
    /**
     * 每分钟允许的最大转发操作数
     * 用于频率限制以避免超出Telegram API限制
     * 默认值: 20
     */
    private Integer rateLimitPerMinute = 20;
    
    /**
     * 每批次处理的最大消息数
     * 默认值: 10
     */
    private Integer batchSize = 10;
    
    /**
     * 定时处理转发队列的间隔秒数
     * 默认值: 30
     */
    private Integer scheduleIntervalSeconds = 30;
    
    /**
     * 转发操作失败时的最大重试次数
     * 默认值: 3
     */
    private Integer maxRetryCount = 3;
}
