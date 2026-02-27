package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

/**
 * 消息统计视图对象
 * 用于返回消息统计数据
 */
@Data
public class MessageStatVO {
    
    /**
     * 频道ID
     */
    private Long chatId;
    
    /**
     * 消息数量
     */
    private Long count;
}
