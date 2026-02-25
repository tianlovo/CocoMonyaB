package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

/**
 * 属�?7：失败重试机�?
 * 
 * 对于任何处理失败的媒体组，其状态应该被重置，允许重新收集和处理
 * 
 * **验证：需�?1.5**
 * 
 * Feature: concurrent-safety-optimization, Property 7: Failure Retry Mechanism
 */
class FailureRetryMechanismPropertyTest {
    
    @Property(tries = 15)
    @Label("Property 7: Failed processing resets state for retry")
    void failedProcessingResetsStateForRetry(
        @ForAll @IntRange(min = 1, max = 4) int messageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 设置服务，模拟处理失�?
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
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
        
        // 模拟保存失败
        when(messageStorageService.saveMessage(any())).thenThrow(new RuntimeException("Database error"));
        
        Channel channel = new Channel();
        channel.setChannelId(chatId);
        when(channelRepository.findByChannelId(chatId)).thenReturn(Optional.of(channel));
        
        when(messageParser.parseMediaGroupItem(any(), any(), any())).thenAnswer(invocation -> {
            TdApi.Message msg = invocation.getArgument(0);
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(msg.id);
            entity.setChatId(msg.chatId);
            entity.setMediaAlbumId(msg.mediaAlbumId);
            return entity;
        });
        
        // 添加消息到媒体组
        for (int i = 0; i < messageCount; i++) {
            TdApi.Message message = createMediaGroupMessage(1000L + i, chatId, mediaAlbumId);
            service.handleMediaGroupMessage(message);
        }
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 验证初始状态是 COLLECTING
        MediaGroupState initialState = service.getMediaGroupState(groupKey);
        assertThat(initialState)
            .as("初始状态应该是 COLLECTING")
            .isEqualTo(MediaGroupState.COLLECTING);
        
        // When: 等待超时并处理（会失败）
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // Then: 状态应该被重置（null 或不存在�?
        MediaGroupState stateAfterFailure = service.getMediaGroupState(groupKey);
        assertThat(stateAfterFailure)
            .as("失败后状态应该被重置�?null")
            .isNull();
        
        // When: 尝试添加新消息（应该被接受，因为状态已重置�?
        TdApi.Message retryMessage = createMediaGroupMessage(9999L, chatId, mediaAlbumId);
        boolean acceptedAfterFailure = service.handleMediaGroupMessage(retryMessage);
        
        // Then: 新消息应该被接受
        assertThat(acceptedAfterFailure)
            .as("失败后新消息应该被接受以支持重试")
            .isTrue();
        
        // 验证状态重新初始化�?COLLECTING
        MediaGroupState stateAfterRetry = service.getMediaGroupState(groupKey);
        assertThat(stateAfterRetry)
            .as("重试后状态应该重新初始化�?COLLECTING")
            .isEqualTo(MediaGroupState.COLLECTING);
    }
    
    @Property(tries = 12)
    @Label("Property 7: Successful retry after failure completes normally")
    void successfulRetryAfterFailureCompletesNormally(
        @ForAll @IntRange(min = 2, max = 3) int messageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 设置服务
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
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
        
        // 第一次调用失败，后续调用成功
        when(messageStorageService.saveMessage(any()))
            .thenThrow(new RuntimeException("First attempt failed"))
            .thenReturn(true);
        
        Channel channel = new Channel();
        channel.setChannelId(chatId);
        when(channelRepository.findByChannelId(chatId)).thenReturn(Optional.of(channel));
        
        when(messageParser.parseMediaGroupItem(any(), any(), any())).thenAnswer(invocation -> {
            TdApi.Message msg = invocation.getArgument(0);
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(msg.id);
            entity.setChatId(msg.chatId);
            entity.setMediaAlbumId(msg.mediaAlbumId);
            return entity;
        });
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 第一次尝试：添加消息并处理（会失败）
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(1000L + i, chatId, mediaAlbumId));
        }
        
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // 验证状态被重置
        MediaGroupState stateAfterFailure = service.getMediaGroupState(groupKey);
        assertThat(stateAfterFailure)
            .as("第一次失败后状态应该被重置")
            .isNull();
        
        // 第二次尝试：重新添加消息并处理（会成功）
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(2000L + i, chatId, mediaAlbumId));
        }
        
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // Then: 状态应该是 COMPLETED
        MediaGroupState finalState = service.getMediaGroupState(groupKey);
        assertThat(finalState)
            .as("重试成功后状态应该是 COMPLETED")
            .isEqualTo(MediaGroupState.COMPLETED);
    }
    
    @Property(tries = 10)
    @Label("Property 7: Multiple failures allow multiple retries")
    void multipleFailuresAllowMultipleRetries(
        @ForAll @IntRange(min = 1, max = 2) int messageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 设置服务
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
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
        
        // 前两次调用失败，第三次成�?
        when(messageStorageService.saveMessage(any()))
            .thenThrow(new RuntimeException("First attempt failed"))
            .thenThrow(new RuntimeException("Second attempt failed"))
            .thenReturn(true);
        
        Channel channel = new Channel();
        channel.setChannelId(chatId);
        when(channelRepository.findByChannelId(chatId)).thenReturn(Optional.of(channel));
        
        when(messageParser.parseMediaGroupItem(any(), any(), any())).thenAnswer(invocation -> {
            TdApi.Message msg = invocation.getArgument(0);
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(msg.id);
            entity.setChatId(msg.chatId);
            entity.setMediaAlbumId(msg.mediaAlbumId);
            return entity;
        });
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 第一次尝�?
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(1000L + i, chatId, mediaAlbumId));
        }
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        assertThat(service.getMediaGroupState(groupKey))
            .as("第一次失败后状态应该被重置")
            .isNull();
        
        // 第二次尝�?
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(2000L + i, chatId, mediaAlbumId));
        }
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        assertThat(service.getMediaGroupState(groupKey))
            .as("第二次失败后状态应该被重置")
            .isNull();
        
        // 第三次尝�?
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(3000L + i, chatId, mediaAlbumId));
        }
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // Then: 第三次应该成�?
        MediaGroupState finalState = service.getMediaGroupState(groupKey);
        assertThat(finalState)
            .as("第三次重试成功后状态应该是 COMPLETED")
            .isEqualTo(MediaGroupState.COMPLETED);
    }
    
    // 辅助方法
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
