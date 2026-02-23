package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用于更新标签过滤配置的DTO
 * 所有字段都是可选的，只更新提供的字段
 */
@Data
public class TagFilterConfigUpdateDTO {
    
    /**
     * 标签列表
     * 可选字段
     */
    private List<String> tags;
    
    /**
     * 匹配模式
     * 如果提供，必须是 whitelist 或 blacklist
     */
    @Pattern(regexp = "^(whitelist|blacklist)$", message = "匹配模式必须是whitelist或blacklist")
    private String matchMode;
    
    /**
     * 是否启用
     * 可选字段
     */
    private Boolean enabled;
}
