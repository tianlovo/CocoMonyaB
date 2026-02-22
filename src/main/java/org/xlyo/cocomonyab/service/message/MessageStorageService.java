package org.xlyo.cocomonyab.service.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

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
                // 即使重复，也要从过滤器缓存中移除
                duplicateMessageFilter.markProcessed(message);
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
            
            // 保存成功后，从过滤器缓存中移除（标记为已处理）
            duplicateMessageFilter.markProcessed(message);
            
            log.debug("已保存原始消息: chatId={}, messageId={}, mediaAlbumId={}", 
                message.chatId, message.id, message.mediaAlbumId);
            
            return true;
        } catch (Exception e) {
            log.error("保存消息失败: chatId={}, messageId={}, error={}", 
                message.chatId, message.id, e.getMessage(), e);
            
            // 保存失败，从过滤器缓存中移除（允许重试）
            duplicateMessageFilter.markFailed(message);
            
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
