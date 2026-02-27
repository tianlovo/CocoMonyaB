package org.xlyo.cocomonyab.repository;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;

import java.util.Optional;

/**
 * 转发队列Repository接口
 * 提供转发队列的CRUD操作和查询方法
 * 
 * @author tianluoqaq
 * @since 1.0
 */
@Repository
public interface ForwardQueueRepository extends MongoRepository<@NonNull ForwardQueueItem, @NonNull String> {
    
    /**
     * 根据源频道ID和源消息ID查找队列记录
     *
     * @param sourceChatId 源频道ID
     * @param sourceMessageId 源消息ID
     * @return 包含找到的队列记录的Optional
     */
    Optional<ForwardQueueItem> findBySourceChatIdAndSourceMessageId(Long sourceChatId, Long sourceMessageId);
    
    /**
     * 根据源频道ID分页查询队列记录，按创建时间升序排列
     *
     * @param sourceChatId 源频道ID
     * @param pageable 分页参数
     * @return 队列记录分页结果
     */
    Page<@NonNull ForwardQueueItem> findBySourceChatIdOrderByCreateTimeAsc(Long sourceChatId, Pageable pageable);
    
    /**
     * 根据状态分页查询队列记录，按创建时间升序排列
     *
     * @param status 转发状态
     * @param pageable 分页参数
     * @return 队列记录分页结果
     */
    Page<@NonNull ForwardQueueItem> findByStatusOrderByCreateTimeAsc(ForwardStatus status, Pageable pageable);
    
    /**
     * 根据源频道ID和状态分页查询队列记录，按创建时间升序排列
     *
     * @param sourceChatId 源频道ID
     * @param status 转发状态
     * @param pageable 分页参数
     * @return 队列记录分页结果
     */
    Page<@NonNull ForwardQueueItem> findBySourceChatIdAndStatusOrderByCreateTimeAsc(Long sourceChatId, ForwardStatus status, Pageable pageable);
    
    /**
     * 统计指定状态的队列记录数量
     *
     * @param status 转发状态
     * @return 队列记录数量
     */
    Long countByStatus(ForwardStatus status);
}
