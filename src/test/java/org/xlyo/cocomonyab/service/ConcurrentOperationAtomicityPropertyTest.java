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
 * 属�?3：并发操作原子�?
 * 
 * 对于任何媒体组，当多个线程并发访问时，状态检查、状态更新和数据操作应该是原子的�?
 * 不会出现中间状态或数据不一�?
 * 
 * **验证：需�?4.1, 4.2, 4.3, 4.5**
 * 
 * Feature: concurrent-safety-optimization, Property 3: Concurrent Operation Atomicity
 */
class ConcurrentOperationAtomicityPropertyTest {
    
    @Property(tries = 20)
    @Label("Property 3: State transitions are atomic - only one thread succeeds")
    void stateTransitionsAreAtomic(
        @ForAll @IntRange(min = 5, max = 10) int threadCount,
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
        
        // 添加一条消息以初始化媒体组
        TdApi.Message initialMessage = createMediaGroupMessage(1000L, chatId, mediaAlbumId);
        service.handleMediaGroupMessage(initialMessage);
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // 验证初始状态为 COLLECTING
        assertThat(service.getMediaGroupState(groupKey))
            .isEqualTo(MediaGroupState.COLLECTING);
        
        // When: 多个线程尝试并发添加消息
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> acceptResults = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            final long messageId = 2000L + i;
            executor.submit(() -> {
                try {
                    boolean accepted = service.handleMediaGroupMessage(
                        createMediaGroupMessage(messageId, chatId, mediaAlbumId)
                    );
                    synchronized (acceptResults) {
                        acceptResults.add(accepted);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Then: �?COLLECTING 状态下所有消息都应该被接�?
        assertThat(acceptResults)
            .as("All concurrent additions should succeed in COLLECTING state")
            .hasSize(threadCount)
            .allMatch(accepted -> accepted);
        
        // 状态应该仍然是 COLLECTING
        assertThat(service.getMediaGroupState(groupKey))
            .as("State should remain COLLECTING after concurrent additions")
            .isEqualTo(MediaGroupState.COLLECTING);
    }
    
    @Property(tries = 15)
    @Label("Property 3: Concurrent state check and update are atomic")
    void concurrentStateCheckAndUpdateAreAtomic(
        @ForAll @IntRange(min = 3, max = 5) int messageCount,
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
        
        // 并发添加消息以测试缓冲区操作的原子�?
        ExecutorService executor = Executors.newFixedThreadPool(messageCount);
        CountDownLatch latch = new CountDownLatch(messageCount);
        List<Boolean> results = new ArrayList<>();
        
        for (int i = 0; i < messageCount; i++) {
            final long messageId = 1000L + i;
            executor.submit(() -> {
                try {
                    boolean accepted = service.handleMediaGroupMessage(
                        createMediaGroupMessage(messageId, chatId, mediaAlbumId)
                    );
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
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // Then: 所有消息都应该被接受（原子缓冲区操作）
        assertThat(results)
            .as("All concurrent messages should be accepted atomically")
            .hasSize(messageCount)
            .allMatch(accepted -> accepted);
        
        // 状态应该是 COLLECTING
        assertThat(service.getMediaGroupState(groupKey))
            .as("State should be COLLECTING after concurrent additions")
            .isEqualTo(MediaGroupState.COLLECTING);
        
        // 处理�?
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // 所有消息应该被精确保存一�?
        verify(messageStorageService, times(messageCount)).saveMessage(any(TdApi.Message.class));
        
        // 最终状态应该是 COMPLETED
        assertThat(service.getMediaGroupState(groupKey))
            .as("Final state should be COMPLETED")
            .isEqualTo(MediaGroupState.COMPLETED);
    }
    
    @Property(tries = 15)
    @Label("Property 3: Buffer operations are atomic under concurrent access")
    void bufferOperationsAreAtomicUnderConcurrentAccess(
        @ForAll @IntRange(min = 5, max = 8) int threadCount,
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
        
        // When: 多个线程在高竞争下添加消�?
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final long messageId = 1000L + i;
            executor.submit(() -> {
                try {
                    // 添加小延迟以增加竞争
                    Thread.sleep((long) (Math.random() * 10));
                    service.handleMediaGroupMessage(
                        createMediaGroupMessage(messageId, chatId, mediaAlbumId)
                    );
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 处理媒体�?
        Thread.sleep(2500);
        service.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        // Then: 所有消息应该被精确保存一次（无重复，无丢失）
        verify(messageStorageService, times(threadCount)).saveMessage(any(TdApi.Message.class));
        
        // Plugin manager 应该被精确调用一次，处理完整的组
        verify(pluginManager, times(1)).process(any(), any(TdApi.Message.class));
    }
    
    @Property(tries = 12)
    @Label("Property 3: No intermediate states visible during transitions")
    void noIntermediateStatesVisibleDuringTransitions(
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
        
        // 添加消息
        for (int i = 0; i < messageCount; i++) {
            service.handleMediaGroupMessage(createMediaGroupMessage(1000L + i, chatId, mediaAlbumId));
        }
        
        String groupKey = chatId + ":" + mediaAlbumId;
        
        // When: 在转换期间监控状�?
        List<MediaGroupState> observedStates = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        
        // 线程 1: 持续观察状�?
        executor.submit(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    MediaGroupState state = service.getMediaGroupState(groupKey);
                    if (state != null) {
                        synchronized (observedStates) {
                            observedStates.add(state);
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // 线程 2: 延迟后触发状态转�?
        executor.submit(() -> {
            try {
                Thread.sleep(2500);
                service.processTimedOutMediaGroups();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        latch.await();
        executor.shutdown();
        
        // Then: 所有观察到的状态都应该是有效的（COLLECTING、PROCESSING �?COMPLETED�?
        // 没有 null 或不一致的状�?
        assertThat(observedStates)
            .as("All observed states should be valid")
            .allMatch(state -> 
                state == MediaGroupState.COLLECTING ||
                state == MediaGroupState.PROCESSING ||
                state == MediaGroupState.COMPLETED
            );
        
        // 状态转换应该遵循有效序�?
        // COLLECTING -> PROCESSING -> COMPLETED
        for (int i = 1; i < observedStates.size(); i++) {
            MediaGroupState prev = observedStates.get(i - 1);
            MediaGroupState curr = observedStates.get(i);
            
            // 有效转换
            boolean validTransition = 
                (prev == MediaGroupState.COLLECTING && curr == MediaGroupState.COLLECTING) ||
                (prev == MediaGroupState.COLLECTING && curr == MediaGroupState.PROCESSING) ||
                (prev == MediaGroupState.PROCESSING && curr == MediaGroupState.PROCESSING) ||
                (prev == MediaGroupState.PROCESSING && curr == MediaGroupState.COMPLETED) ||
                (prev == MediaGroupState.COMPLETED && curr == MediaGroupState.COMPLETED);
            
            assertThat(validTransition)
                .as("Transition from " + prev + " to " + curr + " should be valid")
                .isTrue();
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
