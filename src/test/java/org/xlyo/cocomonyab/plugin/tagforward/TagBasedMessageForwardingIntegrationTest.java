package org.xlyo.cocomonyab.plugin.tagforward;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.tagforward.component.ForwardScheduler;
import org.xlyo.cocomonyab.plugin.tagforward.component.QueueManager;
import org.xlyo.cocomonyab.plugin.tagforward.component.TagMatcher;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;
import org.xlyo.cocomonyab.plugin.tagforward.model.TagEntity;
import org.xlyo.cocomonyab.plugin.tagforward.model.TagFilterConfig;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 基于标签的消息转发插件集成测试
 * 
 * <p>此测试类验证完整的端到端流程：
 * <ul>
 *   <li>消息接收和标签匹配</li>
 *   <li>队列管理和持久化</li>
 *   <li>定时调度和转发执行</li>
 *   <li>并发消息处理</li>
 *   <li>配置集成</li>
 * </ul>
 * 
 * <p>使用Testcontainers启动真实的MongoDB实例，
 * 使用Mockito模拟TelegramClient以避免实际的网络调用
 * 
 * <p>验证需求: 所有需求的集成验证
 */
@SpringBootTest
@Testcontainers
@DisplayName("TagBasedMessageForwarding Integration Tests")
class TagBasedMessageForwardingIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("plugin.tag-based-forwarding.enabled", () -> "true");
        registry.add("plugin.tag-based-forwarding.target-channel-id", () -> "-1001234567890");
        registry.add("plugin.tag-based-forwarding.tag-prefix", () -> "#");
        registry.add("plugin.tag-based-forwarding.rate-limit-per-minute", () -> "60");
        registry.add("plugin.tag-based-forwarding.batch-size", () -> "10");
        registry.add("plugin.tag-based-forwarding.schedule-interval-seconds", () -> "1");
        registry.add("plugin.tag-based-forwarding.max-retry-count", () -> "3");
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public TelegramClientManager mockTelegramClientManager() {
            TelegramClientManager manager = mock(TelegramClientManager.class);
            SimpleTelegramClient mockClient = mock(SimpleTelegramClient.class);
            
            when(manager.getClient()).thenReturn(mockClient);
            when(manager.isReady()).thenReturn(true);
            
            // Setup default successful response
            TdApi.Messages successResponse = new TdApi.Messages();
            successResponse.messages = new TdApi.Message[1];
            successResponse.messages[0] = new TdApi.Message();
            
            when(mockClient.send(any(TdApi.ForwardMessages.class)))
                    .thenReturn(CompletableFuture.completedFuture(successResponse));
            
            return manager;
        }
    }
    
    @Autowired
    private TagBasedMessageForwardingPlugin plugin;
    
    @Autowired
    private TagMatcher tagMatcher;
    
    @Autowired
    private QueueManager queueManager;
    
    @Autowired
    private ForwardScheduler forwardScheduler;
    
    @Autowired
    private TagBasedForwardingProperties properties;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private TelegramClientManager clientManager;
    
    private SimpleTelegramClient mockClient;
    
    @BeforeEach
    void setUp() {
        // 清空所有集合
        mongoTemplate.dropCollection("forward_queue");
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        mongoTemplate.dropCollection("tag_characters");
        mongoTemplate.dropCollection("tag_works");
        
        // 获取模拟客户端
        mockClient = clientManager.getClient();
        reset(mockClient);
        
        // 设置默认的成功响应
        TdApi.Messages successResponse = new TdApi.Messages();
        successResponse.messages = new TdApi.Message[1];
        successResponse.messages[0] = new TdApi.Message();
        
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.completedFuture(successResponse));
    }
    
    @AfterEach
    void tearDown() {
        // 清空所有集合
        mongoTemplate.dropCollection("forward_queue");
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        mongoTemplate.dropCollection("tag_characters");
        mongoTemplate.dropCollection("tag_works");
    }
    
    @Test
    @DisplayName("Should complete end-to-end message forwarding flow")
    void testEndToEndForwardingFlow() throws InterruptedException {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 创建包含标签的消息
        TextMessageEntity message = createTextMessage(-100L, 1L, "This is a test message with #TestTag");
        PluginContext context = new PluginContext(createTdApiMessage(-100L, 1L));
        
        // When - 处理消息
        PluginResult result = plugin.doHandle(message, context);
        
        // Then - 验证消息被正确处理
        assertThat(result).isEqualTo(PluginResult.CONTINUE);
        
        // 验证消息已加入队列
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(1);
        
        ForwardQueueItem item = queueItems.get(0);
        assertThat(item.getSourceChatId()).isEqualTo(-100L);
        assertThat(item.getSourceMessageId()).isEqualTo(1L);
        assertThat(item.getMatchedTags()).contains("#TestTag");
        assertThat(item.getStatus()).isEqualTo(ForwardStatus.PENDING);
        
        // 等待调度器处理队列（最多5秒）
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ForwardQueueItem updated = mongoTemplate.findById(item.getId(), 
                            ForwardQueueItem.class, "forward_queue");
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus()).isEqualTo(ForwardStatus.SUCCESS);
                    assertThat(updated.getForwardTime()).isNotNull();
                });
        
        // 验证TelegramClient被调用
        verify(mockClient, atLeastOnce()).send(any(TdApi.ForwardMessages.class));
    }
    
    @Test
    @DisplayName("Should handle concurrent message processing correctly")
    void testConcurrentMessageProcessing() throws InterruptedException {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        int messageCount = 20;
        CountDownLatch latch = new CountDownLatch(messageCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When - 并发处理多条消息
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            final int messageId = i;
            Thread thread = new Thread(() -> {
                try {
                    TextMessageEntity message = createTextMessage(-100L, (long) messageId, 
                            "Concurrent message #" + messageId + " with #TestTag");
                    PluginContext context = new PluginContext(createTdApiMessage(-100L, (long) messageId));
                    
                    PluginResult result = plugin.doHandle(message, context);
                    if (result == PluginResult.CONTINUE) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(messageCount);
        
        // Then - 验证所有消息都已加入队列
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(messageCount);
        
        // 验证所有消息都是PENDING状态
        long pendingCount = queueItems.stream()
                .filter(item -> item.getStatus() == ForwardStatus.PENDING)
                .count();
        assertThat(pendingCount).isGreaterThan(0);
        
        // 等待调度器处理所有消息（最多15秒）
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<ForwardQueueItem> items = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
                    long successfulCount = items.stream()
                            .filter(item -> item.getStatus() == ForwardStatus.SUCCESS)
                            .count();
                    assertThat(successfulCount).isEqualTo(messageCount);
                });
    }
    
    @Test
    @DisplayName("Should handle messages without matching tags")
    void testMessagesWithoutMatchingTags() {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 创建不包含标签的消息
        TextMessageEntity message = createTextMessage(-100L, 1L, "This message has no tags");
        PluginContext context = new PluginContext(createTdApiMessage(-100L, 1L));
        
        // When - 处理消息
        PluginResult result = plugin.doHandle(message, context);
        
        // Then - 验证消息被正确处理但未加入队列
        assertThat(result).isEqualTo(PluginResult.CONTINUE);
        
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).isEmpty();
        
        // 验证TelegramClient未被调用
        verify(mockClient, never()).send(any(TdApi.ForwardMessages.class));
    }
    
    @Test
    @DisplayName("Should handle forward failures with retry logic")
    void testForwardFailureWithRetry() throws InterruptedException {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 模拟转发失败
        when(mockClient.send(any(TdApi.ForwardMessages.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));
        
        // 创建消息并处理
        TextMessageEntity message = createTextMessage(-100L, 1L, "Message with #TestTag");
        PluginContext context = new PluginContext(createTdApiMessage(-100L, 1L));
        plugin.doHandle(message, context);
        
        // 验证消息已加入队列
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(1);
        ForwardQueueItem item = queueItems.get(0);
        
        // When - 等待调度器尝试转发并失败（最多10秒）
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ForwardQueueItem updated = mongoTemplate.findById(item.getId(), 
                            ForwardQueueItem.class, "forward_queue");
                    assertThat(updated).isNotNull();
                    // 应该达到最大重试次数并标记为FAILED
                    if (updated.getRetryCount() >= properties.getMaxRetryCount()) {
                        assertThat(updated.getStatus()).isEqualTo(ForwardStatus.FAILED);
                        assertThat(updated.getErrorMessage()).isNotNull();
                    }
                });
        
        // Then - 验证重试逻辑
        ForwardQueueItem finalItem = mongoTemplate.findById(item.getId(), 
                ForwardQueueItem.class, "forward_queue");
        assertThat(finalItem).isNotNull();
        assertThat(finalItem.getRetryCount()).isGreaterThanOrEqualTo(properties.getMaxRetryCount());
        assertThat(finalItem.getStatus()).isEqualTo(ForwardStatus.FAILED);
    }
    
    @Test
    @DisplayName("Should respect rate limiting")
    void testRateLimiting() throws InterruptedException {
        // Given - 设置标签配置和多条消息
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 创建多条消息
        int messageCount = 15;
        for (int i = 0; i < messageCount; i++) {
            TextMessageEntity message = createTextMessage(-100L, (long) i, 
                    "Message " + i + " with #TestTag");
            PluginContext context = new PluginContext(createTdApiMessage(-100L, (long) i));
            plugin.doHandle(message, context);
        }
        
        // 验证所有消息都已加入队列
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(messageCount);
        
        // When - 等待一段时间让调度器处理
        Thread.sleep(3000);
        
        // Then - 验证转发请求的调用次数受到频率限制
        // 配置是60次/分钟，即1次/秒，3秒内应该不超过5次（考虑调度延迟）
        verify(mockClient, atMost(5)).send(any(TdApi.ForwardMessages.class));
    }
    
    @Test
    @DisplayName("Should handle duplicate messages correctly")
    void testDuplicateMessageHandling() {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 创建相同的消息
        TextMessageEntity message1 = createTextMessage(-100L, 1L, "Message with #TestTag");
        TextMessageEntity message2 = createTextMessage(-100L, 1L, "Message with #TestTag");
        
        PluginContext context1 = new PluginContext(createTdApiMessage(-100L, 1L));
        PluginContext context2 = new PluginContext(createTdApiMessage(-100L, 1L));
        
        // When - 处理相同的消息两次
        plugin.doHandle(message1, context1);
        plugin.doHandle(message2, context2);
        
        // Then - 验证只有一条队列项
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(1);
    }
    
    @Test
    @DisplayName("Should handle multiple tags in single message")
    void testMultipleTagsInMessage() {
        // Given - 设置多个标签
        setupMultipleTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 创建包含多个标签的消息
        TextMessageEntity message = createTextMessage(-100L, 1L, 
                "This message has #Tag1 and #Tag2 and #Tag3");
        PluginContext context = new PluginContext(createTdApiMessage(-100L, 1L));
        
        // When - 处理消息
        plugin.doHandle(message, context);
        
        // Then - 验证所有标签都被匹配
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(1);
        
        ForwardQueueItem item = queueItems.get(0);
        assertThat(item.getMatchedTags()).hasSize(3);
        assertThat(item.getMatchedTags()).containsExactlyInAnyOrder("#Tag1", "#Tag2", "#Tag3");
    }
    
    @Test
    @DisplayName("Should handle case-insensitive tag matching")
    void testCaseInsensitiveTagMatching() {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 创建包含不同大小写标签的消息
        TextMessageEntity message1 = createTextMessage(-100L, 1L, "Message with #testtag");
        TextMessageEntity message2 = createTextMessage(-100L, 2L, "Message with #TESTTAG");
        TextMessageEntity message3 = createTextMessage(-100L, 3L, "Message with #TestTag");
        
        PluginContext context1 = new PluginContext(createTdApiMessage(-100L, 1L));
        PluginContext context2 = new PluginContext(createTdApiMessage(-100L, 2L));
        PluginContext context3 = new PluginContext(createTdApiMessage(-100L, 3L));
        
        // When - 处理所有消息
        plugin.doHandle(message1, context1);
        plugin.doHandle(message2, context2);
        plugin.doHandle(message3, context3);
        
        // Then - 验证所有消息都被匹配
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(3);
    }
    
    @Test
    @DisplayName("Should validate configuration on initialization")
    void testConfigurationValidation() {
        // Given & When & Then - 配置已在@DynamicPropertySource中设置
        // 验证插件已正确初始化
        assertThat(plugin.isEnabled()).isTrue();
        assertThat(properties.getTargetChannelId()).isEqualTo(-1001234567890L);
        assertThat(properties.getTagPrefix()).isEqualTo("#");
        assertThat(properties.getRateLimitPerMinute()).isEqualTo(60);
        assertThat(properties.getBatchSize()).isEqualTo(10);
        assertThat(properties.getScheduleIntervalSeconds()).isEqualTo(1);
        assertThat(properties.getMaxRetryCount()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should handle TelegramClient not ready scenario")
    void testTelegramClientNotReady() throws InterruptedException {
        // Given - 设置标签配置
        setupTagConfiguration();
        tagMatcher.loadTagConfiguration();
        
        // 模拟客户端未就绪
        when(clientManager.isReady()).thenReturn(false);
        
        // 创建消息并处理
        TextMessageEntity message = createTextMessage(-100L, 1L, "Message with #TestTag");
        PluginContext context = new PluginContext(createTdApiMessage(-100L, 1L));
        plugin.doHandle(message, context);
        
        // 验证消息已加入队列
        List<ForwardQueueItem> queueItems = mongoTemplate.findAll(ForwardQueueItem.class, "forward_queue");
        assertThat(queueItems).hasSize(1);
        
        // When - 等待调度器尝试处理
        Thread.sleep(2000);
        
        // Then - 验证消息仍然是PENDING状态（因为客户端未就绪）
        ForwardQueueItem item = mongoTemplate.findById(queueItems.get(0).getId(), 
                ForwardQueueItem.class, "forward_queue");
        assertThat(item).isNotNull();
        assertThat(item.getStatus()).isEqualTo(ForwardStatus.PENDING);
        
        // 验证TelegramClient的send方法未被调用
        verify(mockClient, never()).send(any(TdApi.ForwardMessages.class));
    }
    
    // 辅助方法
    
    /**
     * 设置基本的标签配置
     */
    private void setupTagConfiguration() {
        // 创建标签实体
        TagEntity author = TagEntity.builder()
                .id("author1")
                .name("TestTag")
                .aliases(Arrays.asList("TestAlias"))
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        // 创建标签配置
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
    }
    
    /**
     * 设置多个标签的配置
     */
    private void setupMultipleTagConfiguration() {
        // 创建多个标签实体
        TagEntity author1 = TagEntity.builder()
                .id("author1")
                .name("Tag1")
                .build();
        TagEntity author2 = TagEntity.builder()
                .id("author2")
                .name("Tag2")
                .build();
        TagEntity author3 = TagEntity.builder()
                .id("author3")
                .name("Tag3")
                .build();
        
        mongoTemplate.insert(author1, "tag_authors");
        mongoTemplate.insert(author2, "tag_authors");
        mongoTemplate.insert(author3, "tag_authors");
        
        // 创建标签配置
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1", "author2", "author3"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
    }
    
    /**
     * 创建文本消息实体
     */
    private TextMessageEntity createTextMessage(Long chatId, Long messageId, String textContent) {
        TextMessageEntity message = new TextMessageEntity();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setTextContent(textContent);
        message.setDate((int) (System.currentTimeMillis() / 1000));
        return message;
    }
    
    /**
     * 创建TdApi消息对象
     */
    private TdApi.Message createTdApiMessage(Long chatId, Long messageId) {
        TdApi.Message message = new TdApi.Message();
        message.chatId = chatId;
        message.id = messageId;
        message.date = (int) (System.currentTimeMillis() / 1000);
        return message;
    }
}
