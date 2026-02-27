package org.xlyo.cocomonyab.domain.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 消息查询DTO
 * 用于接收消息查询的过滤条件参数
 */
@Data
public class MessageQueryDTO {
    
    /**
     * 频道ID（可选）
     */
    private Long chatId;
    
    /**
     * 开始日期（Unix时间戳，可选）
     */
    @Min(value = 0, message = "开始日期必须大于等于0")
    private Integer startDate;
    
    /**
     * 结束日期（Unix时间戳，可选）
     */
    @Min(value = 0, message = "结束日期必须大于等于0")
    private Integer endDate;
    
    /**
     * 媒体组ID（可选）
     */
    private Long mediaAlbumId;
}
