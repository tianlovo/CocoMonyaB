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
    @NotNull(message = "必须配置目标频道ID")
    @Negative(message = "目标频道ID必须为负数")
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
    
    /**
     * 欢迎消息模板
     * 用于验证目标频道的发送权限并提供系统状态信息
     * 支持占位符：
     * - {pluginName}: 插件名称
     * - {version}: 系统版本
     * - {tagPrefix}: 标签前缀
     * - {rateLimitPerMinute}: 每分钟转发速率限制
     * - {batchSize}: 批次大小
     * - {scheduleInterval}: 调度间隔（秒）
     * - {maxRetryCount}: 最大重试次数
     * <p>
     * 默认值: 多行欢迎消息模板
     */
    private String welcomeMessage = """
            🎊 CocoMonyaB 标签转发系统已启动
            
            📌 系统信息:
            • 项目名称: CocoMonyaB
            • 系统版本: v{version}
            • 插件名称: {pluginName}
            • 标签前缀: {tagPrefix}
            
            ⚙️ 转发配置:
            • 转发速率: {rateLimitPerMinute} 条/分钟
            • 批次大小: {batchSize} 条消息
            • 调度间隔: {scheduleInterval} 秒
            • 最大重试: {maxRetryCount} 次
            
            📝 功能说明:
            • 自动监控频道消息
            • 基于标签智能过滤
            • 多级审核机制
            • 结构化存储
            
            ✅ 目标频道权限验证通过
            ✅ 系统运行正常，准备接收消息
            
            💡 提示: 系统将自动转发匹配标签的消息到此频道
            """;
}
