package org.xlyo.cocomonyab.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.RawMessage;

import java.util.Optional;

/**
 * 原始消息数据访问接口
 */
@Repository
public interface RawMessageRepository extends MongoRepository<RawMessage, String> {
    
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
}
