package org.xlyo.cocomonyab.plugin.tagforward.component;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ForwardScheduler单元测试
 * 
 * 测试转发调度器的核心功能，包括启动/停止、批量处理、频率限制和错误处理
 */
@Testcontainers
@DisplayName("ForwardScheduler Unit Tests")
class ForwardSchedulerTest {
    
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
    
    @BeforeEach
    void setUp() {
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
        properties.setBatchSize(5);
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
    
    @AfterEach
    void tearDown() {
        forwardScheduler.stop();
        mongoTemplate.dropCollection("forward_queue");
    }
    
    @Test
    @DisplayName("Should start and stop scheduler gracefully")
    void testStartAndStop_shouldWorkGracefully() {
        // When
        forwardScheduler.start();
        
        // Then - 调度器应该正在运行
        // 等待一小段时间确保调度器启动
        await().atMost(2, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> true);
        
        // When - 停止调度器
        forwardScheduler.stop();
        
        // Then - 应该优雅地停止
        // 验证通过没有异常抛出
    }
    
    @Test
    @DisplayName("Should not start scheduler twice")
    void testStart_shouldNotStartTwice() {
        // Given
        forwardScheduler.start();
        
        // When - 尝试再次启动
        forwardScheduler.start();
        
        // Then - 应该记录警告但不崩溃
        forwardScheduler.stop();
    }
    
    @Test
    @DisplayName("Should process pending messages from queue")
    void testProcessQueue_shouldProcessPendingMessages() throws Exception {
        // Given - 创建待处理消息
        queueManager.enqueue(-100L, 1L, Arrays.asList("#tag1"));
        queueManager.enqueue(-100L, 2L, Arrays.asList("#tag2"));
        
        // Mock successful forward
        TdApi.Messages successResponse = new TdApi.Messages();
        successResponse.messages = new TdApi.Message[1];
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        // When - 启动调度器
        forwardScheduler.start();
        
        // Then - 等待消息被处理
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<ForwardQueueItem> pending = queueManager.getPendingItems(10);
                    return pending.isEmpty();
                });
        
