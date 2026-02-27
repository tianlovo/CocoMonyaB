package org.xlyo.cocomonyab.repository;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;

import java.util.List;
import java.util.Optional;

/**
 * 未读消息缓冲区 Repository
 * 
 * @author tianluoqaq
 * @since 1.0
 */
@Repository
public interface UnreadMessageBufferRepository extends MongoRepository<@NonNull UnreadMessageBuffer, @NonNull String> {
    
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
    
    /**
     * 根据频道ID和消息ID查找缓冲记录
     *
     * @param chatId 频道ID
     * @param messageId 消息ID
     * @return 包含找到的缓冲记录的Optional
     */
    Optional<UnreadMessageBuffer> findByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 根据频道ID分页查询缓冲记录，按获取时间升序排列
     *
     * @param chatId 频道ID
     * @param pageable 分页参数
     * @return 缓冲记录分页结果
     */
    Page<@NonNull UnreadMessageBuffer> findByChatIdOrderByFetchTimeAsc(Long chatId, Pageable pageable);
    
    /**
     * 根据状态分页查询缓冲记录，按获取时间升序排列
     *
     * @param status 缓冲区状态
     * @param pageable 分页参数
     * @return 缓冲记录分页结果
     */
    Page<@NonNull UnreadMessageBuffer> findByStatusOrderByFetchTimeAsc(BufferStatus status, Pageable pageable);
    
    /**
     * 根据频道ID和状态分页查询缓冲记录，按获取时间升序排列
     *
     * @param chatId 频道ID
     * @param status 缓冲区状态
     * @param pageable 分页参数
     * @return 缓冲记录分页结果
     */
    Page<@NonNull UnreadMessageBuffer> findByChatIdAndStatusOrderByFetchTimeAsc(Long chatId, BufferStatus status, Pageable pageable);
    
    /**
     * 统计指定频道和状态的消息数量
     *
     * @param chatId 频道ID
     * @param status 缓冲区状态
     * @return 消息数量
     */
    Long countByChatIdAndStatus(Long chatId, BufferStatus status);
}
