package org.xlyo.cocomonyab.service.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.repository.RawMessageRepository;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

import java.time.LocalDateTime;

/**
 * 消息存储服务
 * 负责将TDLib原始消息保存到MongoDB
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStorageService {
    
    private final RawMessageRepository rawMessageRepository;
    private final ObjectMapper objectMapper;
    private final DuplicateMessageFilter duplicateMessageFilter;
    private final MediaGroupMetrics mediaGroupMetrics;
    
    /**
     * 保存消息
     * 
     * @param message TDLib原始消息
     * @return 是否保存成功
     */
    public boolean saveMessage(TdApi.Message message) {
        try {
            // 去重检查（这里是双重检查，防止过滤器被禁用的情况）
            if (isDuplicate(message)) {
                log.debug("消息已存在: chatId={}, messageId={}, mediaAlbumId={}", 
                    message.chatId, message.id, message.mediaAlbumId);
                return false;
            }

            // 序列化为JSON
            String rawJson = serializeToJson(message);

            // 创建RawMessage实体
            RawMessage rawMessage = new RawMessage();
            rawMessage.setChatId(message.chatId);
            rawMessage.setMessageId(message.id);
            rawMessage.setMediaAlbumId(message.mediaAlbumId != 0 ? message.mediaAlbumId : null);
            rawMessage.setDate(message.date);
            rawMessage.setRawJson(rawJson);
            rawMessage.setCreateTime(LocalDateTime.now());
            rawMessage.setUpdateTime(LocalDateTime.now());

            // 保存到数据库
            rawMessageRepository.save(rawMessage);

            log.debug("已保存原始消息: chatId={}, messageId={}, mediaAlbumId={}", 
                message.chatId, message.id, message.mediaAlbumId);

            return true;

        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 数据库唯一索引冲突，消息已存在
            log.warn("消息已存在（数据库索引冲突）: chatId={}, messageId={}, mediaAlbumId={}, error={}", 
                message.chatId, message.id, message.mediaAlbumId, e.getMessage());

            // 记录数据库保存失败指标
            mediaGroupMetrics.recordSaveFailure("duplicate_key");

            // 保存成功的消息应该保留在缓存中（不调用 markFailed）
            return false;

        } catch (Exception e) {
            // 其他错误（序列化失败、数据库连接失败等）
            log.error("保存消息失败: chatId={}, messageId={}, mediaAlbumId={}, errorType={}, error={}", 
                message.chatId, message.id, message.mediaAlbumId, 
                e.getClass().getSimpleName(), e.getMessage(), e);

            // 记录数据库保存失败指标
            mediaGroupMetrics.recordSaveFailure(e.getClass().getSimpleName());

            // 保存失败，标记失败并短暂缓存（防止立即重试）
            duplicateMessageFilter.markFailed(message);

            return false;
        }
    }

    /**
     * 批量保存消息
     * 
     * @param messages TDLib原始消息列表
     * @return 是否全部保存成功
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean saveAll(java.util.List<TdApi.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("批量保存消息：消息列表为空");
            return false;
        }
        
        try {
            java.util.List<RawMessage> rawMessages = new java.util.ArrayList<>();
            
            // 转换所有消息
            for (TdApi.Message message : messages) {
                // 去重检查
                if (isDuplicate(message)) {
                    log.debug("批量保存：消息已存在，跳过: chatId={}, messageId={}, mediaAlbumId={}", 
                        message.chatId, message.id, message.mediaAlbumId);
                    continue;
                }
                
                // 序列化为JSON
                String rawJson = serializeToJson(message);
                
                // 创建RawMessage实体
                RawMessage rawMessage = new RawMessage();
                rawMessage.setChatId(message.chatId);
                rawMessage.setMessageId(message.id);
                rawMessage.setMediaAlbumId(message.mediaAlbumId != 0 ? message.mediaAlbumId : null);
                rawMessage.setDate(message.date);
                rawMessage.setRawJson(rawJson);
                rawMessage.setCreateTime(LocalDateTime.now());
                rawMessage.setUpdateTime(LocalDateTime.now());
                
                rawMessages.add(rawMessage);
            }
            
            if (rawMessages.isEmpty()) {
                log.debug("批量保存：所有消息都已存在，无需保存");
                return false;
            }
            
            // 批量保存到数据库（事务保护）
            rawMessageRepository.saveAll(rawMessages);
            
            log.info("批量保存消息成功: 数量={}", rawMessages.size());
            
            return true;
            
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 数据库唯一索引冲突
            log.error("批量保存消息失败（数据库索引冲突）: 消息数量={}, error={}", 
                messages.size(), e.getMessage(), e);
            
            // 记录数据库保存失败指标
            mediaGroupMetrics.recordSaveFailure("duplicate_key_batch");
            
            // 批量失败时标记所有消息
            for (TdApi.Message message : messages) {
                duplicateMessageFilter.markFailed(message);
            }
            
            return false;
            
        } catch (Exception e) {
            // 其他错误（序列化失败、数据库连接失败等）
            log.error("批量保存消息失败: 消息数量={}, errorType={}, error={}", 
                messages.size(), e.getClass().getSimpleName(), e.getMessage(), e);
            
            // 记录数据库保存失败指标
            mediaGroupMetrics.recordSaveFailure(e.getClass().getSimpleName() + "_batch");
            
            // 批量失败时标记所有消息
            for (TdApi.Message message : messages) {
                duplicateMessageFilter.markFailed(message);
            }
            
            return false;
        }
    }

    
    /**
     * 检查消息是否重复
     */
    private boolean isDuplicate(TdApi.Message message) {
        // 媒体组消息使用mediaAlbumId检查
        if (message.mediaAlbumId != 0) {
            return rawMessageRepository.existsByChatIdAndMediaAlbumId(
                message.chatId, message.mediaAlbumId);
        }
        
        // 单条消息使用messageId检查
        return rawMessageRepository.existsByChatIdAndMessageId(
            message.chatId, message.id);
    }
    
    /**
     * 序列化消息为JSON
     */
    private String serializeToJson(TdApi.Message message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }
    
    /**
     * 从JSON反序列化消息
     */
    public TdApi.Message deserializeFromJson(String json) throws Exception {
        return objectMapper.readValue(json, TdApi.Message.class);
    }
    
    /**
     * 获取原始消息
     */
    public TdApi.Message getRawMessage(Long chatId, Long messageId) {
        try {
            return rawMessageRepository.findByChatIdAndMessageId(chatId, messageId)
                .map(raw -> {
                    try {
                        return deserializeFromJson(raw.getRawJson());
                    } catch (Exception e) {
                        log.error("反序列化消息失败: chatId={}, messageId={}", 
                            chatId, messageId, e);
                        return null;
                    }
                })
                .orElse(null);
        } catch (Exception e) {
            log.error("获取原始消息失败: chatId={}, messageId={}", 
                chatId, messageId, e);
            return null;
        }
    }
}
