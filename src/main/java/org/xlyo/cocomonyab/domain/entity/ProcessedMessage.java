package org.xlyo.cocomonyab.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 已处理消息记录
 * <p>
 * 用于跟踪 TagBasedMessageForwardingPlugin 处理过的消息，
 * 记录消息的处理状态和已读状态。
 * <p>
 * 业务规则：
 * <ul>
 *   <li>每条消息只记录一次（通过 chatId + messageId 唯一索引保证）</li>
 *   <li>如果消息未读，标记为已读后更新状态</li>
 *   <li>如果消息已处理，直接跳过后续处理</li>
 * </ul>
 * 
 * @author tianluoqaq
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_messages")
@CompoundIndex(
    name = "chat_message_unique",
    def = "{'chatId': 1, 'messageId': 1}",
    unique = true
)
public class ProcessedMessage {
    
    /**
     * MongoDB 文档 ID
     */
    @Id
    private String id;
    
    /**
     * 频道 ID（Telegram Chat ID）
     */
    @Indexed
    private Long chatId;
    
    /**
     * 消息 ID（Telegram Message ID）
     */
    @Indexed
    private Long messageId;
    
    /**
     * 消息类型
     * <p>
     * 如：TEXT, PHOTO, VIDEO, MEDIA_GROUP 等
     */
    private String messageType;
    
    /**
     * 是否已读
     * <p>
     * true: 已通过 TdApi.ViewMessages 标记为已读
     * false: 未标记为已读
     */
    @Indexed
    private Boolean isRead;
    
    /**
     * 是否匹配标签
     * <p>
     * true: 匹配到标签，已加入转发队列
     * false: 未匹配到标签
     */
    @Indexed
    private Boolean isMatched;
    
    /**
     * 匹配到的标签列表
     * <p>
     * 如果 isMatched = true，此字段包含匹配到的标签
     * 如果 isMatched = false，此字段为 null 或空数组
     */
    private String[] matchedTags;
    
    /**
     * 处理时间
     * <p>
     * 插件首次处理此消息的时间
     */
    @Indexed
    private LocalDateTime processTime;
    
    /**
     * 标记已读时间
     * <p>
     * 通过 TdApi.ViewMessages 标记为已读的时间
     * 如果 isRead = false，此字段为 null
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
