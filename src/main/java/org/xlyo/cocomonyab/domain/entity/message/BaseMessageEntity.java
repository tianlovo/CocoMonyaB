package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息实体基类
 * 包含所有消息类型的公共字段
 */
@Data
public abstract class BaseMessageEntity {
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 频道ID
     */
    private Long chatId;
    
    /**
     * 频道用户名
     */
    private String channelUsername;
    
    /**
     * 频道标题
     */
    private String channelTitle;
    
    /**
     * 消息日期（Unix时间戳）
     */
    private Integer date;
    
    /**
     * 编辑日期（Unix时间戳）
     */
    private Integer editDate;
    
    /**
     * 消息类型
     */
    private MessageType messageType;
    
    /**
     * 浏览次数
     */
    private Integer views;
    
    /**
     * 转发次数
     */
    private Integer forwards;
    
    // 媒体组相关字段
    
    /**
     * 媒体组ID（0表示非媒体组消息）
     */
    private Long mediaAlbumId;
    
    /**
     * 是否为媒体组消息
     */
    private Boolean isMediaGroup;
    
    /**
     * 媒体组中的消息数量
     */
    private Integer mediaGroupItemCount;
    
    /**
     * 媒体组中的所有消息ID列表
     */
    private List<Long> mediaGroupMessageIds;
    
    // 元数据
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 获取消息类型
     * 子类必须实现此方法返回对应的消息类型
     * 
     * @return 消息类型
     */
    public abstract MessageType getType();
}
