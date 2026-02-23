package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

/**
 * 属性 9：超时状态转换
 * 
 * 对于任何超时且状态为 COLLECTING 的媒体组，
 * 其状态应该转换为 PROCESSING
 * 
 * **验证：需求 2.3**
 * 
 * Feature: concurrent-safety-optimization, Property 9: Timeout State Transition
 */
class TimeoutStateTransitionPropertyTest {
    
    @Property(tries = 15)
    @Label("Property 9: Timeout transitions COLLECTING to PROCESSING")
    void timeoutTransitionsCollectingToProcessing(
        @ForAll @IntRange(min = 1, max = 5) int messageCount,
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
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            mediaGroupMetrics,
            properties
        );
        service.initMetrics();
        
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
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
        
        // When: 等待超时（2秒超时 + 500ms 缓冲）
        Thread.sleep(2500);
        
        // 触发定时任务
        service.processTimedOutMediaGroups();
        
        // Then: 状态应该转换为 PROCESSING 或 COMPLETED
        MediaGroupState finalState = service.getMediaGroupState(groupKey);
        assertThat(finalState)
            .as("超时后状态应该从 COLLECTING 转换为 PROCESSING 或 COMPLETED")
            .isIn(MediaGroupState.PROCESSING, MediaGroupState.COMPLETED);
    }
    
    @Property(tries = 12)
    @Label("Property 9: Non-timeout does not transition state")
    void nonTimeoutDoesNotTransitionState(
        @ForAll @IntRange(min = 1, max = 3) int messageCount,
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
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            mediaGroupMetrics,
            properties
        );
        service.initMetrics();
        
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
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
        
        // When: 等待时间不足以超时（1秒，小于2秒超时）
        Thread.sleep(1000);
        
        // 触发定时任务
        service.processTimedOutMediaGroups();
        
        // Then: 状态应该仍然是 COLLECTING
        MediaGroupState finalState = service.getMediaGroupState(groupKey);
        assertThat(finalState)
            .as("未超时时状态应该保持 COLLECTING")
            .isEqualTo(MediaGroupState.COLLECTING);
    }
    
    @Property(tries = 10)
    @Label("Property 9: Multiple groups timeout independently")
    void multipleGroupsTimeoutIndependently(
        @ForAll @IntRange(min = 2, max = 4) int groupCount,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 设置服务
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        ConcurrentSafetyProperties properties = createDefaultProperties();
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            mediaGroupMetrics,
            properties
        );
        service.initMetrics();
        
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
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
        
        // 创建多个媒体组，间隔添加
        for (int i = 0; i < groupCount; i++) {
            long mediaAlbumId = 1000L + i;
            TdApi.Message message = createMediaGroupMessage(1000L, chatId, mediaAlbumId);
            service.handleMediaGroupMessage(message);
            
            // 每个组之间间隔1秒（确保时间差异明显）
            if (i < groupCount - 1) {
                Thread.sleep(1000);
            }
        }
        
        // 等待第一个组超时（2.5秒，确保第一个组超时但最后一个组未超时）
        Thread.sleep(1500);
        
        // 触发定时任务
        service.processTimedOutMediaGroups();
        
        // Then: 验证第一个组已转换，后续组仍在 COLLECTING
        String firstGroupKey = chatId + ":" + 1000L;
        MediaGroupState firstGroupState = service.getMediaGroupState(firstGroupKey);
        assertThat(firstGroupState)
            .as("第一个组应该已超时并转换状态")
            .isIn(MediaGroupState.PROCESSING, MediaGroupState.COMPLETED);
        
        // 最后一个组应该还在 COLLECTING（因为间隔添加且时间未到）
        if (groupCount > 1) {
            String lastGroupKey = chatId + ":" + (1000L + groupCount - 1);
            MediaGroupState lastGroupState = service.getMediaGroupState(lastGroupKey);
            assertThat(lastGroupState)
                .as("最后一个组应该还在 COLLECTING 状态")
                .isEqualTo(MediaGroupState.COLLECTING);
        }
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
