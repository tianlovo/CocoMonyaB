package org.xlyo.cocomonyab.plugin.tagforward.component;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for QueueManager
 * 
 * Verifies correctness properties across all possible inputs
 */
@SpringBootTest
@Testcontainers
class QueueManagerPropertyTest {
    
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
    
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property 5: Message deduplication")
    void messageDeduplication(
            @ForAll Long chatId,
            @ForAll Long messageId,
            @ForAll @Size(min = 1, max = 5) List<String> tags) {
        
        // Given - 清空集合
        mongoTemplate.dropCollection("forward_queue");
        
        // When - 尝试将相同消息入队两次
        queueManager.enqueue(chatId, messageId, tags);
        queueManager.enqueue(chatId, messageId, tags);
        
        // Then - 应该只有一条记录（消息去重）
        List<ForwardQueueItem> items = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(items).hasSize(1);
        
        ForwardQueueItem item = items.get(0);
        assertThat(item.getSourceChatId()).isEqualTo(chatId);
        assertThat(item.getSourceMessageId()).isEqualTo(messageId);
        assertThat(item.getStatus()).isEqualTo(ForwardStatus.PENDING);
    }
    
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property 7: FIFO processing order")
    void fifoProcessingOrder(
            @ForAll Long chatId,
            @ForAll @IntRange(min = 5, max = 20) int messageCount) {
        
        // Given - 清空集合并创建多个消息，时间递增
        mongoTemplate.dropCollection("forward_queue");
        
        Instant baseTime = Instant.now().minusSeconds(1000);
        List<Long> expectedOrder = new ArrayList<>();
        
        for (int i = 0; i < messageCount; i++) {
            Long messageId = (long) i;
            expectedOrder.add(messageId);
            
            ForwardQueueItem item = ForwardQueueItem.builder()
                    .sourceChatId(chatId)
                    .sourceMessageId(messageId)
                    .matchedTags(Arrays.asList("#test"))
                    .status(ForwardStatus.PENDING)
                    .createTime(baseTime.plusSeconds(i))
                    .retryCount(0)
                    .build();
            
            mongoTemplate.insert(item, "forward_queue");
        }
        
        // When - 获取待处理消息
        List<ForwardQueueItem> pendingItems = queueManager.getPendingItems(messageCount);
        
        // Then - 应该按createTime升序排序（FIFO）
        assertThat(pendingItems).hasSize(messageCount);
        
        for (int i = 0; i < messageCount; i++) {
            assertThat(pendingItems.get(i).getSourceMessageId())
                    .as("Message at position %d should be in FIFO order", i)
                    .isEqualTo(expectedOrder.get(i));
        }
        
        // 验证时间顺序
        for (int i = 1; i < pendingItems.size(); i++) {
            Instant prevTime = pendingItems.get(i - 1).getCreateTime();
            Instant currTime = pendingItems.get(i).getCreateTime();
            assertThat(currTime)
                    .as("Messages should be ordered by createTime")
                    .isAfterOrEqualTo(prevTime);
        }
    }
    
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property 8: Batch size limit")
    void batchSizeLimit(
            @ForAll Long chatId,
            @ForAll @IntRange(min = 10, max = 50) int queueSize,
            @ForAll @IntRange(min = 1, max = 20) int batchSize) {
        
        // Given - 清空集合并创建queueSize个待处理消息
        mongoTemplate.dropCollection("forward_queue");
        
        Instant baseTime = Instant.now().minusSeconds(1000);
        
        for (int i = 0; i < queueSize; i++) {
            ForwardQueueItem item = ForwardQueueItem.builder()
                    .sourceChatId(chatId)
                    .sourceMessageId((long) i)
                    .matchedTags(Arrays.asList("#test"))
                    .status(ForwardStatus.PENDING)
                    .createTime(baseTime.plusSeconds(i))
                    .retryCount(0)
                    .build();
            
            mongoTemplate.insert(item, "forward_queue");
        }
        
        // When - 使用batchSize限制获取消息
        List<ForwardQueueItem> pendingItems = queueManager.getPendingItems(batchSize);
        
        // Then - 返回的消息数量不应超过batchSize
        assertThat(pendingItems.size())
                .as("Returned items should not exceed batch size")
                .isLessThanOrEqualTo(batchSize);
        
        // 如果队列中有足够的消息，应该返回完整的批次
        if (queueSize >= batchSize) {
            assertThat(pendingItems.size())
                    .as("Should return full batch when queue has enough items")
                    .isEqualTo(batchSize);
        } else {
            assertThat(pendingItems.size())
                    .as("Should return all available items when queue is smaller than batch size")
                    .isEqualTo(queueSize);
        }
    }
    
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property: Status update correctness")
    void statusUpdateCorrectness(
            @ForAll Long chatId,
            @ForAll Long messageId,
            @ForAll ForwardStatus newStatus) {
        
        // Given - 清空集合并创建一个PENDING消息
        mongoTemplate.dropCollection("forward_queue");
        
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(chatId)
                .sourceMessageId(messageId)
                .matchedTags(Arrays.asList("#test"))
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .retryCount(0)
                .build();
        
        mongoTemplate.insert(item, "forward_queue");
        String itemId = item.getId();
        
        // When - 更新状态
        String errorMessage = (newStatus == ForwardStatus.FAILED) ? "Test error" : null;
        queueManager.updateStatus(itemId, newStatus, errorMessage);
        
        // Then - 验证状态更新
        ForwardQueueItem updated = mongoTemplate.findById(itemId, ForwardQueueItem.class, "forward_queue");
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(newStatus);
        assertThat(updated.getUpdateTime()).isNotNull();
        
        // 如果是SUCCESS状态，应该设置forwardTime
        if (newStatus == ForwardStatus.SUCCESS) {
            assertThat(updated.getForwardTime())
                    .as("SUCCESS status should set forwardTime")
                    .isNotNull();
        }
        
        // 如果有错误消息，应该被记录
        if (errorMessage != null) {
            assertThat(updated.getErrorMessage())
                    .as("Error message should be recorded")
                    .isEqualTo(errorMessage);
        }
    }
    
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property: Retry count increment")
    void retryCountIncrement(
            @ForAll Long chatId,
            @ForAll Long messageId,
            @ForAll @IntRange(min = 1, max = 10) int incrementTimes) {
        
        // Given - 清空集合并创建一个消息
        mongoTemplate.dropCollection("forward_queue");
        
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(chatId)
                .sourceMessageId(messageId)
                .matchedTags(Arrays.asList("#test"))
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .retryCount(0)
                .build();
        
        mongoTemplate.insert(item, "forward_queue");
        String itemId = item.getId();
        
        // When - 递增retryCount多次
        for (int i = 0; i < incrementTimes; i++) {
            queueManager.incrementRetryCount(itemId);
        }
        
        // Then - retryCount应该等于递增次数
        ForwardQueueItem updated = mongoTemplate.findById(itemId, ForwardQueueItem.class, "forward_queue");
        assertThat(updated).isNotNull();
        assertThat(updated.getRetryCount())
                .as("Retry count should equal number of increments")
                .isEqualTo(incrementTimes);
        assertThat(updated.getUpdateTime()).isNotNull();
    }
    
    // Arbitraries for generating test data
    
    @Provide
    Arbitrary<ForwardStatus> forwardStatus() {
        return Arbitraries.of(ForwardStatus.values());
    }
}
