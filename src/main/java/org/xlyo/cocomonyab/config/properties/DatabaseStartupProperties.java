package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据库启动配置属性
 * <p>
 * 从 application.yaml 的 app.startup.database 配置项读取数据库启动相关配置。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.startup.database")
public class DatabaseStartupProperties {
    
    /**
     * 最大重试次数
     * <p>
     * 数据库连接失败时的最大重试次数，默认为 3 次
     * </p>
     */
    private int maxRetries = 3;
    
    /**
     * 重试延迟（毫秒）
     * <p>
     * 每次重试之间的延迟时间，默认为 2000 毫秒（2秒）
     * </p>
     */
    private long retryDelayMs = 2000;
}
