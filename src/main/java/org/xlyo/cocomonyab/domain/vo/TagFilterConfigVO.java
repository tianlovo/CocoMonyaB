package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用于API响应的标签过滤配置视图对象
 * 包含返回给客户端的所有配置信息
 */
@Data
public class TagFilterConfigVO {
    
    /**
     * MongoDB文档ID
     */
    private String id;
    
    /**
     * Telegram频道ID
     * null表示全局配置，非null表示频道配置
     */
    private Long channelId;
    
    /**
     * 标签列表
     */
    private List<String> tags;
    
    /**
     * 匹配模式：whitelist 或 blacklist
     */
    private String matchMode;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
