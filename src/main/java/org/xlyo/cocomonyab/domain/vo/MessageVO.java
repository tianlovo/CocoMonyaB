package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息视图对象
 * 用于API响应的消息数据
 */
@Data
public class MessageVO {
    
    /**
     * MongoDB文档ID
     */
    private String id;
    
    /**
     * 频道ID（Telegram）
     */
    private Long chatId;
    
    /**
     * 消息ID（Telegram）
     */
    private Long messageId;
    
    /**
     * 媒体组ID（Telegram，可能为null）
     */
    private Long mediaAlbumId;
    
    /**
     * 消息日期（Unix时间戳）
     */
    private Integer date;
    
    /**
     * TDLib原始消息JSON
     */
    private String rawJson;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
