package org.xlyo.cocomonyab.plugin.tagforward.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QueueManager单元测试
 * 
 * 使用Testcontainers启动MongoDB进行集成测试
 */
@SpringBootTest
@Testcontainers
@DisplayName("QueueManager Unit Tests")
class QueueManagerTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("plugin.tag-based-forwarding.target-channel-id", () -> "-1001234567890");
    }
    
    @Autowired
    private QueueManager queueManager;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @BeforeEach
    void setUp() {
        // 清空集合
        mongoTemplate.dropCollection("forward_queue");
    }
    
    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection("forward_queue");
    }
    
    @Test
    @DisplayName("Should create queue item when enqueuing message")
    void testEnqueue_shouldCreateQueueItem() {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 12345L;
        List<String> tags = Arrays.asList("#tag1", "#tag2");
        
        // When
        queueManager.enqueue(chatId, messageId, tags);
        
        // Then
        List<ForwardQueueItem> items = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(items).hasSize(1);
        
        ForwardQueueItem item = items.get(0);
        assertThat(item.getSourceChatId()).isEqualTo(chatId);
        assertThat(item.getSourceMessageId()).isEqualTo(messageId);
        assertThat(item.getMatchedTags()).containsExactlyInAnyOrder("#tag1", "#tag2");
        assertThat(item.getStatus()).isEqualTo(ForwardStatus.PENDING);
        assertThat(item.getRetryCount()).isEqualTo(0);
        assertThat(item.getCreateTime()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle duplicate messages by skipping insertion")
    void testEnqueue_shouldHandleDuplicateMessages() {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 12345L;
        List<String> tags = Arrays.asList("#tag1");
        
        // When - 尝试入队两次
        queueManager.enqueue(chatId, messageId, tags);
        queueManager.enqueue(chatId, messageId, tags);
        
        // Then - 应该只有一条记录
        List<ForwardQueueItem> items = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(items).hasSize(1);
    }
    
    @Test
    @DisplayName("Should return pending items in FIFO order (by createTime)")
    void testGetPendingItems_shouldReturnPendingItemsInOrder() {
        // Given - 创建多个队列项，时间不同
        Instant now = Instant.now();
        
        ForwardQueueItem item1 = createQueueItem(-100L, 1L, now.minusSeconds(30), ForwardStatus.PENDING);
        ForwardQueueItem item2 = createQueueItem(-100L, 2L, now.minusSeconds(20), ForwardStatus.PENDING);
        ForwardQueueItem item3 = createQueueItem(-100L, 3L, now.minusSeconds(10), ForwardStatus.PENDING);
        ForwardQueueItem item4 = createQueueItem(-100L, 4L, now, ForwardStatus.SUCCESS);
        
        mongoTemplate.insert(item1, "forward_queue");
        mongoTemplate.insert(item2, "forward_queue");
        mongoTemplate.insert(item3, "forward_queue");
        mongoTemplate.insert(item4, "forward_queue");
        
        // When
        List<ForwardQueueItem> pendingItems = queueManager.getPendingItems(10);
        
        // Then - 应该只返回PENDING状态的项，按createTime升序
        assertThat(pendingItems).hasSize(3);
        assertThat(pendingItems.get(0).getSourceMessageId()).isEqualTo(1L);
        assertThat(pendingItems.get(1).getSourceMessageId()).isEqualTo(2L);
        assertThat(pendingItems.get(2).getSourceMessageId()).isEqualTo(3L);
    }
    
    @Test
    @DisplayName("Should respect limit parameter when fetching pending items")
    void testGetPendingItems_shouldRespectLimit() {
        // Given - 创建5个待处理项
        for (int i = 0; i < 5; i++) {
            ForwardQueueItem item = createQueueItem(-100L, (long) i, Instant.now().minusSeconds(50 - i), ForwardStatus.PENDING);
            mongoTemplate.insert(item, "forward_queue");
        }
        
        // When - 限制返回2条
        List<ForwardQueueItem> pendingItems = queueManager.getPendingItems(2);
        
        // Then
        assertThat(pendingItems).hasSize(2);
    }
    
    @Test
    @DisplayName("Should update status to SUCCESS and set forwardTime")
    void testUpdateStatus_shouldUpdateToSuccess() {
        // Given
        ForwardQueueItem item = createQueueItem(-100L, 1L, Instant.now(), ForwardStatus.PENDING);
        mongoTemplate.insert(item, "forward_queue");
        
        // When
        queueManager.updateStatus(item.getId(), ForwardStatus.SUCCESS, null);
        
        // Then
        ForwardQueueItem updated = mongoTemplate.findById(item.getId(), ForwardQueueItem.class, "forward_queue");
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(ForwardStatus.SUCCESS);
        assertThat(updated.getForwardTime()).isNotNull();
        assertThat(updated.getUpdateTime()).isNotNull();
    }
    
    @Test
    @DisplayName("Should update status to FAILED and record error message")
    void testUpdateStatus_shouldUpdateToFailedWithError() {
        // Given
        ForwardQueueItem item = createQueueItem(-100L, 1L, Instant.now(), ForwardStatus.PENDING);
        mongoTemplate.insert(item, "forward_queue");
        
        // When
        String errorMessage = "CHAT_WRITE_FORBIDDEN";
        queueManager.updateStatus(item.getId(), ForwardStatus.FAILED, errorMessage);
        
        // Then
        ForwardQueueItem updated = mongoTemplate.findById(item.getId(), ForwardQueueItem.class, "forward_queue");
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(ForwardStatus.FAILED);
        assertThat(updated.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(updated.getUpdateTime()).isNotNull();
        assertThat(updated.getForwardTime()).isNull();
    }
    
    @Test
    @DisplayName("Should increment retry count by 1")
    void testIncrementRetryCount_shouldIncrementCount() {
        // Given
        ForwardQueueItem item = createQueueItem(-100L, 1L, Instant.now(), ForwardStatus.PENDING);
        item.setRetryCount(0);
        mongoTemplate.insert(item, "forward_queue");
        
        // When
        queueManager.incrementRetryCount(item.getId());
        
        // Then
        ForwardQueueItem updated = mongoTemplate.findById(item.getId(), ForwardQueueItem.class, "forward_queue");
        assertThat(updated).isNotNull();
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getUpdateTime()).isNotNull();
    }
    
    @Test
    @DisplayName("Should increment retry count multiple times correctly")
    void testIncrementRetryCount_shouldIncrementMultipleTimes() {
        // Given
        ForwardQueueItem item = createQueueItem(-100L, 1L, Instant.now(), ForwardStatus.PENDING);
        item.setRetryCount(0);
        mongoTemplate.insert(item, "forward_queue");
        
        // When - 递增3次
        queueManager.incrementRetryCount(item.getId());
        queueManager.incrementRetryCount(item.getId());
        queueManager.incrementRetryCount(item.getId());
        
        // Then
        ForwardQueueItem updated = mongoTemplate.findById(item.getId(), ForwardQueueItem.class, "forward_queue");
        assertThat(updated).isNotNull();
        assertThat(updated.getRetryCount()).isEqualTo(3);
    }
    
    // Helper method
    private ForwardQueueItem createQueueItem(Long chatId, Long messageId, Instant createTime, ForwardStatus status) {
        return ForwardQueueItem.builder()
                .sourceChatId(chatId)
                .sourceMessageId(messageId)
                .matchedTags(Arrays.asList("#test"))
                .status(status)
                .createTime(createTime)
                .retryCount(0)
                .build();
    }
}
