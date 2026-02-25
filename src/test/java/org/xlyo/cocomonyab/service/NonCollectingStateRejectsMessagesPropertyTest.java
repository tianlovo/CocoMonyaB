package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 属�?2：非收集状态拒绝新消息
 * 
 * 对于任何处于 PROCESSING �?COMPLETED 状态的媒体组，
 * 尝试添加新消息应该被拒绝
 * 
 * **验证：需�?1.3, 2.5**
 * 
 * Feature: concurrent-safety-optimization, Property 2: Non-Collecting State Rejects New Messages
 */
class NonCollectingStateRejectsMessagesPropertyTest {
    
    @Property(tries = 10)
    @Label("Property 2: PROCESSING state rejects new messages")
    void processingStateRejectsNewMessages(
        @ForAll @IntRange(min = 2, max = 5) int initialMessageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 使用 mock 设置服务
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
        
        // 添加初始消息到媒体组（COLLECTING 状态）
        List<TdApi.Message> initialMessages = new ArrayList<>();
        for (int i = 0; i < initialMessageCount; i++) {
            TdApi.Message message = createMediaGroupMessage(1000L + i, chatId, mediaAlbumId);
            initialMessages.add(message);
            boolean accepted = service.handleMediaGroupMessage(message);
            assertThat(accepted).as("初始消息应该在COLLECTING状态下被接受").isTrue();
        }
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 验证状态是 COLLECTING
        MediaGroupState stateBefore = service.getMediaGroupState(groupKey);
        assertThat(stateBefore).isEqualTo(MediaGroupState.COLLECTING);
        
        // When: 触发超时以转换到 PROCESSING 状�?
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // Then: 状态应该是 PROCESSING �?COMPLETED
        MediaGroupState stateAfter = service.getMediaGroupState(groupKey);
        assertThat(stateAfter)
            .as("超时后状态应该是 PROCESSING �?COMPLETED")
            .isIn(MediaGroupState.PROCESSING, MediaGroupState.COMPLETED);
        
        // When: 尝试添加新消�?
        TdApi.Message newMessage = createMediaGroupMessage(9999L, chatId, mediaAlbumId);
        boolean acceptedAfterProcessing = service.handleMediaGroupMessage(newMessage);
        
        // Then: 新消息应该被拒绝
        assertThat(acceptedAfterProcessing)
            .as("当状态是" + stateAfter + "时新消息应该被拒绝")
            .isFalse();
    }
    
    @Property(tries = 10)
    @Label("Property 2: COMPLETED state rejects new messages")
    void completedStateRejectsNewMessages(
        @ForAll @IntRange(min = 2, max = 4) int initialMessageCount,
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
        
        // 添加初始消息
        for (int i = 0; i < initialMessageCount; i++) {
            TdApi.Message message = createMediaGroupMessage(1000L + i, chatId, mediaAlbumId);
            service.handleMediaGroupMessage(message);
        }
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 等待处理完成
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // 给予时间让处理完�?
        Thread.sleep(500);
        
        // Then: 状态应该是 COMPLETED
        MediaGroupState state = service.getMediaGroupState(groupKey);
        assertThat(state)
            .as("成功处理后状态应该是 COMPLETED")
            .isEqualTo(MediaGroupState.COMPLETED);
        
        // When: 尝试添加新消�?
        TdApi.Message newMessage1 = createMediaGroupMessage(8888L, chatId, mediaAlbumId);
        TdApi.Message newMessage2 = createMediaGroupMessage(9999L, chatId, mediaAlbumId);
        
        boolean accepted1 = service.handleMediaGroupMessage(newMessage1);
        boolean accepted2 = service.handleMediaGroupMessage(newMessage2);
        
        // Then: 所有新消息都应该被拒绝
        assertThat(accepted1)
            .as("第一条新消息应该在COMPLETED状态下被拒绝")
            .isFalse();
        assertThat(accepted2)
            .as("第二条新消息应该在COMPLETED状态下被拒绝")
            .isFalse();
    }
    
    @Property(tries = 8)
    @Label("Property 2: Concurrent rejection in non-COLLECTING states")
    void concurrentMessagesRejectedInNonCollectingStates(
        @ForAll @IntRange(min = 2, max = 3) int initialMessageCount,
        @ForAll @IntRange(min = 3, max = 5) int rejectedMessageCount,
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
        
        // 添加初始消息
        for (int i = 0; i < initialMessageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(1000L + i, chatId, mediaAlbumId));
        }
        
        // 转换�?PROCESSING/COMPLETED
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        String groupKey = chatId + ":" + mediaAlbumId;
        MediaGroupState state = service.getMediaGroupState(groupKey);
        
        // 验证状态不�?COLLECTING
        assertThat(state)
            .as("状态不应该�?COLLECTING")
            .isNotEqualTo(MediaGroupState.COLLECTING);
        
        // When: 尝试并发添加多条消息
        ExecutorService executor = Executors.newFixedThreadPool(rejectedMessageCount);
        CountDownLatch latch = new CountDownLatch(rejectedMessageCount);
        List<Boolean> results = new ArrayList<>();
        
        for (int i = 0; i < rejectedMessageCount; i++) {
            final long messageId = 5000L + i;
            executor.submit(() -> {
                try {
                    TdApi.Message message = createMediaGroupMessage(messageId, chatId, mediaAlbumId);
                    boolean accepted = service.handleMediaGroupMessage(message);
                    synchronized (results) {
                        results.add(accepted);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Then: 所有并发消息都应该被拒绝
        assertThat(results)
            .as("在" + state + "状态下所有消息都应该被拒绝")
            .hasSize(rejectedMessageCount)
            .allMatch(accepted -> !accepted);
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
