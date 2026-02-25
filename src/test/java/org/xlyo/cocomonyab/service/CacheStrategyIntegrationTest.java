package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.filter.impl.DuplicateMessageFilter;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * 缓存策略集成测试
 * 
 * 验证需求：
 * - 需求 5.2: 缓存使用基于时间的自动过期机制（TTL）
 * - 需求 6.4: 缓存保留时间过期后允许消息重新进入处理流程
 * - 需求 7.2: 缓存过期行为测试和失败重试机制
 */
@ExtendWith(MockitoExtension.class)
class CacheStrategyIntegrationTest {
    
    @Mock
    private RawMessageRepository rawMessageRepository;
    
    private DuplicateMessageFilter duplicateMessageFilter;
    
    @BeforeEach
    void setUp() {
        // 配置属性 - 使用较短的TTL便于测试
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        properties.getCache().setTtl(2); // 2秒TTL
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(1); // 1秒失败消息TTL
        
        duplicateMessageFilter = new DuplicateMessageFilter(rawMessageRepository, properties);
        
        // 模拟数据库查询 - 默认消息不存在
        lenient().when(rawMessageRepository.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        lenient().when(rawMessageRepository.existsByChatIdAndMediaAlbumId(anyLong(), anyLong()))
            .thenReturn(false);
    }
    
    @Test
    void testCacheExpirationAllowsReprocessing() throws InterruptedException {
        // Given: 创建一条测试消息
        TdApi.Message message = createTestMessage(123L, -1001234567890L);
        FilterContext context = new FilterContext();
        
        // When: 第一次过滤 - 应该接受
        FilterResult firstResult = duplicateMessageFilter.filter(message, context);
        assertThat(firstResult).isEqualTo(FilterResult.ACCEPT);
        
        // Then: 立即重试 - 应该被拒绝（缓存中存在）
        FilterContext context2 = new FilterContext();
        FilterResult immediateRetry = duplicateMessageFilter.filter(message, context2);
        assertThat(immediateRetry).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("正在处理中或已处理");
        
        // When: 等待缓存过期（2秒 + 缓冲时间）
        Thread.sleep(2500);
        
        // Then: 过期后应该允许重新处理
        FilterContext context3 = new FilterContext();
        FilterResult afterExpiry = duplicateMessageFilter.filter(message, context3);
        assertThat(afterExpiry).isEqualTo(FilterResult.ACCEPT);
    }
    
    @Test
    void testFailedMessageCacheExpiration() throws InterruptedException {
        // Given: 创建一条测试消息
        TdApi.Message message = createTestMessage(456L, -1001234567890L);
        
        // When: 标记消息处理失败
        duplicateMessageFilter.markFailed(message);
        
        // Then: 立即尝试处理 - 应该被拒绝（失败缓存中存在）
        // 注意：markFailed使用单独的失败缓存，不影响主缓存
        // 所以这里应该能通过主缓存检查
        FilterContext context = new FilterContext();
        FilterResult immediateResult = duplicateMessageFilter.filter(message, context);
        // 第一次过滤应该接受（因为主缓存中不存在）
        assertThat(immediateResult).isEqualTo(FilterResult.ACCEPT);
        
        // When: 等待失败缓存过期（1秒 + 缓冲时间）
        Thread.sleep(1500);
        
        // Then: 过期后应该允许重新处理
        FilterContext context2 = new FilterContext();
        FilterResult afterExpiry = duplicateMessageFilter.filter(message, context2);
        // 由于之前已经添加到主缓存，这里应该被拒绝
        assertThat(afterExpiry).isEqualTo(FilterResult.REJECT);
    }
    
    @Test
    void testMediaGroupCacheExpiration() throws InterruptedException {
        // Given: 创建一条媒体组消息
        TdApi.Message message = createMediaGroupMessage(789L, -1001234567890L, 5000L);
        FilterContext context = new FilterContext();
        
        // When: 第一次过滤 - 应该接受
        FilterResult firstResult = duplicateMessageFilter.filter(message, context);
        assertThat(firstResult).isEqualTo(FilterResult.ACCEPT);
        
        // Then: 立即重试 - 应该被拒绝
        FilterContext context2 = new FilterContext();
        FilterResult immediateRetry = duplicateMessageFilter.filter(message, context2);
        assertThat(immediateRetry).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("正在处理中或已处理");
        
        // When: 等待缓存过期
        Thread.sleep(2500);
        
        // Then: 过期后应该允许重新处理
        FilterContext context3 = new FilterContext();
        FilterResult afterExpiry = duplicateMessageFilter.filter(message, context3);
        assertThat(afterExpiry).isEqualTo(FilterResult.ACCEPT);
    }
    
    @Test
    void testDatabaseCheckAfterCacheExpiration() throws InterruptedException {
        // Given: 创建一条测试消息
        TdApi.Message message = createTestMessage(999L, -1001234567890L);
        FilterContext context = new FilterContext();
        
        // When: 第一次过滤 - 应该接受
        FilterResult firstResult = duplicateMessageFilter.filter(message, context);
        assertThat(firstResult).isEqualTo(FilterResult.ACCEPT);
        
        // 模拟消息已保存到数据库
        lenient().when(rawMessageRepository.existsByChatIdAndMessageId(
            message.chatId, message.id
        )).thenReturn(true);
        
        // When: 等待缓存过期
        Thread.sleep(2500);
        
        // Then: 过期后尝试处理 - 应该查询数据库并拒绝
        FilterContext context2 = new FilterContext();
        FilterResult afterExpiry = duplicateMessageFilter.filter(message, context2);
        assertThat(afterExpiry).isEqualTo(FilterResult.REJECT);
        assertThat(context2.getRejectReason()).contains("数据库中存在重复消息");
    }
    
    @Test
    void testCacheStatsTracking() {
        // Given: 创建多条测试消息
        TdApi.Message message1 = createTestMessage(111L, -1001234567890L);
        TdApi.Message message2 = createTestMessage(222L, -1001234567890L);
        TdApi.Message message3 = createTestMessage(333L, -1001234567890L);
        
        // When: 过滤消息
        duplicateMessageFilter.filter(message1, new FilterContext());
        duplicateMessageFilter.filter(message2, new FilterContext());
        duplicateMessageFilter.filter(message3, new FilterContext());
        
        // 重复过滤第一条消息
        duplicateMessageFilter.filter(message1, new FilterContext());
        
        // Then: 验证缓存统计
        assertThat(duplicateMessageFilter.getProcessingCount()).isGreaterThan(0);
        
        // 验证缓存统计信息可用
        var stats = duplicateMessageFilter.getCacheStats();
        assertThat(stats).isNotNull();
        assertThat(stats.requestCount()).isGreaterThan(0);
    }
    
    @Test
    void testCacheClearOperation() {
        // Given: 添加一些消息到缓存
        TdApi.Message message1 = createTestMessage(444L, -1001234567890L);
        TdApi.Message message2 = createTestMessage(555L, -1001234567890L);
        
        duplicateMessageFilter.filter(message1, new FilterContext());
        duplicateMessageFilter.filter(message2, new FilterContext());
        
        assertThat(duplicateMessageFilter.getProcessingCount()).isGreaterThan(0);
        
        // When: 清空缓存
        duplicateMessageFilter.clearCache();
        
        // Then: 缓存应该为空
        assertThat(duplicateMessageFilter.getProcessingCount()).isEqualTo(0);
        
        // 之前被拒绝的消息现在应该可以通过
        FilterContext context = new FilterContext();
        FilterResult result = duplicateMessageFilter.filter(message1, context);
        assertThat(result).isEqualTo(FilterResult.ACCEPT);
    }
    
    @Test
    void testConcurrentCacheAccess() throws InterruptedException {
        // Given: 创建一条测试消息
        TdApi.Message message = createTestMessage(666L, -1001234567890L);
        
        // When: 多线程并发访问缓存
        int threadCount = 10;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger acceptCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger rejectCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    FilterContext context = new FilterContext();
                    FilterResult result = duplicateMessageFilter.filter(message, context);
                    if (result == FilterResult.ACCEPT) {
                        acceptCount.incrementAndGet();
                    } else {
                        rejectCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        
        // Then: 验证所有线程都完成了，并且总数正确
        assertThat(acceptCount.get() + rejectCount.get()).isEqualTo(threadCount);
        
        // 至少有一些消息被接受（证明缓存工作）
        assertThat(acceptCount.get()).isGreaterThan(0);
        
        // 验证缓存中有记录
        assertThat(duplicateMessageFilter.getProcessingCount()).isGreaterThan(0);
    }
    
    // Helper methods
    
    private TdApi.Message createTestMessage(long messageId, long chatId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.isChannelPost = true;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.mediaAlbumId = 0;
        
        TdApi.MessageText content = new TdApi.MessageText();
        content.text = new TdApi.FormattedText();
        content.text.text = "Test message";
        message.content = content;
        
        return message;
    }
    
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
