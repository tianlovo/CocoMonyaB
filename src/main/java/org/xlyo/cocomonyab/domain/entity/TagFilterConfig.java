package org.xlyo.cocomonyab.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签过滤配置实体
 * 存储标签过滤规则，支持全局配置和频道级配置
 */
@Document(collection = "tag_filter_configs")
@Data
public class TagFilterConfig {
    
    @Id
    private String id;
    
    /**
     * Telegram频道ID
     * null表示全局配置，非null表示频道配置
     */
    @Indexed(unique = true, sparse = true)
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
