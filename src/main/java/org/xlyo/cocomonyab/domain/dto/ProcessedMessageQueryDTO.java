package org.xlyo.cocomonyab.domain.dto;

import lombok.Data;

/**
 * 已处理消息查询DTO
 * 用于接收已处理消息查询的过滤条件参数
 */
@Data
public class ProcessedMessageQueryDTO {
    
    /**
     * 频道ID（可选）
     */
    private Long chatId;
    
    /**
     * 是否已读（可选）
     * true: 已标记为已读
     * false: 未标记为已读
     */
    private Boolean isRead;
    
    /**
     * 是否匹配标签（可选）
     * true: 匹配到标签
     * false: 未匹配到标签
     */
    private Boolean isMatched;
}
