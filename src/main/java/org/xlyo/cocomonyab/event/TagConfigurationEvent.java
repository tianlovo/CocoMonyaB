package org.xlyo.cocomonyab.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 标签配置事件
 * 当标签相关配置发生变化时发布此事件，通知相关组件更新缓存
 * <p>
 * 触发场景：
 * <ul>
 *   <li>tag_filter_configs_v2 集合变更（标签过滤配置）</li>
 *   <li>authors 集合变更（作者标签）</li>
 *   <li>characters 集合变更（角色标签）</li>
 *   <li>works 集合变更（作品标签）</li>
 * </ul>
 */
@Getter
public class TagConfigurationEvent extends ApplicationEvent {
    
    /**
     * 事件类型
     */
    public enum EventType {
        /**
         * 标签过滤配置被添加
         */
        TAG_FILTER_ADDED,
        
        /**
         * 标签过滤配置被移除
         */
        TAG_FILTER_REMOVED,
        
        /**
         * 标签过滤配置被更新
         */
        TAG_FILTER_UPDATED,
        
        /**
         * 作者标签被添加或更新
         */
        AUTHOR_CHANGED,
        
        /**
         * 角色标签被添加或更新
         */
        CHARACTER_CHANGED,
        
        /**
         * 作品标签被添加或更新
         */
        WORK_CHANGED,
        
        /**
         * 需要重新加载所有标签配置
         */
        RELOAD_ALL
    }
    
    /**
     * 事件类型
     */
    private final EventType eventType;
    
    /**
     * 实体ID（对于 RELOAD_ALL 类型可以为 null）
     */
    private final String entityId;
    
    /**
     * 创建标签配置事件
     *
     * @param source 事件源
     * @param eventType 事件类型
     * @param entityId 实体ID
     */
    public TagConfigurationEvent(Object source, EventType eventType, String entityId) {
        super(source);
        this.eventType = eventType;
        this.entityId = entityId;
    }
    
    /**
     * 创建标签过滤配置添加事件
     */
    public static TagConfigurationEvent tagFilterAdded(Object source, String filterId) {
        return new TagConfigurationEvent(source, EventType.TAG_FILTER_ADDED, filterId);
    }
    
    /**
     * 创建标签过滤配置移除事件
     */
    public static TagConfigurationEvent tagFilterRemoved(Object source, String filterId) {
        return new TagConfigurationEvent(source, EventType.TAG_FILTER_REMOVED, filterId);
    }
    
    /**
     * 创建标签过滤配置更新事件
     */
    public static TagConfigurationEvent tagFilterUpdated(Object source, String filterId) {
        return new TagConfigurationEvent(source, EventType.TAG_FILTER_UPDATED, filterId);
    }
    
    /**
     * 创建作者标签变更事件
     */
    public static TagConfigurationEvent authorChanged(Object source, String authorId) {
        return new TagConfigurationEvent(source, EventType.AUTHOR_CHANGED, authorId);
    }
    
    /**
     * 创建角色标签变更事件
     */
    public static TagConfigurationEvent characterChanged(Object source, String characterId) {
        return new TagConfigurationEvent(source, EventType.CHARACTER_CHANGED, characterId);
    }
    
    /**
     * 创建作品标签变更事件
     */
    public static TagConfigurationEvent workChanged(Object source, String workId) {
        return new TagConfigurationEvent(source, EventType.WORK_CHANGED, workId);
    }
    
    /**
     * 创建重新加载事件
     */
    public static TagConfigurationEvent reloadAll(Object source) {
        return new TagConfigurationEvent(source, EventType.RELOAD_ALL, null);
    }
    
    @Override
    public String toString() {
        return String.format("TagConfigurationEvent{type=%s, entityId=%s}", 
            eventType, entityId);
    }
}
