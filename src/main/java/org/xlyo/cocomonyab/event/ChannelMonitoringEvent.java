package org.xlyo.cocomonyab.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 频道监控事件
 * 当频道监控配置发生变化时发布此事件，通知相关组件更新缓存
 */
@Getter
public class ChannelMonitoringEvent extends ApplicationEvent {
    
    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 频道被添加到监控列表
         */
        CHANNEL_ADDED,
        
        /**
         * 频道从监控列表中移除
         */
        CHANNEL_REMOVED,
        
        /**
         * 频道监控状态被更新
         */
        CHANNEL_UPDATED,
        
        /**
         * 需要重新加载所有监控频道
         */
        RELOAD_ALL
    }
    
    /**
     * 事件类型
     */
    private final EventType eventType;
    
    /**
     * 频道ID（对于 RELOAD_ALL 类型可以为 null）
     */
    private final Long channelId;
    
    /**
     * 监控状态（对于 CHANNEL_UPDATED 类型有效）
     */
    private final Boolean monitoringStatus;
    
    /**
     * 创建频道监控事件
     *
     * @param source 事件源
     * @param eventType 事件类型
     * @param channelId 频道ID
     * @param monitoringStatus 监控状态
     */
    public ChannelMonitoringEvent(Object source, EventType eventType, Long channelId, Boolean monitoringStatus) {
        super(source);
        this.eventType = eventType;
        this.channelId = channelId;
        this.monitoringStatus = monitoringStatus;
    }
    
    /**
     * 创建频道添加事件
     */
    public static ChannelMonitoringEvent channelAdded(Object source, Long channelId, Boolean monitoringStatus) {
        return new ChannelMonitoringEvent(source, EventType.CHANNEL_ADDED, channelId, monitoringStatus);
    }
    
    /**
     * 创建频道移除事件
     */
    public static ChannelMonitoringEvent channelRemoved(Object source, Long channelId) {
        return new ChannelMonitoringEvent(source, EventType.CHANNEL_REMOVED, channelId, null);
    }
    
    /**
     * 创建频道更新事件
     */
    public static ChannelMonitoringEvent channelUpdated(Object source, Long channelId, Boolean monitoringStatus) {
        return new ChannelMonitoringEvent(source, EventType.CHANNEL_UPDATED, channelId, monitoringStatus);
    }
    
    /**
     * 创建重新加载事件
     */
    public static ChannelMonitoringEvent reloadAll(Object source) {
        return new ChannelMonitoringEvent(source, EventType.RELOAD_ALL, null, null);
    }
    
    @Override
    public String toString() {
        return String.format("ChannelMonitoringEvent{type=%s, channelId=%d, monitoringStatus=%s}", 
            eventType, channelId, monitoringStatus);
    }
}
