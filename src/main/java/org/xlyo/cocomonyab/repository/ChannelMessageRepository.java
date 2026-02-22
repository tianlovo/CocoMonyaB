package org.xlyo.cocomonyab.repository;

import lombok.NonNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;

import java.util.List;
import java.util.Optional;

/**
 * 频道消息Repository接口
 * 提供消息的CRUD操作和查询方法
 */
@Repository
public interface ChannelMessageRepository extends MongoRepository<@NonNull ChannelMessage, @NonNull String> {
    
    /**
     * 检查消息是否已存在（用于去重）
     *
     * @param chatId 频道ID
     * @param messageId 消息ID
     * @return 如果消息已存在返回true
     */
    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 根据频道ID和消息ID查找消息
     *
     * @param chatId 频道ID
     * @param messageId 消息ID
     * @return 包含找到的消息的Optional
     */
    Optional<ChannelMessage> findByChatIdAndMessageId(Long chatId, Long messageId);
    
    /**
     * 根据频道ID和媒体组ID查找所有消息
     *
     * @param chatId 频道ID
     * @param mediaAlbumId 媒体组ID
     * @return 属于该媒体组的所有消息列表
     */
    List<ChannelMessage> findByChatIdAndMediaAlbumId(Long chatId, Long mediaAlbumId);
    
    /**
     * 统计媒体组中的消息数量
     *
     * @param chatId 频道ID
     * @param mediaAlbumId 媒体组ID
     * @return 消息数量
     */
    long countByChatIdAndMediaAlbumId(Long chatId, Long mediaAlbumId);
}