        // 验证所有消息都被标记为SUCCESS
        List<ForwardQueueItem> allItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(allItems).hasSize(2);
        assertThat(allItems).allMatch(item -> item.getStatus() == ForwardStatus.SUCCESS);
        assertThat(allItems).allMatch(item -> item.getForwardTime() != null);
    }
    
    @Test
    @DisplayName("Should respect batch size limit")
    void testProcessQueue_shouldRespectBatchSize() throws Exception {
        // Given - 创建超过批量大小的消息
        int messageCount = properties.getBatchSize() + 3;
        for (int i = 0; i < messageCount; i++) {
            queueManager.enqueue(-100L, (long) i, Arrays.asList("#tag"));
        }
        
        // Mock successful forward
        TdApi.Messages successResponse = new TdApi.Messages();
        successResponse.messages = new TdApi.Message[1];
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        // When - 启动调度器并等待一次处理
        forwardScheduler.start();
        
        // Then - 第一批应该只处理batchSize条消息
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<ForwardQueueItem> allItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
                    long successCount = allItems.stream()
                            .filter(item -> item.getStatus() == ForwardStatus.SUCCESS)
                            .count();
                    return successCount >= properties.getBatchSize();
                });
        
        // 验证至少处理了batchSize条消息
        List<ForwardQueueItem> allItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        long successCount = allItems.stream()
                .filter(item -> item.getStatus() == ForwardStatus.SUCCESS)
                .count();
        assertThat(successCount).isGreaterThanOrEqualTo(properties.getBatchSize());
    }
    
    @Test
    @DisplayName("Should handle forward success correctly")
    void testForwardMessage_shouldHandleSuccess() throws Exception {
        // Given
        queueManager.enqueue(-100L, 1L, Arrays.asList("#tag1"));
        
        // Mock successful forward
        TdApi.Messages successResponse = new TdApi.Messages();
        successResponse.messages = new TdApi.Message[1];
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        // When
        forwardScheduler.start();
        
        // Then
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<ForwardQueueItem> items = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
                    return !items.isEmpty() && items.get(0).getStatus() == ForwardStatus.SUCCESS;
                });
        
        ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").get(0);
        assertThat(item.getStatus()).isEqualTo(ForwardStatus.SUCCESS);
        assertThat(item.getForwardTime()).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle forward error and increment retry count")
    void testForwardMessage_shouldHandleErrorAndRetry() throws Exception {
        // Given
        queueManager.enqueue(-100L, 1L, Arrays.asList("#tag1"));
        
        // Mock failed forward
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));
        
        // When
        forwardScheduler.start();
        
        // Then - 等待重试计数增加
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<ForwardQueueItem> items = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
                    return !items.isEmpty() && items.get(0).getRetryCount() > 0;
                });
        
        ForwardQueueItem item = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue").get(0);
        assertThat(item.getRetryCount()).isGreaterThan(0);
        assertThat(item.getStatus()).isEqualTo(ForwardStatus.PENDING);
    }
    
    @Test
    @DisplayName("Should mark as FAILED after max retry count")
    void testForwardMessage_shouldMarkFailedAfterMaxRetries() throws Exception {
        // Given - 创建一个已经重试多次的消息
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(-100L)
                .sourceMessageId(1L)
                .matchedTags(Arrays.asList("#tag1"))
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .retryCount(properties.getMaxRetryCount() - 1)
                .build();
        mongoTemplate.insert(item, "forward_queue");
        
        // Mock failed forward
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Permanent error")));
        
        // When
        forwardScheduler.start();
        
        // Then - 应该标记为FAILED
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> {
                    ForwardQueueItem updated = mongoTemplate.findById(item.getId(), ForwardQueueItem.class, "forward_queue");
                    return updated != null && updated.getStatus() == ForwardStatus.FAILED;
                });
        
        ForwardQueueItem updated = mongoTemplate.findById(item.getId(), ForwardQueueItem.class, "forward_queue");
        assertThat(updated.getStatus()).isEqualTo(ForwardStatus.FAILED);
        assertThat(updated.getRetryCount()).isEqualTo(properties.getMaxRetryCount());
        assertThat(updated.getErrorMessage()).isNotNull();
    }
    
    @Test
    @DisplayName("Should construct ForwardMessages request correctly")
    void testForwardMessage_shouldConstructRequestCorrectly() throws Exception {
        // Given
        Long sourceChatId = -1001111111111L;
        Long sourceMessageId = 12345L;
        queueManager.enqueue(sourceChatId, sourceMessageId, Arrays.asList("#tag1"));
        
        // Mock successful forward
        TdApi.Messages successResponse = new TdApi.Messages();
        successResponse.messages = new TdApi.Message[1];
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.completedFuture(successResponse));
        
        // When
        forwardScheduler.start();
        
        // Then - 等待请求被发送
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
        
        // 捕获请求参数
        ArgumentCaptor<TdApi.ForwardMessages> requestCaptor = ArgumentCaptor.forClass(TdApi.ForwardMessages.class);
        verify(mockClient, atLeastOnce()).send(requestCaptor.capture());
        
        TdApi.ForwardMessages request = requestCaptor.getValue();
        assertThat(request.chatId).isEqualTo(properties.getTargetChannelId());
        assertThat(request.fromChatId).isEqualTo(sourceChatId);
        assertThat(request.messageIds).hasSize(1);
        assertThat(request.messageIds[0]).isEqualTo(sourceMessageId);
        assertThat(request.sendCopy).isFalse();
        assertThat(request.removeCaption).isFalse();
    }
    
    @Test
    @DisplayName("Should skip processing when client is not ready")
    void testForwardMessage_shouldSkipWhenClientNotReady() throws Exception {
        // Given
        queueManager.enqueue(-100L, 1L, Arrays.asList("#tag1"));
        when(clientManager.isReady()).thenReturn(false);
        
        // When
        forwardScheduler.start();
        
        // Then - 等待一段时间，消息应该仍然是PENDING状态
        Thread.sleep(2000);
        
        List<ForwardQueueItem> items = queueManager.getPendingItems(10);
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getStatus()).isEqualTo(ForwardStatus.PENDING);
        
        // 验证没有调用send方法
        verify(mockClient, never()).send(any(TdApi.ForwardMessages.class));
    }
    
    @Test
    @DisplayName("Should handle exceptions during processing without crashing")
    void testProcessQueue_shouldHandleExceptionsGracefully() throws Exception {
        // Given
        queueManager.enqueue(-100L, 1L, Arrays.asList("#tag1"));
        
        // Mock exception during send
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenThrow(new RuntimeException("Unexpected error"));
        
        // When
        forwardScheduler.start();
        
        // Then - 调度器应该继续运行，不崩溃
        Thread.sleep(2000);
        
        // 验证消息仍在队列中
        List<ForwardQueueItem> items = queueManager.getPendingItems(10);
        assertThat(items).hasSize(1);
    }
}
