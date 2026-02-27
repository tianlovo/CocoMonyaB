package org.xlyo.cocomonyab.domain.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 标签过滤配置创建DTO
 */
@Data
public class TagFilterConfigCreateDTO {
    /**
     * 作者标签ID列表
     */
    @NotNull(message = "作者标签列表不能为null")
    private List<String> authorIds;
    
    /**
     * 角色标签ID列表
     */
    @NotNull(message = "角色标签列表不能为null")
    private List<String> characterIds;
    
    /**
     * 原作标签ID列表
     */
    @NotNull(message = "原作标签列表不能为null")
    private List<String> workIds;
    
    /**
     * 自定义标签映射
     * key: 自定义标签ID, value: 标签字符串
     */
    @NotNull(message = "自定义标签列表不能为null")
    private Map<String, String> customTags;
    
    /**
     * 匹配模式：whitelist 或 blacklist
     */
    @NotBlank(message = "匹配模式不能为空")
    @Pattern(regexp = "whitelist|blacklist", message = "匹配模式必须是whitelist或blacklist")
    private String matchMode;
    
    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为null")
    private Boolean enabled;
}
