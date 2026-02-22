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

/**
 * 属性 1：成功处理后状态转换
 * 
 * 对于任何成功处理的媒体组，其最终状态应该是 COMPLETED
 * 
 * **验证：需求 1.4, 2.4**
 * 
 * Feature: concurrent-safety-optimization, Property 1: Successful Processing State Transition
 */
class SuccessfulProcessingStateTransitionPropertyTest {
    
    @Property(tries = 15)
    @Label("Property 1: Successful processing transitions to COMPLETED")
    void successfulProcessingTransitionsToCompleted(
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
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter
        );
        
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
        
        // When: 等待超时并处理
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        
        // 给予时间让处理完成
        Thread.sleep(500);
        
        // Then: 状态应该是 COMPLETED
        MediaGroupState finalState = service.getMediaGroupState(groupKey);
        assertThat(finalState)
            .as("成功处理后状态应该是 COMPLETED")
            .isEqualTo(MediaGroupState.COMPLETED);
    }
    
    @Property(tries = 12)
    @Label("Property 1: COMPLETED state persists after processing")
    void completedStatePersistsAfterProcessing(
        @ForAll @IntRange(min = 2, max = 4) int messageCount,
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
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter
        );
        
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
        
        // 添加消息并处理
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(1000L + i, chatId, mediaAlbumId));
        }
        
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 验证状态是 COMPLETED
        MediaGroupState state1 = service.getMediaGroupState(groupKey);
        assertThat(state1)
            .as("处理后状态应该是 COMPLETED")
            .isEqualTo(MediaGroupState.COMPLETED);
        
        // When: 再次调用定时任务
        service.processTimedOutMediaGroups();
        
        // Then: 状态应该保持 COMPLETED
        MediaGroupState state2 = service.getMediaGroupState(groupKey);
        assertThat(state2)
            .as("COMPLETED 状态应该持久保持")
            .isEqualTo(MediaGroupState.COMPLETED);
    }
    
    @Property(tries = 10)
    @Label("Property 1: Multiple groups complete independently")
    void multipleGroupsCompleteIndependently(
        @ForAll @IntRange(min = 2, max = 3) int groupCount,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws InterruptedException {
        // Given: 设置服务
        ChannelRepository channelRepository = mock(ChannelRepository.class);
        MessageStorageService messageStorageService = mock(MessageStorageService.class);
        MessageParser messageParser = mock(MessageParser.class);
        PluginManager pluginManager = mock(PluginManager.class);
        FilterChainManager filterChainManager = mock(FilterChainManager.class);
        ChannelMonitoringFilter channelMonitoringFilter = mock(ChannelMonitoringFilter.class);
        
        ChannelMonitorService service = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter
        );
        
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
        
        // 创建多个媒体组
        for (int i = 0; i < groupCount; i++) {
            long mediaAlbumId = 1000L + i;
            TdApi.Message message = createMediaGroupMessage(1000L, chatId, mediaAlbumId);
            service.handleMediaGroupMessage(message);
        }
        
        // 等待所有组超时并处理
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // Then: 验证所有组都是 COMPLETED 状态
        for (int i = 0; i < groupCount; i++) {
            String groupKey = chatId + ":" + (1000L + i);
            MediaGroupState state = service.getMediaGroupState(groupKey);
            assertThat(state)
                .as("媒体组 " + groupKey + " 应该是 COMPLETED 状态")
                .isEqualTo(MediaGroupState.COMPLETED);
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
}
