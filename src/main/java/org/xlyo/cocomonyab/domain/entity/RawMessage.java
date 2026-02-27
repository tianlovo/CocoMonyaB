package org.xlyo.cocomonyab.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 原始消息存储实体
 * 直接保存TDLib消息对象的JSON序列化数据
 */
@Document(collection = "raw_messages")
@CompoundIndexes({
    @CompoundIndex(name = "chat_message_unique", def = "{'chatId': 1, 'messageId': 1}", unique = true),
    @CompoundIndex(name = "chat_album_unique", def = "{'chatId': 1, 'mediaAlbumId': 1}", unique = true, sparse = true),
    @CompoundIndex(name = "chat_date_idx", def = "{'chatId': 1, 'date': -1}")
})
@Data
public class RawMessage {
    @Id
    private String id;
    
    /**
     * 频道ID
     */
    @Indexed
    private Long chatId;
    
    /**
     * 消息ID（单条消息的主键之一）
     */
    @Indexed
    private Long messageId;
    
    /**
     * 媒体组ID（媒体组消息的主键之一）
     */
    @Indexed
    private Long mediaAlbumId;
    
    /**
     * 消息日期（Unix时间戳）
     */
    private Integer date;
    
    /**
     * TDLib原始消息对象的JSON序列化数据
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
