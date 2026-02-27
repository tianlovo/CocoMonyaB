package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未读消息缓冲区视图对象
 * 用于API响应的未读消息缓冲区数据
 */
@Data
public class UnreadMessageBufferVO {
    
    /**
     * MongoDB文档ID
     */
    private String id;
    
    /**
     * 频道ID
     */
    private Long chatId;
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 获取时间
     */
    private LocalDateTime fetchTime;
    
    /**
     * 缓冲区状态
     */
    private String status;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
