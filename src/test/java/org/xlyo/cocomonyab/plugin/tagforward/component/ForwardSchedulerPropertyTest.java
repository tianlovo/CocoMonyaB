package org.xlyo.cocomonyab.plugin.tagforward.component;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for ForwardScheduler
 * 
 * **Validates: Requirements 6.11, 6.12, 6.13, 6.14, 6.15, 6.16, 7.4, 7.6, 7.7, 12.2, 12.3, 12.4, 12.5, 12.6, 12.10, 12.11, 12.12**
 */
@Testcontainers
class ForwardSchedulerPropertyTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    private ForwardScheduler forwardScheduler;
    private QueueManager queueManager;
    private MongoTemplate mongoTemplate;
    private TagBasedForwardingProperties properties;
    private TelegramClientManager clientManager;
    private SimpleTelegramClient mockClient;
    
    private void setUp() {
        // 创建MongoDB模板
        mongoTemplate = new MongoTemplate(
                com.mongodb.client.MongoClients.create(mongoDBContainer.getReplicaSetUrl()),
                "test"
        );
        
        // 清空集合
        mongoTemplate.dropCollection("forward_queue");
        
        // 创建配置
        properties = new TagBasedForwardingProperties();
        properties.setTargetChannelId(-1001234567890L);
        properties.setScheduleIntervalSeconds(1);
        properties.setBatchSize(10);
        properties.setRateLimitPerMinute(60);
        properties.setMaxRetryCount(3);
        
        // 创建QueueManager
        queueManager = new QueueManager(mongoTemplate);
        
        // 创建mock client
        mockClient = mock(SimpleTelegramClient.class);
        clientManager = mock(TelegramClientManager.class);
        when(clientManager.isReady()).thenReturn(true);
        when(clientManager.getClient()).thenReturn(mockClient);
        
        // 创建ForwardScheduler
        forwardScheduler = new ForwardScheduler(queueManager, clientManager, properties);
        forwardScheduler.initialize();
    }
    
    private void tearDown() {
        if (forwardScheduler != null) {
            forwardScheduler.stop();
        }
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection("forward_queue");
        }
    }
    
    @Property(tries = 50)
    @Label("Feature: tag-based-message-forwarding, Property 9: Forward success status update")
    void forwardSuccessStatusUpdate(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId) {
        
        try {
            setUp();
            
            // Given - 创建待转发消息
            queueManager.enqueue(sourceChatId, sourceMessageId, Arrays.asList("#test"));
            
            // Mock successful forward
            TdApi.Messages successResponse = new TdApi.Messages();
            successResponse.messages = new TdApi.Message[1];
            when(mockClient.send(any(TdApi.ForwardMessages.class)))
                    .thenReturn(CompletableFuture.completedFuture(successResponse));
            
            // When - 启动调度器
            forwardScheduler.start();
            
            // Then - 等待消息被处理并验证状态更新
            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                                .filter(i -> i.getSourceChatId().equals(sourceChatId) && 
                                           i.getSourceMessageId().equals(sourceMessageId))
                                .findFirst()
                                .orElse(null);
                        return item != null && item.getStatus() == ForwardStatus.SUCCESS;
                    });
            
            ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                    .filter(i -> i.getSourceChatId().equals(sourceChatId) && 
                               i.getSourceMessageId().equals(sourceMessageId))
                    .findFirst()
                    .orElse(null);
            
            assertThat(item).isNotNull();
            assertThat(item.getStatus())
                    .as("Status should be SUCCESS after successful forward")
                    .isEqualTo(ForwardStatus.SUCCESS);
            assertThat(item.getForwardTime())
                    .as("ForwardTime should be set after successful forward")
                    .isNotNull();
            
        } finally {
            tearDown();
        }
    }
    
    @Property(tries = 50)
    @Label("Feature: tag-based-message-forwarding, Property 10: Retry logic")
    void retryLogic(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId,
            @ForAll @IntRange(min = 1, max = 5) int failureCount) {
        
        try {
            setUp();
            
            // Given - 创建待转发消息
            queueManager.enqueue(sourceChatId, sourceMessageId, Arrays.asList("#test"));
            
            // Mock failed forward
            when(mockClient.send(any(TdApi.ForwardMessages.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));
            
            // When - 启动调度器并等待多次重试
            forwardScheduler.start();
            
            // Then - 等待重试计数增加
            int expectedRetryCount = Math.min(failureCount, properties.getMaxRetryCount());
            await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                                .filter(i -> i.getSourceChatId().equals(sourceChatId) && 
                                           i.getSourceMessageId().equals(sourceMessageId))
                                .findFirst()
                                .orElse(null);
                        return item != null && item.getRetryCount() >= expectedRetryCount;
                    });
            
            ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                    .filter(i -> i.getSourceChatId().equals(sourceChatId) && 
                               i.getSourceMessageId().equals(sourceMessageId))
                    .findFirst()
                    .orElse(null);
            
            assertThat(item).isNotNull();
            
            // 如果失败次数小于maxRetryCount，状态应该保持PENDING
            if (failureCount < properties.getMaxRetryCount()) {
                assertThat(item.getStatus())
                        .as("Status should remain PENDING when retryCount < maxRetryCount")
                        .isEqualTo(ForwardStatus.PENDING);
                assertThat(item.getRetryCount())
                        .as("RetryCount should be incremented")
                        .isGreaterThan(0);
            } else {
                // 如果达到maxRetryCount，状态应该变为FAILED
                assertThat(item.getStatus())
                        .as("Status should be FAILED when retryCount >= maxRetryCount")
                        .isEqualTo(ForwardStatus.FAILED);
                assertThat(item.getRetryCount())
                        .as("RetryCount should equal maxRetryCount")
                        .isEqualTo(properties.getMaxRetryCount());
                assertThat(item.getErrorMessage())
                        .as("Error message should be recorded")
                        .isNotNull();
            }
            
        } finally {
            tearDown();
        }
    }
    
    @Property(tries = 30)
    @Label("Feature: tag-based-message-forwarding, Property 11: Rate limit enforcement")
    void rateLimitEnforcement(
            @ForAll @IntRange(min = 10, max = 30) int messageCount,
            @ForAll @IntRange(min = 5, max = 15) int rateLimitPerMinute) {
        
        try {
            setUp();
            
            // 设置频率限制
            properties.setRateLimitPerMinute(rateLimitPerMinute);
            forwardScheduler = new ForwardScheduler(queueManager, clientManager, properties);
            forwardScheduler.initialize();
            
            // Given - 创建多条待转发消息
            for (int i = 0; i < messageCount; i++) {
                queueManager.enqueue(-100L, (long) i, Arrays.asList("#test"));
            }
            
            // Mock successful forward
            TdApi.Messages successResponse = new TdApi.Messages();
            successResponse.messages = new TdApi.Message[1];
            when(mockClient.send(any(TdApi.ForwardMessages.class)))
                    .thenReturn(CompletableFuture.completedFuture(successResponse));
            
            // When - 启动调度器
            long startTime = System.currentTimeMillis();
            forwardScheduler.start();
            
            // Then - 等待一段时间后检查处理的消息数量
            Thread.sleep(3000); // 等待3秒
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            
            // 计算在这段时间内应该处理的最大消息数
            double expectedMaxMessages = (rateLimitPerMinute / 60.0) * elapsedSeconds * 1.5; // 1.5倍容差
            
            long processedCount = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                    .filter(item -> item.getStatus() == ForwardStatus.SUCCESS)
                    .count();
            
            assertThat(processedCount)
                    .as("Processed messages should respect rate limit")
                    .isLessThanOrEqualTo((long) expectedMaxMessages);
            
            // 被跳过的消息应该保持PENDING状态
            long pendingCount = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                    .filter(item -> item.getStatus() == ForwardStatus.PENDING)
                    .count();
            
            assertThat(pendingCount + processedCount)
                    .as("Total messages should equal original count")
                    .isEqualTo(messageCount);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            tearDown();
        }
    }
    
    @Property(tries = 50)
    @Label("Feature: tag-based-message-forwarding, Property 13: Forward request construction correctness")
    void forwardRequestConstructionCorrectness(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId,
            @ForAll Long targetChannelId) {
        
        try {
            setUp();
            
            // 设置目标频道ID（必须为负数）
            Long validTargetChannelId = targetChannelId >= 0 ? -targetChannelId : targetChannelId;
            properties.setTargetChannelId(validTargetChannelId);
            forwardScheduler = new ForwardScheduler(queueManager, clientManager, properties);
            forwardScheduler.initialize();
            
            // Given - 创建待转发消息
            queueManager.enqueue(sourceChatId, sourceMessageId, Arrays.asList("#test"));
            
            // Mock successful forward and capture request
            TdApi.Messages successResponse = new TdApi.Messages();
            successResponse.messages = new TdApi.Message[1];
            when(mockClient.send(any(TdApi.ForwardMessages.class)))
                    .thenReturn(CompletableFuture.completedFuture(successResponse));
            
            // When - 启动调度器
            forwardScheduler.start();
            
            // Then - 等待请求被发送并验证请求构造
            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        try {
                            verify(mockClient, atLeastOnce()).send(any(TdApi.ForwardMessages.class));
                            return true;
                        } catch (AssertionError e) {
                            return false;
                        }
                    });
            
            // 捕获并验证请求参数
            org.mockito.ArgumentCaptor<TdApi.ForwardMessages> requestCaptor = 
                    org.mockito.ArgumentCaptor.forClass(TdApi.ForwardMessages.class);
            verify(mockClient, atLeastOnce()).send(requestCaptor.capture());
            
            TdApi.ForwardMessages request = requestCaptor.getValue();
            
            assertThat(request.chatId)
                    .as("Request chatId should equal target channel ID")
                    .isEqualTo(validTargetChannelId);
            assertThat(request.fromChatId)
                    .as("Request fromChatId should equal source chat ID")
                    .isEqualTo(sourceChatId);
            assertThat(request.messageIds)
                    .as("Request messageIds should contain source message ID")
                    .hasSize(1)
                    .contains(sourceMessageId);
            assertThat(request.sendCopy)
                    .as("Request sendCopy should be false to preserve forward info")
                    .isFalse();
            assertThat(request.removeCaption)
                    .as("Request removeCaption should be false to preserve caption")
                    .isFalse();
            
        } finally {
            tearDown();
        }
    }
    
    @Property(tries = 30)
    @Label("Feature: tag-based-message-forwarding, Property 14: Telegram error handling")
    void telegramErrorHandling(
            @ForAll Long sourceChatId,
            @ForAll Long sourceMessageId,
            @ForAll("telegramErrors") String errorType) {
        
        try {
            setUp();
            
            // Given - 创建待转发消息
            queueManager.enqueue(sourceChatId, sourceMessageId, Arrays.asList("#test"));
            
            // Mock different types of Telegram errors
            RuntimeException error;
            switch (errorType) {
                case "FLOOD_WAIT":
                    error = new RuntimeException("FLOOD_WAIT_30");
                    break;
                case "MESSAGE_ID_INVALID":
                    error = new RuntimeException("MESSAGE_ID_INVALID");
                    break;
                case "CHAT_WRITE_FORBIDDEN":
                    error = new RuntimeException("CHAT_WRITE_FORBIDDEN");
                    break;
                default:
                    error = new RuntimeException("Unknown error");
            }
            
            when(mockClient.send(any(TdApi.ForwardMessages.class)))
                    .thenReturn(CompletableFuture.failedFuture(error));
            
            // When - 启动调度器
            forwardScheduler.start();
            
            // Then - 等待错误处理
            await().atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                                .filter(i -> i.getSourceChatId().equals(sourceChatId) && 
                                           i.getSourceMessageId().equals(sourceMessageId))
                                .findFirst()
                                .orElse(null);
                        return item != null && item.getRetryCount() > 0;
                    });
            
            ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").stream()
                    .filter(i -> i.getSourceChatId().equals(sourceChatId) && 
                               i.getSourceMessageId().equals(sourceMessageId))
                    .findFirst()
                    .orElse(null);
            
            assertThat(item).isNotNull();
            
            // 所有错误类型都应该触发重试机制
            assertThat(item.getRetryCount())
                    .as("Error should trigger retry mechanism")
                    .isGreaterThan(0);
            
            // 根据错误类型验证处理逻辑
            switch (errorType) {
                case "FLOOD_WAIT":
                case "MESSAGE_ID_INVALID":
                case "CHAT_WRITE_FORBIDDEN":
                    // 这些错误应该被记录并触发重试或标记为失败
                    if (item.getRetryCount() >= properties.getMaxRetryCount()) {
                        assertThat(item.getStatus())
                                .as("Should be marked as FAILED after max retries")
                                .isEqualTo(ForwardStatus.FAILED);
                    } else {
                        assertThat(item.getStatus())
                                .as("Should remain PENDING for retry")
                                .isEqualTo(ForwardStatus.PENDING);
                    }
                    break;
            }
            
        } finally {
            tearDown();
        }
    }
    
    // Arbitraries for generating test data
    
    @Provide
    Arbitrary<String> telegramErrors() {
        return Arbitraries.of("FLOOD_WAIT", "MESSAGE_ID_INVALID", "CHAT_WRITE_FORBIDDEN");
    }
}
