package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 转发队列视图对象
 * 用于API响应的转发队列数据
 */
@Data
public class ForwardQueueVO {
    
    /**
     * MongoDB文档ID
     */
    private String id;
    
    /**
     * 源频道ID
     */
    private Long sourceChatId;
    
    /**
     * 源消息ID
     */
    private Long sourceMessageId;
    
    /**
     * 媒体组消息ID列表
     */
    private List<Long> mediaGroupMessageIds;
    
    /**
     * 匹配到的标签列表
     */
    private List<String> matchedTags;
    
    /**
     * 转发状态
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 转发成功时间
     */
    private LocalDateTime forwardTime;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 错误消息
     */
    private String errorMessage;
}
