package org.xlyo.cocomonyab.plugin.impl.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket消息广播插件配置属性
 * 
 * <p>该配置类用于管理WebSocket消息广播插件的所有配置项。
 * 通过Spring Boot的{@code @ConfigurationProperties}机制，
 * 可以在application.yml或application.properties文件中配置这些属性。</p>
 * 
 * <h2>配置前缀</h2>
 * <p>所有配置项使用 {@code plugin.websocket-broadcast} 作为前缀。</p>
 * 
 * <h2>配置示例</h2>
 * <pre>
 * # application.yml
 * plugin:
 *   websocket-broadcast:
 *     enabled: true
 *     topic-prefix: /topic/channel/real
 *     monitoring-topic-prefix: /topic/channel/monitoring
 *     async-broadcast: false
 * </pre>
 * 
 * <h2>默认值</h2>
 * <ul>
 *   <li>{@code enabled}: true（插件默认启用）</li>
 *   <li>{@code topicPrefix}: "/topic/channel/real"</li>
 *   <li>{@code monitoringTopicPrefix}: "/topic/channel/monitoring"</li>
 *   <li>{@code asyncBroadcast}: false（默认同步广播）</li>
 * </ul>
 * 
 * <h2>配置说明</h2>
 * <ul>
 *   <li><strong>enabled</strong>: 控制插件是否启用，设置为false可以禁用插件</li>
 *   <li><strong>topicPrefix</strong>: 消息广播的topic前缀，客户端订阅时需要使用此前缀</li>
 *   <li><strong>monitoringTopicPrefix</strong>: 监控事件的topic前缀</li>
 *   <li><strong>asyncBroadcast</strong>: 是否启用异步广播（实验性功能）</li>
 * </ul>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "plugin.websocket-broadcast")
public class WebSocketBroadcastProperties {
    
    /**
     * 是否启用插件
     * 
     * <p>控制WebSocket消息广播插件是否启用。</p>
     * <ul>
     *   <li>{@code true}: 插件启用，会广播消息到WebSocket</li>
     *   <li>{@code false}: 插件禁用，不会广播消息</li>
     * </ul>
     * 
     * <p><strong>配置项:</strong> {@code plugin.websocket-broadcast.enabled}</p>
     * <p><strong>默认值:</strong> {@code true}</p>
     */
    private boolean enabled = true;
    
    /**
     * 消息广播topic前缀
     * 
     * <p>用于构建消息广播的WebSocket topic路径。
     * 完整的topic格式为: {@code {topicPrefix}/{channelId}}</p>
     * 
     * <p><strong>示例:</strong></p>
     * <ul>
     *   <li>topicPrefix = "/topic/channel/real"</li>
     *   <li>channelId = -1001234567890</li>
     *   <li>完整topic = "/topic/channel/real/-1001234567890"</li>
     * </ul>
     * 
     * <p><strong>配置项:</strong> {@code plugin.websocket-broadcast.topic-prefix}</p>
     * <p><strong>默认值:</strong> {@code "/topic/channel/real"}</p>
     */
    private String topicPrefix = "/topic/channel/real";
    
    /**
     * 监控事件topic前缀
     * 
     * <p>用于构建频道监控事件的WebSocket topic路径。
     * 完整的topic格式为: {@code {monitoringTopicPrefix}/{eventType}}</p>
     * 
     * <p><strong>示例:</strong></p>
     * <ul>
     *   <li>monitoringTopicPrefix = "/topic/channel/monitoring"</li>
     *   <li>eventType = "added"</li>
     *   <li>完整topic = "/topic/channel/monitoring/added"</li>
     * </ul>
     * 
     * <p><strong>支持的事件类型:</strong></p>
     * <ul>
     *   <li>added: 频道被添加到监控列表</li>
     *   <li>removed: 频道从监控列表移除</li>
     *   <li>updated: 频道监控状态更新</li>
     *   <li>reload: 重新加载所有频道</li>
     * </ul>
     * 
     * <p><strong>配置项:</strong> {@code plugin.websocket-broadcast.monitoring-topic-prefix}</p>
     * <p><strong>默认值:</strong> {@code "/topic/channel/monitoring"}</p>
     */
    private String monitoringTopicPrefix = "/topic/channel/monitoring";
    
    /**
     * 是否启用异步广播
     * 
     * <p>控制消息广播是否使用异步方式。</p>
     * <ul>
     *   <li>{@code true}: 使用异步方式广播，不阻塞消息处理流程</li>
     *   <li>{@code false}: 使用同步方式广播，等待广播完成后继续处理</li>
     * </ul>
     * 
     * <p><strong>注意:</strong> 异步广播是实验性功能，可能需要额外的线程池配置。</p>
     * 
     * <p><strong>配置项:</strong> {@code plugin.websocket-broadcast.async-broadcast}</p>
     * <p><strong>默认值:</strong> {@code false}</p>
     */
    private boolean asyncBroadcast = false;
}
