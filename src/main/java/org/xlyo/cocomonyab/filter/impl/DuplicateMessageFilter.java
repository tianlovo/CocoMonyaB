package org.xlyo.cocomonyab.filter.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.filter.AbstractMessageFilter;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;

import java.util.concurrent.TimeUnit;

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
 * <p>
 * 缓存策略：
 * - 使用 Caffeine 缓存，10秒自动过期
 * - 最大缓存大小 10000
 * - 启用缓存统计
 */
@Slf4j
@Component
public class DuplicateMessageFilter extends AbstractMessageFilter {
    
    private static final int PRIORITY = 95; // 高优先级，尽早过滤重复消息
    
    private final RawMessageRepository rawMessageRepository;
    private final ConcurrentSafetyProperties concurrentSafetyProperties;
    
    /**
     * 正在处理的消息缓存（内存去重）
     * 用于防止并发情况下的重复处理
     * Key格式：
     * - 单条消息: "chatId:messageId"
     * - 媒体组: "chatId:album:mediaAlbumId"
     * <p>
     * 使用 Caffeine 缓存：
     * - TTL 从配置读取（默认10秒）
     * - 最大缓存大小从配置读取（默认10000）
     * - 启用统计
     */
    private final Cache<String, Boolean> processingCache;
    
    /**
     * 失败消息的短暂缓存
     * TTL 从配置读取（默认5秒）
     * 用于防止失败消息立即重试
     */
    private final Cache<String, Boolean> failedCache;
    
    public DuplicateMessageFilter(
            RawMessageRepository rawMessageRepository,
            ConcurrentSafetyProperties concurrentSafetyProperties) {
        this.rawMessageRepository = rawMessageRepository;
        this.concurrentSafetyProperties = concurrentSafetyProperties;
        
        // 从配置读取缓存参数
        int cacheTtl = concurrentSafetyProperties.getCache().getTtl();
        int cacheMaxSize = concurrentSafetyProperties.getCache().getMaxSize();
        int failedMessageTtl = concurrentSafetyProperties.getCache().getFailedMessageTtl();
        
        // 配置主缓存
        this.processingCache = Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
            .maximumSize(cacheMaxSize)
            .recordStats()
            .build();
        
        // 配置失败缓存
        this.failedCache = Caffeine.newBuilder()
            .expireAfterWrite(failedMessageTtl, TimeUnit.SECONDS)
            .maximumSize(1000)
            .recordStats()
            .build();
        
        log.info("DuplicateMessageFilter 初始化完成 - 缓存TTL: {}秒, 最大大小: {}, 失败消息TTL: {}秒",
            cacheTtl, cacheMaxSize, failedMessageTtl);
    }
    
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
        // 媒体组消息的特殊处理
        if (message.mediaAlbumId != 0) {
            return filterMediaGroupMessage(message, context);
        }
        
