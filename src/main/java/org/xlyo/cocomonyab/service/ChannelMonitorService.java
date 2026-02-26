package org.xlyo.cocomonyab.service;

import com.google.common.util.concurrent.Striped;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.domain.entity.Channel;
import jakarta.annotation.PostConstruct;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageStorageService;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaGroupMessageEntity;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * 频道监控服务
 * 负责处理新消息、媒体组缓冲
 * <p>
 * 注意：频道监控检查已移至 ChannelMonitoringFilter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelMonitorService implements MediaGroupProcessor {
    
    private final ChannelRepository channelRepository;
    private final MessageStorageService messageStorageService;
    private final MessageParser messageParser;
    private final PluginManager pluginManager;
    private final FilterChainManager filterChainManager;
    private final ChannelMonitoringFilter channelMonitoringFilter;
    private final DuplicateMessageFilter duplicateMessageFilter;
    private final MediaGroupMetrics mediaGroupMetrics;
    private final ConcurrentSafetyProperties concurrentSafetyProperties;
    
    // 媒体组缓冲区：key = chatId:mediaAlbumId, value = 消息列表
    private final Map<String, List<TdApi.Message>> mediaGroupBuffer = new ConcurrentHashMap<>();
    
    // 媒体组最后更新时间：key = chatId:mediaAlbumId, value = 时间戳
    private final Map<String, Long> mediaGroupTimestamps = new ConcurrentHashMap<>();
    
    // 媒体组状态机：key = chatId:mediaAlbumId, value = 状态
    private final Map<String, MediaGroupState> mediaGroupStates = new ConcurrentHashMap<>();
    
    // 分段锁（锁条带数从配置读取）用于减少锁竞争
    private Striped<Lock> groupLocks;
    
    /**
     * 初始化监控指标
     * 注册 Gauge 指标以实时反映缓冲区大小和活跃媒体组数量
     */
    @PostConstruct
    public void initMetrics() {
        // 初始化分段锁（从配置读取锁条带数量）
        int lockStripes = concurrentSafetyProperties.getLock().getStripes();
        this.groupLocks = Striped.lock(lockStripes);
        log.info("初始化分段锁，锁条带数量: {}", lockStripes);
        
        // 注册媒体组缓冲区大小指标
        mediaGroupMetrics.registerBufferSizeGauge(() -> {
            return mediaGroupBuffer.values().stream()
                .mapToInt(List::size)
                .sum();
        });
        
        // 注册活跃媒体组数量指标
        mediaGroupMetrics.registerActiveMediaGroupCountGauge(() -> mediaGroupStates.size());
        
        log.info("媒体组监控指标已初始化");
    }
    
    /**
     * 获取媒体组的锁
     * 使用分段锁机制，基于 groupKey 计算锁索引
     * 
     * @param groupKey 媒体组键（chatId:mediaAlbumId）
     * @return 该媒体组对应的锁实例
     */
    private Lock getGroupLock(String groupKey) {
        return groupLocks.get(groupKey);
    }
    
    /**
     * 检查频道是否在监控列表中
     * 委托给 ChannelMonitoringFilter
     */
    public boolean isMonitoring(long chatId) {
        return channelMonitoringFilter.isMonitoring(chatId);
    }
    
    /**
     * 处理新消息
     */
    public void handleNewMessage(TdApi.Message message) {
        try {
            log.info("📨 [消息接收] chatId={}, messageId={}, isChannelPost={}, mediaAlbumId={}", 
                message.chatId, message.id, message.isChannelPost, message.mediaAlbumId);
            
            // 先执行过滤器链
            if (!filterChainManager.executeChain(message)) {
                log.info("❌ [消息过滤] 消息被过滤器拒绝: chatId={}, messageId={}", 
                    message.chatId, message.id);
                return; // 消息被过滤，不保存也不处理
            }
            
            log.info("✅ [过滤通过] 消息通过过滤器链: chatId={}, messageId={}", 
                message.chatId, message.id);
            
            // 检查是否为媒体组消息
            if (message.mediaAlbumId != 0) {
                log.info("📦 [媒体组] 检测到媒体组消息: chatId={}, messageId={}, mediaAlbumId={}", 
                    message.chatId, message.id, message.mediaAlbumId);
                handleMediaGroupMessage(message);
            } else {
                // 普通消息，直接处理
                log.info("📄 [单条消息] 处理单条消息: chatId={}, messageId={}", 
                    message.chatId, message.id);
                processSingleMessage(message);
            }
            
        } catch (Exception e) {
            log.error("❌ [处理失败] 处理频道消息失败: chatId={}, messageId={}", 
                message.chatId, message.id, e);
        }
    }
    
    /**
     * 处理媒体组消息（实现 MediaGroupProcessor 接口）
     * 使用状态机和分段锁保证并发安全
     * 
     * @param message 新到达的媒体组消息
     * @return true 如果消息被接受，false 如果被拒绝
     */
    @Override
    public boolean handleMediaGroupMessage(TdApi.Message message) {
        String groupKey = message.chatId + ":" + message.mediaAlbumId;
        Lock lock = getGroupLock(groupKey);
        
        long lockStartTime = System.currentTimeMillis();
        log.debug("尝试获取锁: groupKey={}, thread={}", groupKey, Thread.currentThread().getName());
        lock.lock();
        long lockWaitTime = System.currentTimeMillis() - lockStartTime;
        
        // 记录锁等待时间
        if (lockWaitTime > 0) {
            mediaGroupMetrics.recordLockWaitTime(groupKey, lockWaitTime);
            if (lockWaitTime > 1000) {
                log.warn("获取锁超时: groupKey={}, waitTime={}ms, thread={}", 
                    groupKey, lockWaitTime, Thread.currentThread().getName());
            }
        }
        
        log.debug("已获取锁: groupKey={}, waitTime={}ms, thread={}", 
            groupKey, lockWaitTime, Thread.currentThread().getName());
        
        try {
            // 检查当前状态
            MediaGroupState state = mediaGroupStates.get(groupKey);
            
            // 如果正在处理或已完成，拒绝新消息
            if (state == MediaGroupState.PROCESSING || state == MediaGroupState.COMPLETED) {
                String reason = "媒体组状态为 " + state + "，不接受新消息";
                log.warn("消息被拒绝: groupKey={}, messageId={}, currentState={}, reason={}", 
                    groupKey, message.id, state, reason);
                log.warn("并发冲突: groupKey={}, thread={}, operation={}, currentState={}", 
                    groupKey, Thread.currentThread().getName(), "handleMediaGroupMessage", state);
                return false;
            }
            
            // 设置为收集状态（如果是新媒体组）
            MediaGroupState previousState = mediaGroupStates.putIfAbsent(groupKey, MediaGroupState.COLLECTING);
            if (previousState == null) {
                // 新媒体组，记录状态转换
                log.info("媒体组状态转换: groupKey={}, oldState={}, newState={}", 
                    groupKey, "NONE", "COLLECTING");
                mediaGroupMetrics.recordStateTransition("NONE", "COLLECTING");
            }
            
            // 添加消息到缓冲区
            mediaGroupBuffer.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(message);
            
            // 更新时间戳
            mediaGroupTimestamps.put(groupKey, System.currentTimeMillis());
            
            log.debug("消息已添加到媒体组 {}: messageId={}, 当前数量={}, 状态={}", 
                groupKey, message.id, mediaGroupBuffer.get(groupKey).size(), 
                mediaGroupStates.get(groupKey));
            
            return true;
            
        } finally {
            lock.unlock();
            log.debug("已释放锁: groupKey={}, thread={}", groupKey, Thread.currentThread().getName());
        }
    }
    
    /**
     * 处理媒体组消息（内部方法，保持向后兼容）
     * 委托给 handleMediaGroupMessage(TdApi.Message)
     */
    private void handleMediaGroupMessage_legacy(TdApi.Message message) {
        handleMediaGroupMessage(message);
    }
    
    /**
     * 获取媒体组当前状态（实现 MediaGroupProcessor 接口）
     * 
     * @param groupKey 媒体组键（chatId:mediaAlbumId）
     * @return 媒体组状态，如果不存在返回 null
     */
    @Override
    public MediaGroupState getMediaGroupState(String groupKey) {
        return mediaGroupStates.get(groupKey);
    }
    
    /**
     * 定时处理超时的媒体组（实现 MediaGroupProcessor 接口）
     * 每秒检查一次，使用状态机和分段锁保证并发安全
     */
    @Override
    @Scheduled(fixedDelay = 1000)
    public void processTimedOutMediaGroups() {
        long now = System.currentTimeMillis();
        long mediaGroupTimeout = concurrentSafetyProperties.getMediaGroup().getTimeout();
        List<String> timedOutGroups = new ArrayList<>();
        
        // 找出超时的媒体组
        mediaGroupTimestamps.forEach((groupKey, timestamp) -> {
            if (now - timestamp >= mediaGroupTimeout) {
                Lock lock = getGroupLock(groupKey);
                
                long lockStartTime = System.currentTimeMillis();
                log.debug("尝试获取锁: groupKey={}, thread={}", groupKey, Thread.currentThread().getName());
                lock.lock();
                long lockWaitTime = System.currentTimeMillis() - lockStartTime;
                
                // 记录锁等待时间
                if (lockWaitTime > 0) {
                    mediaGroupMetrics.recordLockWaitTime(groupKey, lockWaitTime);
                    if (lockWaitTime > 1000) {
                        log.warn("获取锁超时: groupKey={}, waitTime={}ms, thread={}", 
                            groupKey, lockWaitTime, Thread.currentThread().getName());
                    }
                }
                
                log.debug("已获取锁: groupKey={}, waitTime={}ms, thread={}", 
                    groupKey, lockWaitTime, Thread.currentThread().getName());
                
                try {
                    MediaGroupState state = mediaGroupStates.get(groupKey);
                    
                    // 只处理 COLLECTING 状态的媒体组
                    if (state == MediaGroupState.COLLECTING) {
                        // 转换状态为 PROCESSING
                        mediaGroupStates.put(groupKey, MediaGroupState.PROCESSING);
                        timedOutGroups.add(groupKey);
                        
                        // 记录状态转换
                        log.info("媒体组状态转换: groupKey={}, oldState={}, newState={}", 
                            groupKey, "COLLECTING", "PROCESSING");
                        mediaGroupMetrics.recordStateTransition("COLLECTING", "PROCESSING");
                        
                        log.debug("媒体组 {} 超时，状态转换: COLLECTING -> PROCESSING", groupKey);
                    }
                } finally {
                    lock.unlock();
                    log.debug("已释放锁: groupKey={}, thread={}", groupKey, Thread.currentThread().getName());
                }
            }
        });
        
        // 处理超时的媒体组
        for (String groupKey : timedOutGroups) {
            processMediaGroup(groupKey);
        }
    }
    
    /**
     * 处理单个媒体组
     * 在锁保护下移除缓冲区数据并处理
     * 
     * @param groupKey 媒体组键（chatId:mediaAlbumId）
     */
    private void processMediaGroup(String groupKey) {
        Lock lock = getGroupLock(groupKey);
        long startTime = System.currentTimeMillis();
        
        long lockStartTime = System.currentTimeMillis();
        log.debug("尝试获取锁: groupKey={}, thread={}", groupKey, Thread.currentThread().getName());
        lock.lock();
        long lockWaitTime = System.currentTimeMillis() - lockStartTime;
        
        // 记录锁等待时间
        if (lockWaitTime > 0) {
            mediaGroupMetrics.recordLockWaitTime(groupKey, lockWaitTime);
            if (lockWaitTime > 1000) {
                log.warn("获取锁超时: groupKey={}, waitTime={}ms, thread={}", 
                    groupKey, lockWaitTime, Thread.currentThread().getName());
            }
        }
        
        log.debug("已获取锁: groupKey={}, waitTime={}ms, thread={}", 
            groupKey, lockWaitTime, Thread.currentThread().getName());
        
        try {
            // 移除缓冲区数据
            List<TdApi.Message> messages = mediaGroupBuffer.remove(groupKey);
            mediaGroupTimestamps.remove(groupKey);
            
            if (messages == null || messages.isEmpty()) {
                log.warn("媒体组 {} 缓冲区为空", groupKey);
                mediaGroupStates.remove(groupKey);
                return;
            }
            
            try {
                // 处理媒体组逻辑
                doProcessMediaGroup(messages);
                
                // 处理成功，转换状态为 COMPLETED
                mediaGroupStates.put(groupKey, MediaGroupState.COMPLETED);
                
                // 记录状态转换
                log.info("媒体组状态转换: groupKey={}, oldState={}, newState={}", 
                    groupKey, "PROCESSING", "COMPLETED");
                mediaGroupMetrics.recordStateTransition("PROCESSING", "COMPLETED");
                
                log.info("媒体组 {} 处理成功，状态转换: PROCESSING -> COMPLETED", groupKey);
                
                // 标记媒体组为已处理（防止重复处理）
                if (!messages.isEmpty()) {
                    TdApi.Message firstMessage = messages.get(0);
                    if (firstMessage.mediaAlbumId != 0) {
                        duplicateMessageFilter.markMediaGroupProcessed(
                            firstMessage.chatId, 
                            firstMessage.mediaAlbumId
                        );
                    }
                }
                
                // 记录处理延迟
                long duration = System.currentTimeMillis() - startTime;
                mediaGroupMetrics.recordProcessingDuration(groupKey, duration);
                
            } catch (Exception e) {
                log.error("媒体组 {} 处理失败，状态重置为 COLLECTING", groupKey, e);
                
                // 处理失败，重置状态允许重试
                mediaGroupStates.remove(groupKey);
                
                // 记录状态转换
                log.info("媒体组状态转换: groupKey={}, oldState={}, newState={}", 
                    groupKey, "PROCESSING", "NONE");
                mediaGroupMetrics.recordStateTransition("PROCESSING", "NONE");
            }
            
        } finally {
            lock.unlock();
            log.debug("已释放锁: groupKey={}, thread={}", groupKey, Thread.currentThread().getName());
        }
    }
    
    /**
     * 执行媒体组处理的实际逻辑
     * 
     * @param messages 媒体组的所有消息
     */
    private void doProcessMediaGroup(List<TdApi.Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        
        // 按消息ID排序
        messages.sort(Comparator.comparingLong(m -> m.id));
        
        TdApi.Message firstMessage = messages.getFirst();
        long mediaAlbumId = firstMessage.mediaAlbumId;
        long chatId = firstMessage.chatId;
        
        log.debug("处理媒体组: chatId={}, mediaAlbumId={}, 消息数量={}", 
            chatId, mediaAlbumId, messages.size());
        
        // 获取频道信息
        Channel channel = channelRepository.findByChannelId(chatId).orElse(null);
        String channelUsername = channel != null ? channel.getChannelUsername() : null;
        String channelTitle = channel != null ? channel.getChannelTitle() : null;
        
        // 1. 保存和解析每条消息（复用单消息处理逻辑）
        List<BaseMessageEntity> parsedMessages = new ArrayList<>();
        for (TdApi.Message message : messages) {
            // 保存原始消息
            messageStorageService.saveMessage(message);
            
            // 解析消息（使用 parseMediaGroupItem 忽略 mediaAlbumId）
            try {
                BaseMessageEntity entity = messageParser.parseMediaGroupItem(message, channelUsername, channelTitle);
                parsedMessages.add(entity);
            } catch (Exception e) {
                log.error("解析媒体组消息失败: chatId={}, messageId={}", 
                    message.chatId, message.id, e);
            }
        }
        
        // 2. 创建媒体组实体并处理
        if (!parsedMessages.isEmpty()) {
            MediaGroupMessageEntity mediaGroupEntity = createMediaGroupEntity(parsedMessages, mediaAlbumId);
            
            // 3. 使用插件管理器处理（包括控制台打印）
            pluginManager.process(mediaGroupEntity, firstMessage);
        }
    }
    
    /**
     * 创建媒体组实体
     */
    private MediaGroupMessageEntity createMediaGroupEntity(List<BaseMessageEntity> parsedMessages, long mediaAlbumId) {
        MediaGroupMessageEntity mediaGroupEntity = new MediaGroupMessageEntity();
        
        // 从第一条消息复制基础字段
        BaseMessageEntity first = parsedMessages.get(0);
        mediaGroupEntity.setMessageId(first.getMessageId());
        mediaGroupEntity.setChatId(first.getChatId());
        mediaGroupEntity.setChannelUsername(first.getChannelUsername());
        mediaGroupEntity.setChannelTitle(first.getChannelTitle());
        mediaGroupEntity.setDate(first.getDate());
        mediaGroupEntity.setEditDate(first.getEditDate());
        mediaGroupEntity.setViews(first.getViews());
        mediaGroupEntity.setForwards(first.getForwards());
        mediaGroupEntity.setMediaAlbumId(mediaAlbumId);
        mediaGroupEntity.setIsMediaGroup(true);
        mediaGroupEntity.setMediaGroupItemCount(parsedMessages.size());
        
        // 收集所有消息ID
        List<Long> messageIds = parsedMessages.stream()
            .map(BaseMessageEntity::getMessageId)
            .toList();
        mediaGroupEntity.setMediaGroupMessageIds(messageIds);
        
        // 设置媒体组项目
        mediaGroupEntity.setItems(parsedMessages);
        
        return mediaGroupEntity;
    }
    
    /**
     * 处理单条消息（非媒体组）
     */
    private void processSingleMessage(TdApi.Message message) {
        // 获取频道信息
        Channel channel = channelRepository.findByChannelId(message.chatId).orElse(null);
        String channelUsername = channel != null ? channel.getChannelUsername() : null;
        String channelTitle = channel != null ? channel.getChannelTitle() : null;
        
        log.info("💾 [保存消息] 保存原始消息到数据库: chatId={}, messageId={}, channel={}", 
            message.chatId, message.id, channelTitle != null ? channelTitle : "未知频道");
        
        // 1. 保存原始消息到数据库
        messageStorageService.saveMessage(message);
        
        // 2. 解析消息为实体类
        try {
            log.info("🔄 [解析消息] 开始解析消息: chatId={}, messageId={}", 
                message.chatId, message.id);
            
            BaseMessageEntity entity = messageParser.parse(message, channelUsername, channelTitle);
            
            log.info("🔌 [插件处理] 开始执行插件链: chatId={}, messageId={}, messageType={}", 
                message.chatId, message.id, entity.getType());
            
            // 3. 使用插件管理器处理（包括控制台打印）
            pluginManager.process(entity, message);
            
            log.info("✅ [处理完成] 消息处理完成: chatId={}, messageId={}", 
                message.chatId, message.id);
            
        } catch (Exception e) {
            log.error("❌ [解析失败] 解析消息失败: chatId={}, messageId={}", 
                message.chatId, message.id, e);
        }
    }
    
    /**
     * 启动监控
     * 委托给 ChannelMonitoringFilter
     */
    public void startMonitoring(long chatId) {
        channelMonitoringFilter.startMonitoring(chatId);
    }
    
    /**
     * 停止监控
     * 委托给 ChannelMonitoringFilter
     */
    public void stopMonitoring(long chatId) {
        channelMonitoringFilter.stopMonitoring(chatId);
    }
    
    /**
     * 重新加载监控列表
     * 委托给 ChannelMonitoringFilter
     */
    public void reloadMonitoringChannels() {
        channelMonitoringFilter.reloadMonitoringChannels();
    }
    
    /**
     * 获取监控频道数量
     * 委托给 ChannelMonitoringFilter
     */
    public int getMonitoringChannelCount() {
        return channelMonitoringFilter.getMonitoringChannelCount();
    }
}
