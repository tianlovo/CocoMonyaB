package org.xlyo.cocomonyab.domain.dto;

import lombok.Data;

/**
 * 转发队列查询DTO
 * 用于接收转发队列查询的过滤条件参数
 */
@Data
public class ForwardQueueQueryDTO {
    
    /**
     * 源频道ID（可选）
     */
    private Long sourceChatId;
    
    /**
     * 转发状态（可选）
     * PENDING: 待处理
     * SUCCESS: 转发成功
     * FAILED: 转发失败
     */
    private String status;
}
