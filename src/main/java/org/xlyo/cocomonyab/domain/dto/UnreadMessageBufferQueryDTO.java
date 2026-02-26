package org.xlyo.cocomonyab.domain.dto;

import lombok.Data;

/**
 * 未读消息缓冲区查询DTO
 * 用于接收未读消息缓冲区查询的过滤条件参数
 */
@Data
public class UnreadMessageBufferQueryDTO {
    
    /**
     * 频道ID（可选）
     */
    private Long chatId;
    
    /**
     * 缓冲区状态（可选）
     * PENDING: 待处理
     * PROCESSED: 已处理
     * FAILED: 处理失败
     */
    private String status;
}
