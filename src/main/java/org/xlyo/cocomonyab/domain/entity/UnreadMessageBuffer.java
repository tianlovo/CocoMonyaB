package org.xlyo.cocomonyab.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;

import java.time.LocalDateTime;

/**
 * 未读消息缓冲区实体
 * <p>
 * 用于临时存储获取到的未读消息，支持程序重启后恢复处理。
 * 缓冲区记录消息的基本信息和处理状态，确保消息不会因程序重启而丢失。
 * <p>
 * 业务规则：
 * <ul>
 *   <li>每条消息只缓冲一次（通过 chatId + messageId 唯一索引保证）</li>
 *   <li>消息处理成功后标记为 PROCESSED 状态</li>
 *   <li>消息处理失败后标记为 FAILED 状态并记录错误信息</li>
 *   <li>PROCESSED 状态的消息会在 7 天后自动清理（TTL 索引）</li>
 * </ul>
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "unread_messages_buffer")
@CompoundIndexes({
    @CompoundIndex(
        name = "idx_chat_message_unique",
        def = "{'chatId': 1, 'messageId': 1}",
        unique = true
    ),
    @CompoundIndex(
        name = "idx_status_fetchTime",
        def = "{'status': 1, 'fetchTime': 1}"
    )
})
public class UnreadMessageBuffer {
    
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
     * 获取时间
     * <p>
     * 消息从 Telegram API 获取并保存到缓冲区的时间
     */
    @Indexed
    private LocalDateTime fetchTime;
    
    /**
     * 缓冲区状态
     * <p>
     * PENDING: 待处理
     * PROCESSED: 已处理
     * FAILED: 处理失败
     */
    @Indexed
    private BufferStatus status;
    
    /**
     * 原始消息 JSON
     * <p>
     * 序列化的 TdApi.Message 对象，用于恢复完整的消息数据
     */
    private String rawMessage;
    
    /**
     * 错误信息
     * <p>
     * 当 status = FAILED 时，记录失败原因
     */
    private String errorMessage;
    
    /**
     * 创建时间
     * <p>
     * 用于 TTL 索引自动清理
     */
    @Indexed
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
