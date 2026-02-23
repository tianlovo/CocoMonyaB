package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;

/**
 * DuplicateMessageFilter单元测试
 */
@ExtendWith(MockitoExtension.class)
class DuplicateMessageFilterTest {
    
    @Mock
    private RawMessageRepository rawMessageRepository;
    
    private DuplicateMessageFilter filter;
    private FilterContext context;
    
    @BeforeEach
    void setUp() {
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        properties.getMediaGroup().setTimeout(2000);
        properties.getMediaGroup().setMaxBufferSize(1000);
        properties.getLock().setStripes(128);
        properties.getLock().setTimeout(5000);
        properties.getCache().setTtl(10);
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(5);
        
        filter = new DuplicateMessageFilter(rawMessageRepository, properties);
        context = new FilterContext();
    }
    
    @Test
    void testAcceptNewSingleMessage() {
        // Given: 新的单条消息（数据库中不存在）
        TdApi.Message message = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该接受
        assertEquals(FilterResult.ACCEPT, result);
        verify(rawMessageRepository).existsByChatIdAndMessageId(456L, 123L);
        
        // 清理缓存
        filter.markProcessed(message);
    }
    
    @Test
    void testRejectDuplicateSingleMessage() {
        // Given: 重复的单条消息（数据库中已存在）
        TdApi.Message message = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(true);
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该拒绝
        assertEquals(FilterResult.REJECT, result);
        assertTrue(context.getRejectReason().contains("数据库中存在重复消息"));
        assertTrue(context.getRejectReason().contains("chatId=456"));
        assertTrue(context.getRejectReason().contains("messageId=123"));
        verify(rawMessageRepository).existsByChatIdAndMessageId(456L, 123L);
    }
    
    @Test
    void testRejectConcurrentDuplicateSingleMessage() {
        // Given: 相同消息并发到达
        TdApi.Message message1 = createSingleMessage(123L, 456L);
        TdApi.Message message2 = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        
        // When: 第一个消息通过过滤
        FilterResult result1 = filter.filter(message1, new FilterContext());
        
        // When: 第二个相同消息立即到达（还在处理中）
        FilterResult result2 = filter.filter(message2, new FilterContext());
        
        // Then: 第一个接受，第二个被内存缓存拒绝
        assertEquals(FilterResult.ACCEPT, result1);
        assertEquals(FilterResult.REJECT, result2);
        
        // 清理缓存
        filter.markProcessed(message1);
    }
    
    @Test
    void testAcceptNewMediaGroupMessage() {
        // Given: 新的媒体组消息（数据库中不存在）
        TdApi.Message message = createMediaGroupMessage(123L, 456L, 789L);
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(456L, 789L)).thenReturn(false);
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该接受
        assertEquals(FilterResult.ACCEPT, result);
        verify(rawMessageRepository).existsByChatIdAndMediaAlbumId(456L, 789L);
        verify(rawMessageRepository, never()).existsByChatIdAndMessageId(anyLong(), anyLong());
        