        // 单条消息的处理
        return filterSingleMessage(message, context);
    }
    
    /**
     * 过滤单条消息
     */
    private FilterResult filterSingleMessage(TdApi.Message message, FilterContext context) {
        String cacheKey = buildCacheKey(message);
        
        // 第一层检查：内存缓存（防止并发重复）
        Boolean inCache = processingCache.getIfPresent(cacheKey);
        if (Boolean.TRUE.equals(inCache)) {
            context.setRejectReason(String.format(
                "消息正在处理中或已处理: %s", cacheKey
            ));
            log.debug("过滤正在处理的消息: {}", cacheKey);
            return FilterResult.REJECT;
        }
        
        // 第二层检查：数据库查询（持久化去重）
        boolean existsInDb = rawMessageRepository.existsByChatIdAndMessageId(
            message.chatId, 
            message.id
        );
        
        if (existsInDb) {
            // 数据库中已存在，添加到缓存并拒绝（保留缓存而不是移除）
            processingCache.put(cacheKey, Boolean.TRUE);
            context.setRejectReason(String.format(
                "数据库中存在重复消息: chatId=%d, messageId=%d", 
                message.chatId, message.id
            ));
            log.debug("过滤数据库中的重复消息: chatId={}, messageId={}", 
                message.chatId, message.id);
            return FilterResult.REJECT;
        }
        
        // 消息不重复，添加到缓存并接受
        processingCache.put(cacheKey, Boolean.TRUE);
        context.setAttribute("cacheKey", cacheKey);
        return FilterResult.ACCEPT;
    }
    
    /**
     * 过滤媒体组消息
     * 媒体组的每条消息都需要通过，但要防止整个媒体组被重复处理
     * 
     * 注意：媒体组的收集状态由 ChannelMonitorService 的状态机管理
     * DuplicateMessageFilter 只负责检查数据库中是否已存在该媒体组
     */
    private FilterResult filterMediaGroupMessage(TdApi.Message message, FilterContext context) {
        String albumCacheKey = message.chatId + ":album:" + message.mediaAlbumId;
        String messageCacheKey = buildCacheKey(message);
        
        // 第一层检查：内存缓存（防止并发重复）
        // 只检查缓存，不在这里添加到缓存
        Boolean inCache = processingCache.getIfPresent(albumCacheKey);
        if (Boolean.TRUE.equals(inCache)) {
            context.setRejectReason(String.format(
                "媒体组正在处理中或已处理: chatId=%d, mediaAlbumId=%d", 
                message.chatId, message.mediaAlbumId
            ));
            log.debug("过滤正在处理的媒体组: chatId={}, mediaAlbumId={}", 
                message.chatId, message.mediaAlbumId);
            return FilterResult.REJECT;
        }
        
        // 第二层检查：数据库查询（持久化去重）
        boolean albumExistsInDb = rawMessageRepository.existsByChatIdAndMediaAlbumId(
            message.chatId, 
            message.mediaAlbumId
        );
        
        if (albumExistsInDb) {
            // 整个媒体组已存在，添加到缓存并拒绝（保留缓存而不是移除）
            processingCache.put(albumCacheKey, Boolean.TRUE);
            context.setRejectReason(String.format(
                "数据库中存在重复媒体组: chatId=%d, mediaAlbumId=%d", 
                message.chatId, message.mediaAlbumId
            ));
            log.debug("过滤数据库中的重复媒体组: chatId={}, mediaAlbumId={}", 
                message.chatId, message.mediaAlbumId);
            return FilterResult.REJECT;
        }
        
        // 媒体组不存在于数据库，允许通过
        // 注意：不在这里标记媒体组为"正在处理"
        // 媒体组的收集状态由 ChannelMonitorService 的状态机管理
        // 只标记单条消息（防止同一条消息被重复处理）
        processingCache.put(messageCacheKey, Boolean.TRUE);
        
        context.setAttribute("cacheKey", messageCacheKey);
        context.setAttribute("albumCacheKey", albumCacheKey);
        return FilterResult.ACCEPT;
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(TdApi.Message message) {
        // 单条消息：chatId:messageId
        return message.chatId + ":" + message.id;
    }
    
    /**
     * 消息处理完成后的回调
     * 注意：使用 Caffeine 缓存后，不需要手动移除，缓存会自动过期
     * 此方法保留用于向后兼容
     */
    @Deprecated
    public void markProcessed(TdApi.Message message) {
        // 使用 Caffeine 缓存后，缓存会自动过期，不需要手动移除
        // 保留此方法用于向后兼容
        log.trace("消息处理完成（缓存将自动过期）: chatId={}, messageId={}", 
            message.chatId, message.id);
    }
    
    /**
     * 标记媒体组为已处理
     * 在媒体组成功保存到数据库后调用，防止重复处理
     * 
     * @param chatId 频道ID
     * @param mediaAlbumId 媒体组ID
     */
    public void markMediaGroupProcessed(long chatId, long mediaAlbumId) {
        String albumCacheKey = chatId + ":album:" + mediaAlbumId;
        processingCache.put(albumCacheKey, Boolean.TRUE);
        log.debug("标记媒体组为已处理: chatId={}, mediaAlbumId={}", chatId, mediaAlbumId);
    }
    
    /**
     * 标记消息处理失败
     * 保留在短暂缓存中（TTL从配置读取），防止立即重试
     * 
     * @param message 处理失败的消息
     */
    public void markFailed(TdApi.Message message) {
        String cacheKey = buildCacheKey(message);
        
        // 添加到失败缓存（TTL从配置读取）
        failedCache.put(cacheKey, Boolean.TRUE);
        
        // 如果是媒体组消息，也标记媒体组
        if (message.mediaAlbumId != 0) {
            String albumCacheKey = message.chatId + ":album:" + message.mediaAlbumId;
            failedCache.put(albumCacheKey, Boolean.TRUE);
        }
        
        int failedMessageTtl = concurrentSafetyProperties.getCache().getFailedMessageTtl();
        log.debug("消息处理失败，短暂缓存（{}秒）: {}", failedMessageTtl, cacheKey);
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    public CacheStats getCacheStats() {
        return processingCache.stats();
    }
    
    /**
     * 获取失败缓存统计信息
     * 
     * @return 失败缓存统计
     */
    public CacheStats getFailedCacheStats() {
        return failedCache.stats();
    }
    
    /**
     * 获取当前正在处理的消息数量（用于监控）
     */
    public long getProcessingCount() {
        return processingCache.estimatedSize();
    }
    
    /**
     * 获取失败缓存中的消息数量（用于监控）
     */
    public long getFailedCount() {
        return failedCache.estimatedSize();
    }
    
    /**
     * 清空处理缓存（用于测试或异常恢复）
     */
    public void clearCache() {
        long size = processingCache.estimatedSize();
        processingCache.invalidateAll();
        failedCache.invalidateAll();
        log.info("清空处理缓存，移除约 {} 条记录", size);
    }
}
