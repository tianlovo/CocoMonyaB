package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 测试 DuplicateMessageFilter 对媒体组消息的处理
 * 验证修复：媒体组的第一条消息不应该阻止后续消息通过
 */
class DuplicateMessageFilterMediaGroupTest {
    
    @Mock
    private RawMessageRepository rawMessageRepository;
    
    private DuplicateMessageFilter filter;
    private ConcurrentSafetyProperties properties;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建配置
        properties = new ConcurrentSafetyProperties();
        properties.getCache().setTtl(10);
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(5);
        
        filter = new DuplicateMessageFilter(rawMessageRepository, properties);
        
        // 模拟数据库查询：媒体组不存在
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(anyLong(), anyLong()))
            .thenReturn(false);
    }
    
    @Test
    void testMediaGroupMessagesCanAllPassThrough() {
        // Given: 一个媒体组的3条消息
        long chatId = -1002079028242L;
        long mediaAlbumId = 14174754552106749L;
        
        TdApi.Message message1 = createMediaGroupMessage(chatId, 309329920L, mediaAlbumId);
        TdApi.Message message2 = createMediaGroupMessage(chatId, 310378496L, mediaAlbumId);
        TdApi.Message message3 = createMediaGroupMessage(chatId, 311427072L, mediaAlbumId);
        
        FilterContext context1 = new FilterContext();
        FilterContext context2 = new FilterContext();
        FilterContext context3 = new FilterContext();
        
        // When: 依次过滤这3条消息
        FilterResult result1 = filter.doFilter(message1, context1);
        FilterResult result2 = filter.doFilter(message2, context2);
        FilterResult result3 = filter.doFilter(message3, context3);
        
        // Then: 所有消息都应该通过（因为数据库中不存在）
        assertThat(result1).isEqualTo(FilterResult.ACCEPT);
        assertThat(result2).isEqualTo(FilterResult.ACCEPT);
        assertThat(result3).isEqualTo(FilterResult.ACCEPT);
    }
    
    @Test
    void testMediaGroupMessagesRejectedAfterMarkedAsProcessed() {
        // Given: 一个媒体组的消息
        long chatId = -1002079028242L;
        long mediaAlbumId = 14174754552106749L;
        
        TdApi.Message message1 = createMediaGroupMessage(chatId, 309329920L, mediaAlbumId);
        TdApi.Message message2 = createMediaGroupMessage(chatId, 310378496L, mediaAlbumId);
        
        FilterContext context1 = new FilterContext();
        FilterContext context2 = new FilterContext();
        
        // When: 第一条消息通过后，标记媒体组为已处理
        FilterResult result1 = filter.doFilter(message1, context1);
        filter.markMediaGroupProcessed(chatId, mediaAlbumId);
        
        // 第二条消息尝试通过
        FilterResult result2 = filter.doFilter(message2, context2);
        
        // Then: 第一条消息通过，第二条消息被拒绝
        assertThat(result1).isEqualTo(FilterResult.ACCEPT);
        assertThat(result2).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("媒体组正在处理中或已处理");
    }
    
    private TdApi.Message createMediaGroupMessage(long chatId, long messageId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.chatId = chatId;
        message.id = messageId;
        message.mediaAlbumId = mediaAlbumId;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.content = new TdApi.MessagePhoto();
        return message;
    }
}
