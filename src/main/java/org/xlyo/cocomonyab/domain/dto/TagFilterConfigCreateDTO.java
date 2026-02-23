package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 用于创建标签过滤配置的DTO
 * 支持创建全局配置（channelId为null）和频道配置（channelId非null）
 */
@Data
public class TagFilterConfigCreateDTO {
    
    /**
     * Telegram频道ID
     * null表示创建全局配置，非null表示创建频道配置
     */
    private Long channelId;
    
    /**
     * 标签列表
     * 不能为null，但可以为空列表
     */
    @NotNull(message = "标签列表不能为null")
    private List<String> tags;
    
    /**
     * 匹配模式
     * 必须是 whitelist 或 blacklist
     */
    @NotNull(message = "匹配模式不能为空")
    @Pattern(regexp = "^(whitelist|blacklist)$", message = "匹配模式必须是whitelist或blacklist")
    private String matchMode;
    
    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为null")
    private Boolean enabled;
}
