package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转发队列统计信息视图对象
 * 用于API响应的转发队列统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardQueueStatsVO {
    
    /**
     * 待处理数量
     */
    private Long pendingCount;
    
    /**
     * 成功数量
     */
    private Long successCount;
    
    /**
     * 失败数量
     */
    private Long failedCount;
    
    /**
     * 总数量
     */
    private Long totalCount;
}
