package org.xlyo.cocomonyab.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 启动配置属性
 * <p>
 * 从 application.yaml 的 app.startup 配置项读取启动相关配置。
 * 支持通过环境变量覆盖配置值。
 * </p>
 * <p>
 * 验证需求：11.1, 11.2, 11.3, 11.4, 11.5
 * </p>
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.startup")
public class StartupConfigurationProperties {
    
    /**
     * 启动超时时间（分钟）
     * <p>
     * 应用启动的最大允许时间，超过此时间将终止启动。
     * 默认值：5 分钟
     * 最小值：1 分钟
     * </p>
     * <p>
     * 验证需求：11.1
     * </p>
     */
    @Min(value = 1, message = "启动超时时间必须至少为 1 分钟")
    private int timeoutMinutes = 5;
    
    /**
     * 数据库启动配置
     * <p>
     * 验证需求：11.3
     * </p>
     */
    @Valid
    private Database database = new Database();
    
    /**
     * 组件启用配置
     * <p>
     * 验证需求：11.2
     * </p>
     */
    @Valid
    private Components components = new Components();
    
    /**
     * 启动进度监控配置
     */
    @Valid
    private Progress progress = new Progress();
    
    /**
     * 数据库启动配置
     */
    @Data
    public static class Database {
        /**
         * 最大重试次数
         * <p>
         * 数据库连接失败时的最大重试次数。
         * 默认值：3 次
         * 最小值：0 次（不重试）
         * </p>
         */
        @Min(value = 0, message = "数据库最大重试次数不能为负数")
        private int maxRetries = 3;
        
        /**
         * 重试延迟（毫秒）
         * <p>
         * 每次重试之间的延迟时间。
         * 默认值：2000 毫秒（2秒）
         * 最小值：0 毫秒
         * </p>
         */
        @Min(value = 0, message = "数据库重试延迟不能为负数")
        private long retryDelayMs = 2000;
    }
    
    /**
     * 组件启用配置
     */
    @Data
    public static class Components {
        /**
         * 是否启用嵌入式 MongoDB
         * <p>
         * true: 启动嵌入式 MongoDB 实例
         * false: 使用远程 MongoDB 连接
         * 默认值：true
         * </p>
         */
        private boolean embeddedMongodb = true;
        
        /**
         * 是否启用未读消息检测
         * <p>
         * true: 启动时自动检测未读消息
         * false: 不检测未读消息
         * 默认值：true
         * </p>
         */
        private boolean unreadMessageDetection = true;
        
        /**
         * 是否启用 Telegram 客户端
         * <p>
         * true: 初始化并启动 Telegram 客户端
         * false: 不启动 Telegram 客户端（测试模式）
         * 默认值：true
         * </p>
         */
        private boolean telegramClient = true;
    }
    
    /**
     * 启动进度监控配置
     */
    @Data
    public static class Progress {
        /**
         * 是否启用日志输出
         * <p>
         * true: 在日志中输出启动进度信息
         * false: 不输出启动进度日志
         * 默认值：true
         * </p>
         */
        private boolean logEnabled = true;
        
        /**
         * 是否启用 JMX 监控
         * <p>
         * true: 通过 JMX 暴露启动进度信息
         * false: 不启用 JMX 监控
         * 默认值：false
         * </p>
         */
        private boolean jmxEnabled = false;
    }
}
