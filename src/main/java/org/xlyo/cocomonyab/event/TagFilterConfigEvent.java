package org.xlyo.cocomonyab.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 标签过滤配置事件
 * 当标签过滤配置发生变化时发布此事件，通知相关组件更新缓存
 */
@Getter
public class TagFilterConfigEvent extends ApplicationEvent {
    
    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 配置被创建
         */
        CONFIG_CREATED,
        
        /**
         * 配置被更新
         */
        CONFIG_UPDATED,
        
        /**
         * 配置被删除
         */
        CONFIG_DELETED,
        
        /**
         * 重新加载所有配置
         */
        RELOAD_ALL
    }
    
    /**
     * 事件类型
     */
    private final EventType eventType;
    
    /**
     * 频道ID（null表示全局配置，对于RELOAD_ALL类型也可以为null）
     */
    private final Long channelId;
    
    /**
     * MongoDB文档ID
     */
    private final String configId;
    
    /**
     * 配置的启用状态（对于CONFIG_DELETED和RELOAD_ALL类型可以为null）
     */
    private final Boolean enabled;
    
    /**
     * 创建标签过滤配置事件
     *
     * @param source 事件源
     * @param eventType 事件类型
     * @param channelId 频道ID
     * @param configId MongoDB文档ID
     * @param enabled 配置的启用状态
     */
    public TagFilterConfigEvent(Object source, EventType eventType, 
                                Long channelId, String configId, Boolean enabled) {
        super(source);
        this.eventType = eventType;
        this.channelId = channelId;
        this.configId = configId;
        this.enabled = enabled;
    }
    
    /**
     * 创建配置创建事件
     */
    public static TagFilterConfigEvent configCreated(Object source, Long channelId, 
                                                     String configId, Boolean enabled) {
        return new TagFilterConfigEvent(source, EventType.CONFIG_CREATED, channelId, configId, enabled);
    }
    
    /**
     * 创建配置更新事件
     */
    public static TagFilterConfigEvent configUpdated(Object source, Long channelId, 
                                                     String configId, Boolean enabled) {
        return new TagFilterConfigEvent(source, EventType.CONFIG_UPDATED, channelId, configId, enabled);
    }
    
    /**
     * 创建配置删除事件
     */
    public static TagFilterConfigEvent configDeleted(Object source, Long channelId, String configId) {
        return new TagFilterConfigEvent(source, EventType.CONFIG_DELETED, channelId, configId, null);
    }
    
    /**
     * 创建重新加载事件
     */
    public static TagFilterConfigEvent reloadAll(Object source) {
        return new TagFilterConfigEvent(source, EventType.RELOAD_ALL, null, null, null);
    }
    
    @Override
    public String toString() {
        return String.format("TagFilterConfigEvent{type=%s, channelId=%s, configId=%s, enabled=%s}", 
            eventType, channelId, configId, enabled);
    }
}