        // 清理缓存
        filter.markProcessed(message);
    }
    
    @Test
    void testRejectDuplicateMediaGroupMessage() {
        // Given: 重复的媒体组消息（数据库中已存在）
        TdApi.Message message = createMediaGroupMessage(123L, 456L, 789L);
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(456L, 789L)).thenReturn(true);
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该拒绝
        assertEquals(FilterResult.REJECT, result);
        assertTrue(context.getRejectReason().contains("数据库中存在重复媒体组"));
        assertTrue(context.getRejectReason().contains("chatId=456"));
        assertTrue(context.getRejectReason().contains("mediaAlbumId=789"));
        verify(rawMessageRepository).existsByChatIdAndMediaAlbumId(456L, 789L);
        verify(rawMessageRepository, never()).existsByChatIdAndMessageId(anyLong(), anyLong());
    }
    
    @Test
    void testRejectConcurrentDuplicateMediaGroup() {
        // Given: 相同媒体组的多个消息并发到达
        TdApi.Message message1 = createMediaGroupMessage(123L, 456L, 789L);
        TdApi.Message message2 = createMediaGroupMessage(124L, 456L, 789L); // 同一媒体组
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(456L, 789L)).thenReturn(false);
        
        // When: 第一个消息通过过滤
        FilterResult result1 = filter.filter(message1, new FilterContext());
        
        // When: 第二个相同媒体组的消息立即到达
        FilterResult result2 = filter.filter(message2, new FilterContext());
        
        // Then: 第一个接受，第二个被内存缓存拒绝
        assertEquals(FilterResult.ACCEPT, result1);
        assertEquals(FilterResult.REJECT, result2);
        
        // 清理缓存
        filter.markProcessed(message1);
    }
    
    @Test
    void testFilterProperties() {
        // Then: 验证过滤器属性
        assertEquals("DuplicateMessageFilter", filter.getName());
        assertEquals(95, filter.getPriority());
        assertTrue(filter.isEnabled());
    }
    
    @Test
    void testMarkProcessedIsDeprecated() {
        // Given: 消息通过过滤并在处理中
        TdApi.Message message = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        
        filter.filter(message, new FilterContext());
        long initialCount = filter.getProcessingCount();
        assertThat(initialCount).isGreaterThan(0);
        
        // When: 标记为已处理（已废弃的方法）
        @SuppressWarnings("deprecation")
        boolean deprecated = true;
        if (deprecated) {
            filter.markProcessed(message);
        }
        
        // Then: 缓存不会立即移除（使用 Caffeine 后自动过期）
        // 缓存会在 10 秒后自动过期
        long afterMarkCount = filter.getProcessingCount();
        assertThat(afterMarkCount).isGreaterThan(0);
        
        // 相同消息再次到达应该被缓存拒绝
        FilterResult result = filter.filter(message, new FilterContext());
        assertEquals(FilterResult.REJECT, result);
    }
    
    @Test
    void testMarkFailedAddsToFailedCache() {
        // Given: 消息通过过滤但保存失败
        TdApi.Message message = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        
        filter.filter(message, new FilterContext());
        long initialProcessingCount = filter.getProcessingCount();
        assertThat(initialProcessingCount).isGreaterThan(0);
        
        // When: 标记为失败
        filter.markFailed(message);
        
        // Then: 添加到失败缓存
        long failedCount = filter.getFailedCount();
        assertThat(failedCount).isGreaterThan(0);
        
        // And: 处理缓存仍然保留（不会立即移除）
        long afterFailCount = filter.getProcessingCount();
        assertThat(afterFailCount).isGreaterThan(0);
        
        // 相同消息再次到达应该被处理缓存拒绝
        FilterResult result = filter.filter(message, new FilterContext());
        assertEquals(FilterResult.REJECT, result);
    }
    
    @Test
    void testClearCache() {
        // Given: 多个消息在处理中
        TdApi.Message message1 = createSingleMessage(100L, 456L);
        TdApi.Message message2 = createSingleMessage(200L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(anyLong(), anyLong())).thenReturn(false);
        
        filter.filter(message1, new FilterContext());
        filter.filter(message2, new FilterContext());
        assertEquals(2, filter.getProcessingCount());
        
        // When: 清空缓存
        filter.clearCache();
        
        // Then: 缓存被清空
        assertEquals(0, filter.getProcessingCount());
    }
    
    @Test
    void testSameMessageIdDifferentChat() {
        // Given: 相同消息ID但不同频道
        TdApi.Message message1 = createSingleMessage(123L, 456L);
        TdApi.Message message2 = createSingleMessage(123L, 789L);
        
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(true);
        when(rawMessageRepository.existsByChatIdAndMessageId(789L, 123L)).thenReturn(false);
        
        // When: 过滤消息
        FilterResult result1 = filter.filter(message1, new FilterContext());
        FilterResult result2 = filter.filter(message2, new FilterContext());
        
        // Then: 第一个拒绝，第二个接受
        assertEquals(FilterResult.REJECT, result1);
        assertEquals(FilterResult.ACCEPT, result2);
        
        // 清理
        filter.markProcessed(message2);
    }
    
    // 辅助方法
    
    private TdApi.Message createSingleMessage(long messageId, long chatId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.mediaAlbumId = 0; // 单条消息
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]), 
            null, 
            null
        );
        return message;
    }
    
    private TdApi.Message createMediaGroupMessage(long messageId, long chatId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.mediaAlbumId = mediaAlbumId; // 媒体组消息
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
            new TdApi.FormattedText("Caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }
}
