package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.PhotoMessageEntity;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

/**
 * Property 4: Concurrent Message Collection Integrity
 * 
 * 对于任何媒体组，当多条消息并发到达时，所有消息应该被收集到同一个缓冲区而不丢失
 * 
 * **验证需求: Requirements 1.1**
 * 
 * Feature: concurrent-safety-optimization, Property 4: Concurrent Message Collection Integrity
 */
class ConcurrentMessageCollectionPropertyTest {
    
    @Property(tries = 20)
    @Label("Property 4: Concurrent Message Collection Integrity - All concurrent messages collected")
    void concurrentMessagesAreCollectedCompletely(
        @ForAll @IntRange(min = 2, max = 5) int messageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 创建mock依赖
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        DuplicateMessageFilter duplicateMessageFilter = mock(DuplicateMessageFilter.class);
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
        
        // Mock filter chain接受所有消息
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(chatId)).thenReturn(true);
        
        // Mock channel repository
        Channel channel = new Channel();
        channel.setChannelId(chatId);
        channel.setChannelUsername("test_channel");
        when(channelRepository.findByChannelId(chatId)).thenReturn(Optional.of(channel));
        
        // Mock message storage
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
        // Mock message parser
        when(messageParser.parseMediaGroupItem(any(), any(), any())).thenAnswer(invocation -> {
            TdApi.Message msg = invocation.getArgument(0);
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(msg.id);
            entity.setChatId(msg.chatId);
            entity.setMediaAlbumId(msg.mediaAlbumId);
            return entity;
        });
        
        // 创建媒体组消息
        List<TdApi.Message> messages = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            long messageId = 1000L + i;
            TdApi.Message message = createMediaGroupMessage(messageId, chatId, mediaAlbumId);
            messages.add(message);
        }
        
        // When: 并发处理消息
        ExecutorService executor = Executors.newFixedThreadPool(messageCount);
        CountDownLatch latch = new CountDownLatch(messageCount);
        
        for (TdApi.Message message : messages) {
            executor.submit(() -> {
                try {
                    service.handleMediaGroupMessage(message);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Then: 验证所有消息都在缓冲区中
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // Property 1: 状态应该是COLLECTING
        MediaGroupState state = service.getMediaGroupState(groupKey);
        assertThat(state)
            .as("Media group state should be COLLECTING")
            .isEqualTo(MediaGroupState.COLLECTING);
        
        // Property 2: 所有消息应该被接受（没有拒绝）
        // 我们通过检查所有handleMediaGroupMessage调用返回true来验证这一点
        // 由于我们正在测试属性，我们检查最终状态
        
        // 等待超时并处理
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // Property 3: Plugin manager应该被调用一次，处理所有消息
        verify(pluginManager, times(1)).process(any(BaseMessageEntity.class), any(TdApi.Message.class));
        
        // Property 4: 所有消息应该被保存
        verify(messageStorageService, times(messageCount)).saveMessage(any(TdApi.Message.class));
    }
    
    @Property(tries = 15)
    @Label("Property 4: Concurrent Message Collection Integrity - No message loss during concurrent access")
    void noMessageLossDuringConcurrentAccess(
        @ForAll @IntRange(min = 3, max = 5) int messageCount,
        @ForAll @IntRange(min = 1000, max = 9999) long mediaAlbumId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 使用mock设置service
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
        
        MediaGroupMetrics mediaGroupMetrics = mock(MediaGroupMetrics.class);
        DuplicateMessageFilter duplicateMessageFilter = mock(DuplicateMessageFilter.class);
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
        
        // 创建消息
        List<TdApi.Message> messages = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            messages.add(createMediaGroupMessage(1000L + i, chatId, mediaAlbumId));
        }
        
        // When: 在高竞争下并发添加消息
        ExecutorService executor = Executors.newFixedThreadPool(messageCount);
        CountDownLatch latch = new CountDownLatch(messageCount);
        List<Boolean> results = new ArrayList<>();
        
        for (TdApi.Message message : messages) {
            executor.submit(() -> {
                try {
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
        
        // Then: 所有消息应该被接受
        assertThat(results)
            .as("All messages should be accepted during COLLECTING state")
            .hasSize(messageCount)
            .allMatch(accepted -> accepted);
        
        // 等待并处理
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // 验证所有消息都被保存
        verify(messageStorageService, times(messageCount)).saveMessage(any(TdApi.Message.class));
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
