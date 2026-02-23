package org.xlyo.cocomonyab.domain.dto;

import lombok.Data;

/**
 * 用于查询标签过滤配置的DTO
 * 定义查询过滤条件
 */
@Data
public class TagFilterConfigQueryDTO {
    
    /**
     * 按频道ID过滤
     */
    private Long channelId;
    
    /**
     * 按匹配模式过滤
     */
    private String matchMode;
    
    /**
     * 按启用状态过滤
     */
    private Boolean enabled;
}
