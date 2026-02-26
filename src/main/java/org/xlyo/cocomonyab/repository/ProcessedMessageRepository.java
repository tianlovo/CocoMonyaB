package org.xlyo.cocomonyab.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.ProcessedMessage;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 已处理消息 Repository
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Repository
public interface ProcessedMessageRepository extends MongoRepository<ProcessedMessage, String> {
    
    /**
     * 通过 chatId 和 messageId 查询消息
     * 
     * @param chatId 频道 ID
     * @param messageId 消息 ID
     * @return 已处理消息记录
     */
    Optional<ProcessedMessage> findByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 检查消息是否已处理
     * 
     * @param chatId 频道 ID
     * @param messageId 消息 ID
     * @return true 如果已处理，false 否则
     */
    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 统计未读消息数量
     * 
     * @return 未读消息数量
     */
    long countByIsReadFalse();
    
    /**
     * 统计匹配标签的消息数量
     * 
     * @return 匹配标签的消息数量
     */
    long countByIsMatchedTrue();
    
    /**
     * 删除指定时间之前的记录
     * <p>
     * 用于定期清理历史数据
     * 
     * @param before 时间阈值
     * @return 删除的记录数
     */
    long deleteByCreateTimeBefore(LocalDateTime before);
}
