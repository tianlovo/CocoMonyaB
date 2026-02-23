package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 媒体组并发处理集成测试
 * 
 * 验证需求：
 * - 需求 1.1: 媒体组的多条消息并发到达时，所有消息都被收集到同一个缓冲区中
 * - 需求 1.2: 定时任务检查超时的媒体组时，防止消息丢失或重复处理
 * - 需求 7.1: 并发消息处理测试，模拟多线程同时处理消息
 * - 需求 7.4: 验证没有消息丢失
 * - 需求 7.5: 验证没有消息重复处理
 */
@SpringBootTest
@TestPropertySource(properties = {
    "concurrent-safety.media-group.timeout=1000",
    "concurrent-safety.lock.stripes=128",
    "concurrent-safety.cache.ttl=10"
})
class MediaGroupConcurrentProcessingIntegrationTest {
    
    @Autowired
    private ChannelMonitorService channelMonitorService;
    
    @Autowired
    private RawMessageRepository rawMessageRepository;
    
    private static final long TEST_CHAT_ID = -1001234567890L;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        rawMessageRepository.deleteAll();
        
        // 启动监控
        channelMonitorService.startMonitoring(TEST_CHAT_ID);
    }
    
    @Test
    void testConcurrentMediaGroupMessageCollection() throws InterruptedException {
        // Given: 创建一个媒体组的5条消息
        long mediaAlbumId = 5629499534213120L;
        int messageCount = 5;
        List<TdApi.Message> messages = createMediaGroupMessages(
            TEST_CHAT_ID, mediaAlbumId, messageCount
        );
        
        // When: 使用多线程并发发送消息
        ExecutorService executor = Executors.newFixedThreadPool(messageCount);
        CountDownLatch latch = new CountDownLatch(messageCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (TdApi.Message message : messages) {
            executor.submit(() -> {
                try {
                    channelMonitorService.handleNewMessage(message);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有消息处理完成
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // 等待媒体组超时处理
        Thread.sleep(1500);
        channelMonitorService.processTimedOutMediaGroups();
        
        // 等待异步处理完成
        Thread.sleep(500);
        
        executor.shutdown();
        
        // Then: 验证所有消息都被成功处理
        assertThat(successCount.get()).isEqualTo(messageCount);
        
        // 验证数据库中保存了所有消息
        long savedCount = rawMessageRepository.countByChatIdAndMediaAlbumId(
            TEST_CHAT_ID, mediaAlbumId
        );
        assertThat(savedCount).isEqualTo(messageCount);
        
        // 验证没有重复消息
        List<RawMessage> savedMessages = rawMessageRepository.findAllByChatIdAndMediaAlbumId(
            TEST_CHAT_ID, mediaAlbumId
        );
        assertThat(savedMessages).hasSize(messageCount);
        
        // 验证所有消息ID都不同
        List<Long> messageIds = savedMessages.stream()
            .map(RawMessage::getMessageId)
            .toList();
        assertThat(messageIds).doesNotHaveDuplicates();
    }
    
    @Test
    void testMultipleMediaGroupsConcurrentProcessing() throws InterruptedException {
        // Given: 创建3个不同的媒体组，每个3条消息
        int groupCount = 3;
        int messagesPerGroup = 3;
        List<List<TdApi.Message>> mediaGroups = new ArrayList<>();
        
        for (int i = 0; i < groupCount; i++) {
            long mediaAlbumId = 1000L + i;
            mediaGroups.add(createMediaGroupMessages(
                TEST_CHAT_ID, mediaAlbumId, messagesPerGroup
            ));
        }
        
        // When: 并发发送所有媒体组的消息
        int totalMessages = groupCount * messagesPerGroup;
        ExecutorService executor = Executors.newFixedThreadPool(totalMessages);
        CountDownLatch latch = new CountDownLatch(totalMessages);
        
        for (List<TdApi.Message> group : mediaGroups) {
            for (TdApi.Message message : group) {
                executor.submit(() -> {
                    try {
                        channelMonitorService.handleNewMessage(message);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // 等待媒体组超时处理
        Thread.sleep(1500);
        channelMonitorService.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        executor.shutdown();
        
        // Then: 验证每个媒体组都被正确处理
        for (int i = 0; i < groupCount; i++) {
            long mediaAlbumId = 1000L + i;
            long savedCount = rawMessageRepository.countByChatIdAndMediaAlbumId(
                TEST_CHAT_ID, mediaAlbumId
            );
            assertThat(savedCount)
                .as("媒体组 %d 应该有 %d 条消息", mediaAlbumId, messagesPerGroup)
                .isEqualTo(messagesPerGroup);
        }
        
        // 验证总消息数
        long totalSaved = rawMessageRepository.countByChatId(TEST_CHAT_ID);
        assertThat(totalSaved).isEqualTo(totalMessages);
    }
    
    @Test
    void testRaceConditionBetweenNewMessageAndTimeout() throws InterruptedException {
        // Given: 创建一个媒体组的消息
        long mediaAlbumId = 2000L;
        List<TdApi.Message> messages = createMediaGroupMessages(
            TEST_CHAT_ID, mediaAlbumId, 4
        );
        
        // When: 先发送前3条消息
        for (int i = 0; i < 3; i++) {
            channelMonitorService.handleNewMessage(messages.get(i));
        }
        
        // 并发执行：定时任务处理 + 新消息添加
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger timeoutProcessed = new AtomicInteger(0);
        AtomicInteger messageAdded = new AtomicInteger(0);
        
        // 线程1：等待超时后处理
        executor.submit(() -> {
            try {
                Thread.sleep(1200); // 等待超时
                channelMonitorService.processTimedOutMediaGroups();
                timeoutProcessed.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // 线程2：在超时边界添加新消息
        executor.submit(() -> {
            try {
                Thread.sleep(1100); // 接近超时时添加
                channelMonitorService.handleNewMessage(messages.get(3));
                messageAdded.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // 等待异步处理完成
        Thread.sleep(500);
        
        executor.shutdown();
        
        // Then: 验证没有消息丢失
        long savedCount = rawMessageRepository.countByChatIdAndMediaAlbumId(
            TEST_CHAT_ID, mediaAlbumId
        );
        
        // 应该保存了3条或4条消息（取决于第4条消息是否在超时前添加）
        assertThat(savedCount)
            .as("应该保存3条或4条消息，不应该丢失")
            .isGreaterThanOrEqualTo(3)
            .isLessThanOrEqualTo(4);
        
        // 验证没有重复
        List<RawMessage> savedMessages = rawMessageRepository.findAllByChatIdAndMediaAlbumId(
            TEST_CHAT_ID, mediaAlbumId
        );
        List<Long> messageIds = savedMessages.stream()
            .map(RawMessage::getMessageId)
            .toList();
        assertThat(messageIds).doesNotHaveDuplicates();
    }
    
    @Test
    void testHighConcurrencyMediaGroupProcessing() throws InterruptedException {
        // Given: 创建10个媒体组，每个5条消息，共50条消息
        int groupCount = 10;
        int messagesPerGroup = 5;
        List<List<TdApi.Message>> mediaGroups = new ArrayList<>();
        
        for (int i = 0; i < groupCount; i++) {
            long mediaAlbumId = 3000L + i;
            mediaGroups.add(createMediaGroupMessages(
                TEST_CHAT_ID, mediaAlbumId, messagesPerGroup
            ));
        }
        
        // When: 使用高并发线程池发送所有消息
        int totalMessages = groupCount * messagesPerGroup;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalMessages);
        ConcurrentHashMap<Long, AtomicInteger> groupMessageCounts = new ConcurrentHashMap<>();
        
        for (List<TdApi.Message> group : mediaGroups) {
            for (TdApi.Message message : group) {
                executor.submit(() -> {
                    try {
                        channelMonitorService.handleNewMessage(message);
                        groupMessageCounts
                            .computeIfAbsent(message.mediaAlbumId, k -> new AtomicInteger(0))
                            .incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // 等待所有媒体组超时处理
        Thread.sleep(1500);
        channelMonitorService.processTimedOutMediaGroups();
        Thread.sleep(1000);
        
        executor.shutdown();
        
        // Then: 验证所有消息都被正确处理
        for (int i = 0; i < groupCount; i++) {
            long mediaAlbumId = 3000L + i;
            
            // 验证数据库中的消息数
            long savedCount = rawMessageRepository.countByChatIdAndMediaAlbumId(
                TEST_CHAT_ID, mediaAlbumId
            );
            assertThat(savedCount)
                .as("媒体组 %d 应该有 %d 条消息", mediaAlbumId, messagesPerGroup)
                .isEqualTo(messagesPerGroup);
            
            // 验证没有重复
            List<RawMessage> savedMessages = rawMessageRepository.findAllByChatIdAndMediaAlbumId(
                TEST_CHAT_ID, mediaAlbumId
            );
            List<Long> messageIds = savedMessages.stream()
                .map(RawMessage::getMessageId)
                .toList();
            assertThat(messageIds).doesNotHaveDuplicates();
        }
        
        // 验证总消息数
        long totalSaved = rawMessageRepository.countByChatId(TEST_CHAT_ID);
        assertThat(totalSaved).isEqualTo(totalMessages);
    }
    
    @Test
    void testNoMessageLossUnderConcurrentLoad() throws InterruptedException {
        // Given: 创建一个大媒体组（10条消息）
        long mediaAlbumId = 4000L;
        int messageCount = 10;
        List<TdApi.Message> messages = createMediaGroupMessages(
            TEST_CHAT_ID, mediaAlbumId, messageCount
        );
        
        // When: 使用随机延迟并发发送消息，模拟真实场景
        ExecutorService executor = Executors.newFixedThreadPool(messageCount);
        CountDownLatch latch = new CountDownLatch(messageCount);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (TdApi.Message message : messages) {
            executor.submit(() -> {
                try {
                    // 随机延迟0-50ms
                    Thread.sleep(random.nextInt(50));
                    channelMonitorService.handleNewMessage(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        // 等待超时处理
        Thread.sleep(1500);
        channelMonitorService.processTimedOutMediaGroups();
        Thread.sleep(500);
        
        executor.shutdown();
        
        // Then: 验证所有消息都被保存，没有丢失
        long savedCount = rawMessageRepository.countByChatIdAndMediaAlbumId(
            TEST_CHAT_ID, mediaAlbumId
        );
        assertThat(savedCount)
            .as("应该保存所有 %d 条消息，不应该有消息丢失", messageCount)
            .isEqualTo(messageCount);
        
        // 验证每条消息都被保存
        List<RawMessage> savedMessages = rawMessageRepository.findAllByChatIdAndMediaAlbumId(
            TEST_CHAT_ID, mediaAlbumId
        );
        List<Long> savedMessageIds = savedMessages.stream()
            .map(RawMessage::getMessageId)
            .sorted()
            .toList();
        
        List<Long> expectedMessageIds = messages.stream()
            .map(m -> m.id)
            .sorted()
            .toList();
        
        assertThat(savedMessageIds).containsExactlyElementsOf(expectedMessageIds);
    }
    
    // Helper methods
    
    private List<TdApi.Message> createMediaGroupMessages(
            long chatId, long mediaAlbumId, int count) {
        List<TdApi.Message> messages = new ArrayList<>();
        long baseMessageId = mediaAlbumId * 1000;
        
        for (int i = 0; i < count; i++) {
            TdApi.Message message = new TdApi.Message();
            message.id = baseMessageId + i;
            message.chatId = chatId;
            message.isChannelPost = true;
            message.date = (int) (System.currentTimeMillis() / 1000);
            message.mediaAlbumId = mediaAlbumId;
            
            TdApi.MessagePhoto content = new TdApi.MessagePhoto();
            content.photo = new TdApi.Photo();
            content.photo.sizes = new TdApi.PhotoSize[0];
            content.caption = new TdApi.FormattedText();
            content.caption.text = "Photo " + i;
            message.content = content;
            
            messages.add(message);
        }
        
        return messages;
    }
}
