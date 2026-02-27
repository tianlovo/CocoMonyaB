package org.xlyo.cocomonyab.plugin.channelmessage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 频道消息存储插件配置属性
 * 
 * <h2>配置前缀</h2>
 * <p>所有配置项使用 {@code plugin.channel-message-storage} 作为前缀。</p>
 * 
 * <h2>配置示例</h2>
 * <pre>
 * # application.yml
 * plugin:
 *   channel-message-storage:
 *     enabled: true
 * </pre>
 * 
 * @author CocoMonya Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin.channel-message-storage")
public class ChannelMessageStorageProperties {
    
    /**
     * 插件启用状态
     * 
     * <p><strong>配置项:</strong> {@code plugin.channel-message-storage.enabled}</p>
     * <p><strong>默认值:</strong> {@code true}</p>
     */
    private boolean enabled = true;
}
