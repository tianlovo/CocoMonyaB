package org.xlyo.cocomonyab.plugin.tagforward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

/**
 * 标签过滤配置数据模型
 * <p>
 * 从tag_filter_configs_v2集合读取，包含启用的标签配置信息
 */
@Document(collection = "tag_filter_configs_v2")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagFilterConfig {
    
    /**
     * 配置ID
     */
    @Id
    private String id;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 作者ID列表
     */
    private List<String> authorIds;
    
    /**
     * 角色ID列表
     */
    private List<String> characterIds;
    
    /**
     * 原作ID列表
     */
    private List<String> workIds;
    
    /**
     * 自定义标签映射 (key -> value)
     */
    private Map<String, String> customTags;
}
