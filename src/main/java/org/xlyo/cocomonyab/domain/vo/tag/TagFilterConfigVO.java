package org.xlyo.cocomonyab.domain.vo.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 标签过滤配置视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagFilterConfigVO {
    /**
     * 配置ID
     */
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
     */
    private Map<String, String> customTags;
    
    /**
     * 匹配模式
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
