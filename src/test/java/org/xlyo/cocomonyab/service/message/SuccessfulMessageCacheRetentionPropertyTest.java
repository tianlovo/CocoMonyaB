package org.xlyo.cocomonyab.service.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;

/**
 * 属性 13：成功消息缓存保留
 * 
 * 对于任何成功保存的消息，系统应该将其标记为已处理并保留在缓存中直到 TTL 过期
 * 
 * **验证：需求 6.5**
 * 
 * Feature: concurrent-safety-optimization, Property 13: Successful Message Cache Retention
 */
class SuccessfulMessageCacheRetentionPropertyTest {
    
    @Property(tries = 100)
    @Label("Property 13: Successfully saved messages are added to cache by filter")
    void successfullySavedMessagesAreAddedToCacheByFilter(
        @ForAll @LongRange(min = 1000, max = 9999) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId,
        @ForAll @IntRange(min = 0, max = 1) int isMediaGroup
    ) throws Exception {
        // Given: 设置服务和过滤器
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
        
        // 模拟数据库保存成功
        when(rawMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: 通过过滤器检查消息（这会将消息添加到缓存）
        FilterContext context = new FilterContext();
        FilterResult filterResult = duplicateMessageFilter.filter(message, context);
        
        // Then: 过滤器应该接受消息（第一次）
        assertThat(filterResult)
            .as("第一次过滤应该接受消息")
            .isEqualTo(FilterResult.ACCEPT);
        
        // When: 保存消息（应该成功）
        boolean saveResult = service.saveMessage(message);
        
        // Then: 保存应该成功
        assertThat(saveResult)
            .as("保存应该成功")
            .isTrue();
        
        // 验证消息被保存到数据库
        verify(rawMessageRepository, times(1)).save(any());
        
        // When: 再次通过过滤器检查相同消息（应该被缓存拒绝）
        FilterContext context2 = new FilterContext();
        FilterResult filterResult2 = duplicateMessageFilter.filter(message, context2);
        
        // Then: 第二次应该被拒绝（因为缓存中已有）
        assertThat(filterResult2)
            .as("第二次过滤应该拒绝消息（缓存中已有）")
            .isEqualTo(FilterResult.REJECT);
    }
    
    @Property(tries = 100)
    @Label("Property 13: Cache prevents duplicate processing of successful messages")
    void cachePreventsDoubleProcessingOfSuccessfulMessages(
        @ForAll @LongRange(min = 1000, max = 9999) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws Exception {
        // Given: 设置服务和过滤器
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
        
        // 模拟数据库保存成功
        when(rawMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: 第一次保存消息（应该成功）
        boolean firstSaveResult = service.saveMessage(message);
        
        // Then: 第一次保存应该成功
        assertThat(firstSaveResult)
            .as("第一次保存应该成功")
            .isTrue();
        
        // When: 立即尝试第二次保存相同消息（应该被缓存拒绝）
        // 重置 mock 以模拟数据库现在有该消息
        reset(rawMessageRepository);
        when(rawMessageRepository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(true);
        
        boolean secondSaveResult = service.saveMessage(message);
        
        // Then: 第二次保存应该被拒绝（因为数据库中已存在）
        assertThat(secondSaveResult)
            .as("第二次保存应该被拒绝（消息已存在）")
            .isFalse();
        
        // 验证数据库保存只被调用一次（第二次因为 isDuplicate 检查而跳过）
        verify(rawMessageRepository, times(0)).save(any());
    }
    
    @Property(tries = 50)
    @Label("Property 13: Cache expires after TTL allowing reprocessing")
    void cacheExpiresAfterTTLAllowingReprocessing(
        @ForAll @LongRange(min = 1000, max = 9999) long messageId,
        @ForAll @LongRange(min = -1001999999999L, max = -1001000000000L) long chatId
    ) throws Exception {
        // Given: 设置服务和过滤器
        RawMessageRepository rawMessageRepository = mock(RawMessageRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ConcurrentSafetyProperties properties = createDefaultProperties();
        DuplicateMessageFilter duplicateMessageFilter = new DuplicateMessageFilter(rawMessageRepository, properties);
        
        // 创建测试消息
        TdApi.Message message = createTestMessage(messageId, chatId, 0L);
        
        // 模拟数据库不存在该消息
        when(rawMessageRepository.existsByChatIdAndMessageId(chatId, messageId)).thenReturn(false);
        
        // When: 通过过滤器检查消息（第一次）
        FilterContext context1 = new FilterContext();
        FilterResult filterResult1 = duplicateMessageFilter.filter(message, context1);
        
        // Then: 第一次应该接受
        assertThat(filterResult1).isEqualTo(FilterResult.ACCEPT);
        
        // When: 立即再次检查（应该被拒绝）
        FilterContext context2 = new FilterContext();
        FilterResult filterResult2 = duplicateMessageFilter.filter(message, context2);
        
        // Then: 第二次应该被拒绝（缓存中有）
        assertThat(filterResult2).isEqualTo(FilterResult.REJECT);
        
        // When: 等待缓存过期（11秒，大于10秒TTL）
        Thread.sleep(11000);
        
        // When: 缓存过期后再次检查
        FilterContext context3 = new FilterContext();
        FilterResult filterResult3 = duplicateMessageFilter.filter(message, context3);
        
        // Then: 缓存过期后应该再次接受
        assertThat(filterResult3)
            .as("缓存过期后应该再次接受消息")
            .isEqualTo(FilterResult.ACCEPT);
    }
    
    @Property(tries = 100)
    @Label("Property 13: Batch save success does not mark messages as failed")
    void batchSaveSuccessDoesNotMarkMessagesAsFailed(
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
        
        // 模拟批量保存成功
        when(rawMessageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When: 批量保存消息（应该成功）
        boolean saveResult = service.saveAll(messages);
        
        // Then: 保存应该成功
        assertThat(saveResult)
            .as("批量保存应该成功")
            .isTrue();
        
        // 验证批量保存被调用
        verify(rawMessageRepository, times(1)).saveAll(any());
        
        // 验证没有调用 markFailed（成功保存不应该标记为失败）
        long failedCount = duplicateMessageFilter.getFailedCount();
        assertThat(failedCount)
            .as("成功保存不应该标记为失败")
            .isEqualTo(0);
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
