package org.xlyo.cocomonyab.repository;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.RawMessage;

import java.util.List;
import java.util.Optional;

/**
 * 原始消息数据访问接口
 */
@Repository
public interface RawMessageRepository extends MongoRepository<@NonNull RawMessage, @NonNull String> {
    
    /**
     * 检查消息是否已存在（通过chatId和messageId）
     */
    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 检查媒体组消息是否已存在（通过chatId和mediaAlbumId）
     */
    boolean existsByChatIdAndMediaAlbumId(Long chatId, Long mediaAlbumId);
    
    /**
     * 查找消息（通过chatId和messageId）
     */
    Optional<RawMessage> findByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 查找媒体组消息（通过chatId和mediaAlbumId）
     */
    Optional<RawMessage> findByChatIdAndMediaAlbumId(Long chatId, Long mediaAlbumId);
    
    /**
     * 统计指定频道和媒体组的消息数量
     */
    long countByChatIdAndMediaAlbumId(Long chatId, Long mediaAlbumId);
    
    /**
     * 统计指定频道的消息数量
     */
    long countByChatId(Long chatId);
    
    /**
     * 查找指定频道和媒体组的所有消息
     */
    List<RawMessage> findAllByChatIdAndMediaAlbumId(Long chatId, Long mediaAlbumId);
    
    /**
     * 按频道分页查询（按日期降序）
     */
    Page<@NonNull RawMessage> findByChatIdOrderByDateDesc(Long chatId, Pageable pageable);
    
    /**
     * 按频道和日期范围查询（按日期降序）
     */
    Page<@NonNull RawMessage> findByChatIdAndDateBetweenOrderByDateDesc(
        Long chatId, Integer startDate, Integer endDate, Pageable pageable);
    
    /**
     * 按日期范围查询（按日期降序）
     */
    Page<@NonNull RawMessage> findByDateBetweenOrderByDateDesc(
        Integer startDate, Integer endDate, Pageable pageable);
    
    /**
     * 按日期范围统计
     */
    long countByDateBetween(Integer startDate, Integer endDate);
    
    /**
     * 按频道和日期范围统计
     */
    long countByChatIdAndDateBetween(Long chatId, Integer startDate, Integer endDate);
    
    /**
     * 按频道分组统计（使用聚合）
     */
    @Aggregation(pipeline = {
        "{ $group: { _id: '$chatId', count: { $sum: 1 } } }",
        "{ $sort: { count: -1 } }"
    })
    List<ChannelMessageCount> countGroupByChatId();
}
