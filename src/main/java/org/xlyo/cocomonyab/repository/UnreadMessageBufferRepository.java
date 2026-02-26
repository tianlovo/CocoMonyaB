package org.xlyo.cocomonyab.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;

import java.util.List;

/**
 * 未读消息缓冲区 Repository
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Repository
public interface UnreadMessageBufferRepository extends MongoRepository<UnreadMessageBuffer, String> {
    
    /**
     * 检查消息是否已在缓冲区中
     * 
     * @param chatId 频道 ID
     * @param messageId 消息 ID
     * @return true 如果已存在，false 否则
     */
    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 根据状态查询缓冲消息
     * 
     * @param status 缓冲区状态
     * @return 符合条件的缓冲消息列表
     */
    List<UnreadMessageBuffer> findByStatus(BufferStatus status);
    
    /**
     * 统计指定状态的消息数量
     * 
     * @param status 缓冲区状态
     * @return 消息数量
     */
    long countByStatus(BufferStatus status);
    
    /**
     * 根据频道 ID 和状态查询缓冲消息，按消息 ID 升序排序
     * 
     * @param chatId 频道 ID
     * @param status 缓冲区状态
     * @return 符合条件的缓冲消息列表，按 messageId 升序排序
     */
    List<UnreadMessageBuffer> findByChatIdAndStatusOrderByMessageIdAsc(Long chatId, BufferStatus status);
}
