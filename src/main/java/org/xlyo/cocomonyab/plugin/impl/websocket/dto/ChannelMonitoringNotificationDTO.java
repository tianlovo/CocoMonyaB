package org.xlyo.cocomonyab.plugin.impl.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 频道监控通知数据传输对象
 * 
 * <p>该DTO用于通知客户端频道监控列表的变化。
 * 当系统添加、移除或更新监控频道时，会通过WebSocket广播此通知。</p>
 * 
 * <h2>事件类型</h2>
 * <ul>
 *   <li>{@code ADDED}: 频道被添加到监控列表</li>
 *   <li>{@code REMOVED}: 频道从监控列表移除</li>
 *   <li>{@code UPDATED}: 频道监控状态更新</li>
 *   <li>{@code RELOAD_ALL}: 重新加载所有频道</li>
 * </ul>
 * 
 * <h2>Topic订阅</h2>
 * <p>客户端需要订阅以下topic来接收监控事件通知：</p>
 * <ul>
 *   <li>{@code /topic/channel/monitoring/added}: 频道添加事件</li>
 *   <li>{@code /topic/channel/monitoring/removed}: 频道移除事件</li>
 *   <li>{@code /topic/channel/monitoring/updated}: 频道更新事件</li>
 *   <li>{@code /topic/channel/monitoring/reload}: 重新加载事件</li>
 * </ul>
 * 
 * <h2>使用示例</h2>
 * <pre>
 * // 创建频道添加通知
 * ChannelMonitoringNotificationDTO notification = ChannelMonitoringNotificationDTO.builder()
 *     .eventType("ADDED")
 *     .channelId(-1001234567890L)
 *     .monitoringStatus(true)
 *     .timestamp(System.currentTimeMillis())
 *     .build();
 * </pre>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMonitoringNotificationDTO {
    
    /**
     * 事件类型
     * <p>监控事件的类型，对应ChannelMonitoringEvent.EventType枚举。</p>
     * <p>可能的值: ADDED, REMOVED, UPDATED, RELOAD_ALL</p>
     */
    private String eventType;
    
    /**
     * 频道ID
     * <p>受影响的频道ID（Telegram chat ID）。</p>
     * <p>示例: {@code -1001234567890}</p>
     */
    private Long channelId;
    
    /**
     * 监控状态
     * <p>频道的监控状态。</p>
     * <ul>
     *   <li>{@code true}: 频道正在被监控</li>
     *   <li>{@code false}: 频道未被监控</li>
     * </ul>
     */
    private Boolean monitoringStatus;
    
    /**
     * 时间戳
     * <p>事件发生的时间戳（毫秒）。</p>
     * <p>使用 {@code System.currentTimeMillis()} 生成。</p>
     */
    private Long timestamp;
}
