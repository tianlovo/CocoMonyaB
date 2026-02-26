package org.xlyo.cocomonyab.source.unread.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓冲区状态响应
 * <p>
 * 包含缓冲区中各状态消息的统计信息
 *
 * @author CocoMonya Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BufferStatusResponse {
    
    /**
     * 待处理消息数量
     */
    private long pending;
    
    /**
     * 已处理消息数量
     */
    private long processed;
    
    /**
     * 失败消息数量
     */
    private long failed;
    
    /**
     * 总消息数量
     */
    public long getTotal() {
        return pending + processed + failed;
    }
}
