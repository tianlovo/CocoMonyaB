package org.xlyo.cocomonyab.domain.entity.tag;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 标签过滤配置实体（新版）
 * 全局配置，按标签类型分类存储
 */
@Data
@Document(collection = "tag_filter_configs_v2")
public class TagFilterConfig {
    /**
     * 配置ID
     */
    @Id
    private String id;
    
    /**
     * 作者标签ID列表
     */
    private List<String> authorIds;
    
    /**
     * 角色标签ID列表
     */
    private List<String> characterIds;
    
    /**
     * 原作标签ID列表
     */
    private List<String> workIds;
    
    /**
     * 自定义标签映射
     * key: 自定义标签ID, value: 标签字符串
     */
    private Map<String, String> customTags;
    
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
