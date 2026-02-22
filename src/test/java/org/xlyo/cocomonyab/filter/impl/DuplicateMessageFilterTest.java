package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        filter = new DuplicateMessageFilter(rawMessageRepository);
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
        assertTrue(context.getRejectReason().contains("Duplicate message in database"));
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
        assertTrue(context.getRejectReason().contains("Duplicate media group in database"));
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
    void testMarkProcessedRemovesFromCache() {
        // Given: 消息通过过滤并在处理中
        TdApi.Message message = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        
        filter.filter(message, new FilterContext());
        assertEquals(1, filter.getProcessingCount());
        
        // When: 标记为已处理
        filter.markProcessed(message);
        
        // Then: 从缓存中移除
        assertEquals(0, filter.getProcessingCount());
        
        // 相同消息再次到达应该检查数据库
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(true);
        FilterResult result = filter.filter(message, new FilterContext());
        assertEquals(FilterResult.REJECT, result);
    }
    
    @Test
    void testMarkFailedRemovesFromCache() {
        // Given: 消息通过过滤但保存失败
        TdApi.Message message = createSingleMessage(123L, 456L);
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        
        filter.filter(message, new FilterContext());
        assertEquals(1, filter.getProcessingCount());
        
        // When: 标记为失败
        filter.markFailed(message);
        
        // Then: 从缓存中移除，允许重试
        assertEquals(0, filter.getProcessingCount());
        
        // 相同消息再次到达应该能通过（允许重试）
        FilterResult result = filter.filter(message, new FilterContext());
        assertEquals(FilterResult.ACCEPT, result);
        
        // 清理
        filter.markProcessed(message);
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
