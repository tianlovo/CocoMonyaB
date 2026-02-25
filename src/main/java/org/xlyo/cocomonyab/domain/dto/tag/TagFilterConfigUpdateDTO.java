package org.xlyo.cocomonyab.domain.dto.tag;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 标签过滤配置更新DTO
 */
@Data
public class TagFilterConfigUpdateDTO {
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
    @Pattern(regexp = "whitelist|blacklist", message = "匹配模式必须是whitelist或blacklist")
    private String matchMode;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
}
