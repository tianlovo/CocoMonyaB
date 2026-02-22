package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmptyMessageFilter单元测试
 */
class EmptyMessageFilterTest {
    
    private EmptyMessageFilter filter;
    private FilterContext context;
    
    @BeforeEach
    void setUp() {
        filter = new EmptyMessageFilter();
        context = new FilterContext();
    }
    
    @Test
    void testAcceptNonEmptyTextMessage() {
        // Given: 非空文本消息
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Hello World", new TdApi.TextEntity[0]), 
            null, 
            null
        );
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该接受
        assertEquals(FilterResult.ACCEPT, result);
    }
    
    @Test
    void testRejectEmptyTextMessage() {
        // Given: 空文本消息
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("", new TdApi.TextEntity[0]), 
            null, 
            null
        );
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该拒绝
        assertEquals(FilterResult.REJECT, result);
        assertEquals("Text message is empty", context.getRejectReason());
    }
    
    @Test
    void testRejectWhitespaceOnlyMessage() {
        // Given: 只有空格的文本消息
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("   ", new TdApi.TextEntity[0]), 
            null, 
            null
        );
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该拒绝
        assertEquals(FilterResult.REJECT, result);
        assertEquals("Text message is empty", context.getRejectReason());
    }
    
    @Test
    void testRejectNullContentMessage() {
        // Given: 内容为null的消息
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.content = null;
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该拒绝
        assertEquals(FilterResult.REJECT, result);
        assertEquals("Message content is null", context.getRejectReason());
    }
    
    @Test
    void testAcceptNonTextMessage() {
        // Given: 非文本消息（如图片）
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
            new TdApi.FormattedText("Caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        
        // When: 过滤消息
        FilterResult result = filter.filter(message, context);
        
        // Then: 应该接受（非文本消息不检查空内容）
        assertEquals(FilterResult.ACCEPT, result);
    }
    
    @Test
    void testFilterProperties() {
        // Then: 验证过滤器属性
        assertEquals("EmptyMessageFilter", filter.getName());
        assertEquals(100, filter.getPriority());
        assertTrue(filter.isEnabled());
    }
}
