package org.xlyo.cocomonyab.service;

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

/**
 * 频道监控服务
 * 负责处理新消息、媒体组缓冲
 * <p>
 * 注意：频道监控检查已移至 ChannelMonitoringFilter
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelMonitorService {
    
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
    
    // 媒体组等待超时时间（毫秒）
    private static final long MEDIA_GROUP_TIMEOUT = 2000; // 2秒
    
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
     * 处理媒体组消息
     */
    private void handleMediaGroupMessage(TdApi.Message message) {
        String groupKey = message.chatId + ":" + message.mediaAlbumId;
        
        // 添加到缓冲区
        mediaGroupBuffer.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(message);
        
        // 更新时间戳
        mediaGroupTimestamps.put(groupKey, System.currentTimeMillis());
        
        log.debug("收到媒体组消息: chatId={}, mediaAlbumId={}, messageId={}, 当前组内消息数: {}", 
            message.chatId, message.mediaAlbumId, message.id, 
            mediaGroupBuffer.get(groupKey).size());
    }
    
    /**
     * 定时处理超时的媒体组
     * 每秒检查一次
     */
    @Scheduled(fixedDelay = 1000)
    public void processTimedOutMediaGroups() {
        long now = System.currentTimeMillis();
        List<String> timedOutGroups = new ArrayList<>();
        
        // 找出超时的媒体组
        mediaGroupTimestamps.forEach((groupKey, timestamp) -> {
            if (now - timestamp >= MEDIA_GROUP_TIMEOUT) {
                timedOutGroups.add(groupKey);
            }
        });
        
        // 处理超时的媒体组
        for (String groupKey : timedOutGroups) {
            List<TdApi.Message> messages = mediaGroupBuffer.remove(groupKey);
            mediaGroupTimestamps.remove(groupKey);
            
            if (messages != null && !messages.isEmpty()) {
                processMediaGroup(messages);
            }
        }
    }
    
    /**
     * 处理完整的媒体组
     */
    private void processMediaGroup(List<TdApi.Message> messages) {
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
