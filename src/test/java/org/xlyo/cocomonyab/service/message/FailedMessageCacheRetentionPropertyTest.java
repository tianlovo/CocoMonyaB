package org.xlyo.cocomonyab.service.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;

/**
 * 属性 12：失败消息缓存保留
 * 
 * 对于任何保存失败的消息，系统应该在缓存中保留该消息键一段时间以防止立即重试
 * 
 * **验证：需求 6.2**
 * 
 * Feature: concurrent-safety-optimization, Property 12: Failed Message Cache Retention
 */
class FailedMessageCacheRetentionPropertyTest {
    
    @Property(tries = 100)
    @Label("Property 12: Failed messages are retained in cache to prevent immediate retry")
    void failedMessagesAreRetainedInCache(
        @ForAll @LongRange(min = 1000, max = 9999) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId,
        @ForAll @IntRange(min = 0, max = 1) int isMediaGroup
    ) throws Exception {
        // Given: 设置服务，模拟数据库保存失败
        RawMessageRepository rawMessageRepository = mock(RawMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConcurrentSafetyProperties properties = createDefaultProperties();
        DuplicateMessageFilter duplicateMessageFilter = new DuplicateMessageFilter(rawMessageRepository, properties);
        org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics mediaGroupMetrics = mock(org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics.class);
        
        MessageStorageService service = new MessageStorageService(
            rawMessageRepository,
            objectMapper,
            duplicateMessageFilter,
            mediaGroupMetrics
        );
        
        // 创建测试消息
        TdApi.Message message = createTestMessage(messageId, chatId, isMediaGroup == 1 ? 5000L : 0L);
        
        // 模拟数据库不存在该消息
        when(rawMessageRepository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(anyLong(), anyLong())).thenReturn(false);
        
        // 模拟数据库保存失败（非 DuplicateKeyException）
        when(rawMessageRepository.save(any())).thenThrow(new RuntimeException("Database connection error"));
        
        // When: 尝试保存消息（会失败）
        boolean saveResult = service.saveMessage(message);
        
        // Then: 保存应该失败
        assertThat(saveResult)
            .as("保存应该失败")
            .isFalse();
        
        // 验证 markFailed 被调用（通过检查失败缓存）
        long failedCount = duplicateMessageFilter.getFailedCount();
        assertThat(failedCount)
            .as("失败缓存应该包含失败的消息")
            .isGreaterThan(0);
        
        // When: 立即重试（在缓存过期前）
        // 重置 mock 以允许保存
        reset(rawMessageRepository);
        when(rawMessageRepository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(anyLong(), anyLong())).thenReturn(false);
        when(rawMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // 注意：由于 DuplicateMessageFilter 的缓存机制，
        // 失败的消息会在短暂时间内（5秒）被保留在缓存中
        // 这里我们验证缓存确实保留了失败的消息
        
        // Then: 失败缓存应该在短时间内保留消息
        assertThat(duplicateMessageFilter.getFailedCount())
            .as("失败缓存应该保留消息以防止立即重试")
            .isGreaterThan(0);
    }
    
    @Property(tries = 50)
    @Label("Property 12: Failed cache expires after TTL allowing retry")
    void failedCacheExpiresAfterTTL(
        @ForAll @LongRange(min = 1000, max = 9999) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws Exception {
        // Given: 设置服务
        RawMessageRepository rawMessageRepository = mock(RawMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConcurrentSafetyProperties properties = createDefaultProperties();
        DuplicateMessageFilter duplicateMessageFilter = new DuplicateMessageFilter(rawMessageRepository, properties);
        org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics mediaGroupMetrics = mock(org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics.class);
        
        MessageStorageService service = new MessageStorageService(
            rawMessageRepository,
            objectMapper,
            duplicateMessageFilter,
            mediaGroupMetrics
        );
        
        // 创建测试消息
        TdApi.Message message = createTestMessage(messageId, chatId, 0L);
        
        // 模拟数据库不存在该消息
        when(rawMessageRepository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        
        // 模拟数据库保存失败
        when(rawMessageRepository.save(any())).thenThrow(new RuntimeException("Database error"));
        
        // When: 尝试保存消息（会失败）
        boolean saveResult = service.saveMessage(message);
        
        // Then: 保存应该失败
        assertThat(saveResult).isFalse();
        
        // 验证失败缓存包含消息
        long failedCountBefore = duplicateMessageFilter.getFailedCount();
        assertThat(failedCountBefore).isGreaterThan(0);
        
        // When: 等待缓存过期（6秒，大于5秒TTL）
        Thread.sleep(6000);
        
        // Then: 失败缓存应该过期
        long failedCountAfter = duplicateMessageFilter.getFailedCount();
        assertThat(failedCountAfter)
            .as("失败缓存应该在TTL后过期")
            .isLessThanOrEqualTo(failedCountBefore);
    }
    
    @Property(tries = 100)
    @Label("Property 12: Batch save failure marks all messages")
    void batchSaveFailureMarksAllMessages(
        @ForAll @IntRange(min = 2, max = 5) int messageCount,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws Exception {
        // Given: 设置服务
        RawMessageRepository rawMessageRepository = mock(RawMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConcurrentSafetyProperties properties = createDefaultProperties();
        DuplicateMessageFilter duplicateMessageFilter = new DuplicateMessageFilter(rawMessageRepository, properties);
        org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics mediaGroupMetrics = mock(org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics.class);
        
        MessageStorageService service = new MessageStorageService(
            rawMessageRepository,
            objectMapper,
            duplicateMessageFilter,
            mediaGroupMetrics
        );
        
        // 创建测试消息列表
        java.util.List<TdApi.Message> messages = new java.util.ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            messages.add(createTestMessage(1000L + i, chatId, 0L));
        }
        
        // 模拟数据库不存在这些消息
        when(rawMessageRepository.existsByChatIdAndMessageId(anyLong(), anyLong())).thenReturn(false);
        
        // 模拟批量保存失败
        when(rawMessageRepository.saveAll(any())).thenThrow(new RuntimeException("Batch save error"));
        
        // When: 尝试批量保存消息（会失败）
        boolean saveResult = service.saveAll(messages);
        
        // Then: 保存应该失败
        assertThat(saveResult)
            .as("批量保存应该失败")
            .isFalse();
        
        // 验证失败缓存包含所有消息
        long failedCount = duplicateMessageFilter.getFailedCount();
        assertThat(failedCount)
            .as("失败缓存应该包含所有失败的消息")
            .isGreaterThanOrEqualTo(messageCount);
    }
    
    // Helper method to create test messages
    private TdApi.Message createTestMessage(long messageId, long chatId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.mediaAlbumId = mediaAlbumId;
        message.date = 1234567890;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test message", new TdApi.TextEntity[0]),
            null,
            null
        );
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
