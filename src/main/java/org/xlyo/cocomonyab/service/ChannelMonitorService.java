package org.xlyo.cocomonyab.service;

import com.google.common.util.concurrent.Striped;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageStorageService;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaGroupMessageEntity;

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
    
    // 媒体组缓冲区：key = chatId:mediaAlbumId, value = 消息列表
    private final Map<String, List<TdApi.Message>> mediaGroupBuffer = new ConcurrentHashMap<>();
    
    // 媒体组最后更新时间：key = chatId:mediaAlbumId, value = 时间戳
    private final Map<String, Long> mediaGroupTimestamps = new ConcurrentHashMap<>();
    
    // 媒体组状态机：key = chatId:mediaAlbumId, value = 状态
    private final Map<String, MediaGroupState> mediaGroupStates = new ConcurrentHashMap<>();
    
    // 分段锁（128 个锁条带）用于减少锁竞争
    private final Striped<Lock> groupLocks = Striped.lock(128);
    
    // 媒体组等待超时时间（毫秒）
    private static final long MEDIA_GROUP_TIMEOUT = 2000; // 2秒
    
    // 锁条带数量
    private static final int LOCK_STRIPES = 128;
    
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
            // 先执行过滤器链
            if (!filterChainManager.executeChain(message)) {
                log.debug("消息被过滤: chatId={}, messageId={}", message.chatId, message.id);
                return; // 消息被过滤，不保存也不处理
            }
            
            // 检查是否为媒体组消息
            if (message.mediaAlbumId != 0) {
                handleMediaGroupMessage(message);
            } else {
                // 普通消息，直接处理
                processSingleMessage(message);
            }
            
        } catch (Exception e) {
            log.error("处理频道消息失败: chatId={}, messageId={}", message.chatId, message.id, e);
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
        
        lock.lock();
        try {
            // 检查当前状态
            MediaGroupState state = mediaGroupStates.get(groupKey);
            
            // 如果正在处理或已完成，拒绝新消息
            if (state == MediaGroupState.PROCESSING || state == MediaGroupState.COMPLETED) {
                log.warn("媒体组 {} 状态为 {}，拒绝新消息: messageId={}", 
                    groupKey, state, message.id);
                return false;
            }
            
            // 设置为收集状态（如果是新媒体组）
            mediaGroupStates.putIfAbsent(groupKey, MediaGroupState.COLLECTING);
            
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
        List<String> timedOutGroups = new ArrayList<>();
        
        // 找出超时的媒体组
        mediaGroupTimestamps.forEach((groupKey, timestamp) -> {
            if (now - timestamp >= MEDIA_GROUP_TIMEOUT) {
                Lock lock = getGroupLock(groupKey);
                
                lock.lock();
                try {
                    MediaGroupState state = mediaGroupStates.get(groupKey);
                    
                    // 只处理 COLLECTING 状态的媒体组
                    if (state == MediaGroupState.COLLECTING) {
                        // 转换状态为 PROCESSING
                        mediaGroupStates.put(groupKey, MediaGroupState.PROCESSING);
                        timedOutGroups.add(groupKey);
                        
                        log.debug("媒体组 {} 超时，状态转换: COLLECTING -> PROCESSING", groupKey);
                    }
                } finally {
                    lock.unlock();
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
        
        lock.lock();
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
                
                log.info("媒体组 {} 处理成功，状态转换: PROCESSING -> COMPLETED", groupKey);
                
            } catch (Exception e) {
                log.error("媒体组 {} 处理失败，状态重置为 COLLECTING", groupKey, e);
                
                // 处理失败，重置状态允许重试
                mediaGroupStates.remove(groupKey);
            }
            
        } finally {
            lock.unlock();
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
        
        // 1. 保存原始消息到数据库
        messageStorageService.saveMessage(message);
        
        // 2. 解析消息为实体类
        try {
            BaseMessageEntity entity = messageParser.parse(message, channelUsername, channelTitle);
            
            // 3. 使用插件管理器处理（包括控制台打印）
            pluginManager.process(entity, message);
            
        } catch (Exception e) {
            log.error("解析消息失败: chatId={}, messageId={}", message.chatId, message.id, e);
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
