package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Size;
import org.mockito.ArgumentCaptor;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaGroupMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

/**
 * Property 11: Media Group Batch Processing
 * 
 * For any media group, when processed by the Plugin_Manager, all grouped messages 
 * should be passed to plugins together as a single MediaGroupMessageEntity containing all items.
 * 
 * **Validates: Requirements 10.3**
 * 
 * Feature: message-type-plugin-system, Property 11: Media Group Batch Processing
 */
class MediaGroupBatchProcessingPropertyTest {
    
    @Property(tries = 20)
    @Label("Property 11: Media Group Batch Processing - All messages in group passed together")
    void mediaGroupMessagesAreProcessedAsBatch(
        @ForAll @IntRange(min = 2, max = 10) int messageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = 1001000000000L, max = 1001999999999L) long chatId
    ) throws InterruptedException {
        // Given: 创建模拟依赖
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        org.xlyo.cocomonyab.filter.FilterChainManager filterChainManager = mock(org.xlyo.cocomonyab.filter.FilterChainManager.class);
        org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter channelMonitoringFilter = mock(org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter.class);
        DuplicateMessageFilter duplicateMessageFilter = mock(DuplicateMessageFilter.class);
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            duplicateMessageFilter,
            mediaGroupMetrics,
            properties
        );
        service.initMetrics();
        
        // 模拟过滤器链默认接受所有消�?
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        
        // 启动监控
        service.startMonitoring(chatId);
        
        // 创建媒体组消�?
        List<TdApi.Message> messages = new ArrayList<>();
        List<BaseMessageEntity> parsedEntities = new ArrayList<>();
        
        for (int i = 0; i < messageCount; i++) {
            long messageId = 1000L + i;
            TdApi.Message message = createMediaGroupMessage(messageId, chatId, mediaAlbumId);
            messages.add(message);
            
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(messageId);
            entity.setChatId(chatId);
            entity.setMediaAlbumId(mediaAlbumId);
            parsedEntities.add(entity);
            
            when(messageParser.parse(message)).thenReturn(entity);
        }
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
        // When: 处理所有媒体组消息
        for (TdApi.Message message : messages) {
            service.handleNewMessage(message);
        }
        
        // 等待超时处理
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // Then: 验证属�?
        ArgumentCaptor<BaseMessageEntity> entityCaptor = ArgumentCaptor.forClass(BaseMessageEntity.class);
        verify(pluginManager, times(1)).process(entityCaptor.capture(), any());
        
        BaseMessageEntity capturedEntity = entityCaptor.getValue();
        
        // Property 1: 传递的实体应该是MediaGroupMessageEntity
        assertThat(capturedEntity)
            .as("Plugin should receive MediaGroupMessageEntity")
            .isInstanceOf(MediaGroupMessageEntity.class);
        
        MediaGroupMessageEntity mediaGroup = (MediaGroupMessageEntity) capturedEntity;
        
        // Property 2: 媒体组应包含所有消�?
        assertThat(mediaGroup.getItems())
            .as("Media group should contain all messages")
            .hasSize(messageCount);
        
        // Property 3: 媒体组ID应正确设�?
        assertThat(mediaGroup.getMediaAlbumId())
            .as("Media album ID should match")
            .isEqualTo(mediaAlbumId);
        
        // Property 4: isMediaGroup标志应为true
        assertThat(mediaGroup.getIsMediaGroup())
            .as("isMediaGroup flag should be true")
            .isTrue();
        
        // Property 5: 媒体组项目数量应正确
        assertThat(mediaGroup.getMediaGroupItemCount())
            .as("Media group item count should match")
            .isEqualTo(messageCount);
        
        // Property 6: 所有消息ID应被记录
        assertThat(mediaGroup.getMediaGroupMessageIds())
            .as("All message IDs should be recorded")
            .hasSize(messageCount);
        
        // Property 7: 插件管理器应只被调用一次（批处理）
        verify(pluginManager, times(1)).process(any(), any());
    }
    
    @Property(tries = 15)
    @Label("Property 11: Media Group Batch Processing - Message order preserved")
    void mediaGroupMessageOrderIsPreserved(
        @ForAll @Size(min = 2, max = 5) List<@IntRange(min = 100, max = 999) Integer> messageIds,
        @ForAll @IntRange(min = 5000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = 1001000000000L, max = 1001999999999L) long chatId
    ) throws InterruptedException {
        // Given: 创建模拟依赖
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        org.xlyo.cocomonyab.filter.FilterChainManager filterChainManager = mock(org.xlyo.cocomonyab.filter.FilterChainManager.class);
        org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter channelMonitoringFilter = mock(org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter.class);
        DuplicateMessageFilter duplicateMessageFilter = mock(DuplicateMessageFilter.class);
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            duplicateMessageFilter,
            mediaGroupMetrics,
            properties
        );
        service.initMetrics();
        
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        service.startMonitoring(chatId);
        
        // 创建乱序的消�?
        List<TdApi.Message> messages = new ArrayList<>();
        for (Integer msgId : messageIds) {
            TdApi.Message message = createMediaGroupMessage(msgId.longValue(), chatId, mediaAlbumId);
            messages.add(message);
            
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(msgId.longValue());
            entity.setChatId(chatId);
            when(messageParser.parse(message)).thenReturn(entity);
        }
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
        // When: 处理消息
        for (TdApi.Message message : messages) {
            service.handleNewMessage(message);
        }
        
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // Then: 验证消息顺序
        ArgumentCaptor<BaseMessageEntity> entityCaptor = ArgumentCaptor.forClass(BaseMessageEntity.class);
        verify(pluginManager).process(entityCaptor.capture(), any());
        
        MediaGroupMessageEntity mediaGroup = (MediaGroupMessageEntity) entityCaptor.getValue();
        
        // Property: 消息应按ID排序
        List<Long> actualIds = mediaGroup.getItems().stream()
            .map(BaseMessageEntity::getMessageId)
            .toList();
        
        List<Long> expectedIds = messageIds.stream()
            .map(Integer::longValue)
            .sorted()
            .toList();
        
        assertThat(actualIds)
            .as("Messages should be sorted by ID")
            .isEqualTo(expectedIds);
    }
    
    @Property(tries = 15)
    @Label("Property 11: Media Group Batch Processing - Multiple groups processed independently")
    void multipleMediaGroupsProcessedIndependently(
        @ForAll @IntRange(min = 2, max = 4) int group1Size,
        @ForAll @IntRange(min = 2, max = 4) int group2Size,
        @ForAll @IntRange(min = 1000, max = 4999) long albumId1,
        @ForAll @IntRange(min = 5000, max = 9999) long albumId2,
        @ForAll @LongRange(min = 1001000000000L, max = 1001999999999L) long chatId
    ) throws InterruptedException {
        // Given: 创建两个不同的媒体组
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        org.xlyo.cocomonyab.filter.FilterChainManager filterChainManager = mock(org.xlyo.cocomonyab.filter.FilterChainManager.class);
        org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter channelMonitoringFilter = mock(org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter.class);
        DuplicateMessageFilter duplicateMessageFilter = mock(DuplicateMessageFilter.class);
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            duplicateMessageFilter,
            mediaGroupMetrics,
            properties
        );
        service.initMetrics();
        
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        service.startMonitoring(chatId);
        
        // 创建第一个媒体组
        for (int i = 0; i < group1Size; i++) {
            TdApi.Message message = createMediaGroupMessage(1000L + i, chatId, albumId1);
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(1000L + i);
            when(messageParser.parse(message)).thenReturn(entity);
            service.handleNewMessage(message);
        }
        
        // 创建第二个媒体组
        for (int i = 0; i < group2Size; i++) {
            TdApi.Message message = createMediaGroupMessage(2000L + i, chatId, albumId2);
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(2000L + i);
            when(messageParser.parse(message)).thenReturn(entity);
            service.handleNewMessage(message);
        }
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
        // When: 等待超时处理
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // Then: 验证两个媒体组都被独立处�?
        ArgumentCaptor<BaseMessageEntity> entityCaptor = ArgumentCaptor.forClass(BaseMessageEntity.class);
        verify(pluginManager, times(2)).process(entityCaptor.capture(), any());
        
        List<BaseMessageEntity> capturedEntities = entityCaptor.getAllValues();
        
        // Property 1: 应该有两个媒体组
        assertThat(capturedEntities)
            .as("Should process two media groups")
            .hasSize(2);
        
        // Property 2: 每个都应该是MediaGroupMessageEntity
        assertThat(capturedEntities)
            .as("All should be MediaGroupMessageEntity")
            .allMatch(e -> e instanceof MediaGroupMessageEntity);
        
        // Property 3: 媒体组大小应该正�?
        MediaGroupMessageEntity mg1 = (MediaGroupMessageEntity) capturedEntities.get(0);
        MediaGroupMessageEntity mg2 = (MediaGroupMessageEntity) capturedEntities.get(1);
        
        assertThat(mg1.getItems().size() + mg2.getItems().size())
            .as("Total items should match")
            .isEqualTo(group1Size + group2Size);
    }
    
    // Helper method
    private TdApi.Message createMediaGroupMessage(long messageId, long chatId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.isChannelPost = true;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.mediaAlbumId = mediaAlbumId;
        
        TdApi.MessagePhoto content = new TdApi.MessagePhoto();
        content.photo = new TdApi.Photo();
        content.photo.sizes = new TdApi.PhotoSize[0];
        content.caption = new TdApi.FormattedText();
        content.caption.text = "";
        message.content = content;
        
        return message;
    }

    private ConcurrentSafetyProperties createDefaultProperties() {
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        properties.getMediaGroup().setTimeout(2000);
        properties.getMediaGroup().setMaxBufferSize(1000);
        properties.getLock().setStripes(128);
        properties.getLock().setTimeout(5000);
        properties.getCache().setTtl(10);
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(5);
        return properties;
    }

}
