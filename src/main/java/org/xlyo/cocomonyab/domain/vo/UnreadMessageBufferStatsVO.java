package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息缓冲区统计信息视图对象
 * 用于API响应的未读消息缓冲区统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadMessageBufferStatsVO {
    
    /**
     * 待处理数量
     */
    private Long pendingCount;
    
    /**
     * 已处理数量
     */
    private Long processedCount;
    
    /**
     * 失败数量
     */
    private Long failedCount;
    
    /**
     * 总数量
     */
    private Long totalCount;
}
