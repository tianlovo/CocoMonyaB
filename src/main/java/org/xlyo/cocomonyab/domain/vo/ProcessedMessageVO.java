package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 已处理消息视图对象
 * 用于API响应的已处理消息数据
 */
@Data
public class ProcessedMessageVO {
    
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
     * 消息类型
     */
    private String messageType;
    
    /**
     * 是否已读
     */
    private Boolean isRead;
    
    /**
     * 是否匹配标签
     */
    private Boolean isMatched;
    
    /**
     * 匹配到的标签列表
     */
    private List<String> matchedTags;
    
    /**
     * 处理时间
     */
    private LocalDateTime processTime;
    
    /**
     * 标记已读时间
     */
    private LocalDateTime readTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
