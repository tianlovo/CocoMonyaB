package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.filter.AbstractMessageFilter;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重复消息过滤器
 * 避免插件重复处理已经保存过的消息
 * <p>
 * 使用两层检查机制防止并发重复：
 * 1. 内存缓存：快速检查正在处理的消息（解决并发竞态问题）
 * 2. 数据库查询：检查已保存的消息（持久化去重）
 * <p>
 * 检查逻辑：
 * - 单条消息：使用 chatId + messageId
 * - 媒体组消息：使用 chatId + mediaAlbumId
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateMessageFilter extends AbstractMessageFilter {
    
    private static final int PRIORITY = 95; // 高优先级，尽早过滤重复消息
    
    private final RawMessageRepository rawMessageRepository;
    
    /**
     * 正在处理的消息缓存（内存去重）
     * 用于防止并发情况下的重复处理
     * Key格式：
     * - 单条消息: "chatId:messageId"
     * - 媒体组: "chatId:album:mediaAlbumId"
     */
    private final Set<String> processingMessages = ConcurrentHashMap.newKeySet();
    
    @Override
    public String getName() {
        return "DuplicateMessageFilter";
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
        String cacheKey = buildCacheKey(message);
        
        // 第一层检查：内存缓存（防止并发重复）
        if (!processingMessages.add(cacheKey)) {
            // add() 返回 false 表示已存在，说明正在处理中
            context.setRejectReason(String.format(
                "消息正在处理中: %s", cacheKey
            ));
            log.debug("过滤正在处理的消息: {}", cacheKey);
            return FilterResult.REJECT;
        }
        
        // 第二层检查：数据库查询（持久化去重）
        boolean existsInDb = checkDatabase(message);
        
        if (existsInDb) {
            // 数据库中已存在，从缓存中移除并拒绝
            processingMessages.remove(cacheKey);
            
            if (message.mediaAlbumId != 0) {
                context.setRejectReason(String.format(
                    "数据库中存在重复媒体组: chatId=%d, mediaAlbumId=%d", 
                    message.chatId, message.mediaAlbumId
                ));
                log.debug("过滤数据库中的重复媒体组: chatId={}, mediaAlbumId={}", 
                    message.chatId, message.mediaAlbumId);
            } else {
                context.setRejectReason(String.format(
                    "数据库中存在重复消息: chatId=%d, messageId=%d", 
                    message.chatId, message.id
                ));
                log.debug("过滤数据库中的重复消息: chatId={}, messageId={}", 
                    message.chatId, message.id);
            }
            
            return FilterResult.REJECT;
        }
        
        // 消息不重复，标记为正在处理并接受
        // 注意：消息处理完成后需要从缓存中移除，这由 MessageStorageService 负责
        context.setAttribute("cacheKey", cacheKey);
        return FilterResult.ACCEPT;
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(TdApi.Message message) {
        if (message.mediaAlbumId != 0) {
            // 媒体组消息：chatId:album:mediaAlbumId
            return message.chatId + ":album:" + message.mediaAlbumId;
        } else {
            // 单条消息：chatId:messageId
            return message.chatId + ":" + message.id;
        }
    }
    
    /**
     * 检查数据库中是否存在
     */
    private boolean checkDatabase(TdApi.Message message) {
        if (message.mediaAlbumId != 0) {
            // 媒体组消息：使用 chatId + mediaAlbumId 检查
            return rawMessageRepository.existsByChatIdAndMediaAlbumId(
                message.chatId, 
                message.mediaAlbumId
            );
        } else {
            // 单条消息：使用 chatId + messageId 检查
            return rawMessageRepository.existsByChatIdAndMessageId(
                message.chatId, 
                message.id
            );
        }
    }
    
    /**
     * 消息处理完成后，从缓存中移除
     * 此方法应该在消息成功保存到数据库后调用
     */
    public void markProcessed(TdApi.Message message) {
        String cacheKey = buildCacheKey(message);
        processingMessages.remove(cacheKey);
        log.trace("消息处理完成，从缓存移除: {}", cacheKey);
    }
    
    /**
     * 消息处理失败后，从缓存中移除
     * 此方法应该在消息保存失败时调用，允许重试
     */
    public void markFailed(TdApi.Message message) {
        String cacheKey = buildCacheKey(message);
        processingMessages.remove(cacheKey);
        log.debug("消息处理失败，从缓存移除: {}", cacheKey);
    }
    
    /**
     * 获取当前正在处理的消息数量（用于监控）
     */
    public int getProcessingCount() {
        return processingMessages.size();
    }
    
    /**
     * 清空处理缓存（用于测试或异常恢复）
     */
    public void clearCache() {
        int size = processingMessages.size();
        processingMessages.clear();
        log.info("清空处理缓存，移除 {} 条记录", size);
    }
}
