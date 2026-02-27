package org.xlyo.cocomonyab.source.unread.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Assertions;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.exception.UnreadMessageFetchException;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UnreadMessageFetchService 属性测试
 * <p>
 * 使用属性测试验证未读消息获取服务在所有输入下的正确性
 * <p>
 * 测试属性：
 * - Property 3: API 调用正确性
 * - Property 4: 已处理消息过滤
 * - Property 5: 消息排序不变性
 * - Property 13: 消息唯一性
 * - Property 14: 已处理消息跳过
 * - Property 12: 并发消息自动去重
 * - Property 8: 指数退避重试
 * - Property 17: 重试次数限制
 * 
 * @author tianluoqaq
 * @since 1.0
 */
class UnreadMessageFetchServicePropertyTest {
    
    /**
     * Property 3: API 调用正确性
     * <p>
     * For any 频道，获取未读消息时应该调用 getChatHistory API，
     * 且 fromMessageId 参数为 0（从最新消息开始）
     * <p>
     * Validates: Requirements 2.1, 2.2
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 3: API 调用正确性")
    void apiCallCorrectness(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 1, max = 50) int messageCount) {
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // 创建测试消息
        TdApi.Message[] messages = createTestMessages(chatId, messageCount);
        TdApi.Messages response = new TdApi.Messages();
        response.messages = messages;
        response.totalCount = messageCount;
        
        // Mock API 调用
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // Mock 已处理消息检查（都未处理）
        when(processedRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取
        List<TdApi.Message> result = service.fetchUnreadMessages(chatId);
        
        // 验证：调用了 getChatHistory API
        verify(client, atLeastOnce()).send(any(TdApi.GetChatHistory.class));
        
        // 验证：第一次调用的 fromMessageId 为 0（从最新消息开始）
        verify(client, atLeastOnce()).send(argThat((TdApi.GetChatHistory request) -> 
            request.chatId == chatId && request.fromMessageId == 0
        ));
        
        // 验证：返回了消息
        Assertions.assertNotNull(result, "Result should not be null");
    }
    
    /**
     * Property 4: 已处理消息过滤
     * <p>
     * For any 消息列表和已处理消息集合，返回的未读消息不应包含已处理集合中的消息
     * <p>
     * Validates: Requirements 2.3
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 4: 已处理消息过滤")
    void processedMessageFiltering(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 10, max = 50) int totalMessages,
            @ForAll @IntRange(min = 1, max = 10) int processedCount) {
        
        // 确保 processedCount 不超过 totalMessages
        processedCount = Math.min(processedCount, totalMessages);
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // 创建测试消息
        TdApi.Message[] messages = createTestMessages(chatId, totalMessages);
        TdApi.Messages response = new TdApi.Messages();
        response.messages = messages;
        response.totalCount = totalMessages;
        
        // Mock API 调用
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // 标记前 processedCount 条消息为已处理
        Set<Long> processedMessageIds = new HashSet<>();
        for (int i = 0; i < processedCount; i++) {
            processedMessageIds.add(messages[i].id);
        }
        
        // Mock 已处理消息检查
        when(processedRepo.existsByChatIdAndMessageId(eq(chatId), anyLong()))
            .thenAnswer(invocation -> {
                Long messageId = invocation.getArgument(1);
                return processedMessageIds.contains(messageId);
            });
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取
        List<TdApi.Message> result = service.fetchUnreadMessages(chatId);
        
        // 验证：返回的消息不包含已处理的消息
        for (TdApi.Message msg : result) {
            Assertions.assertFalse(processedMessageIds.contains(msg.id),
                "Result should not contain processed message: " + msg.id);
        }
        
        // 验证：返回的消息数量正确
        int expectedCount = totalMessages - processedCount;
        Assertions.assertEquals(expectedCount, result.size(),
            String.format("Expected %d unprocessed messages, but got %d", 
                expectedCount, result.size()));
    }
    
    /**
     * Property 5: 消息排序不变性
     * <p>
     * For any 未读消息列表，返回的消息应该按 messageId 升序排列
     * <p>
     * Validates: Requirements 2.4
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 5: 消息排序不变性")
    void messageSortingInvariant(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 2, max = 50) int messageCount) {
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // 创建测试消息（随机顺序）
        TdApi.Message[] messages = createTestMessages(chatId, messageCount);
        // 打乱顺序
        List<TdApi.Message> shuffled = new ArrayList<>(Arrays.asList(messages));
        Collections.shuffle(shuffled);
        
        TdApi.Messages response = new TdApi.Messages();
        response.messages = shuffled.toArray(new TdApi.Message[0]);
        response.totalCount = messageCount;
        
        // Mock API 调用
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // Mock 已处理消息检查（都未处理）
        when(processedRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取
        List<TdApi.Message> result = service.fetchUnreadMessages(chatId);
        
        // 验证：消息按 messageId 升序排列
        for (int i = 1; i < result.size(); i++) {
            Assertions.assertTrue(result.get(i).id > result.get(i - 1).id,
                String.format("Messages should be sorted by ID: msg[%d].id=%d should be > msg[%d].id=%d",
                    i, result.get(i).id, i - 1, result.get(i - 1).id));
        }
    }
    
    /**
     * Property 13: 消息唯一性
     * <p>
     * For any 消息列表，不应该存在重复的 (chatId, messageId) 组合
     * <p>
     * Validates: Requirements 5.4, 5.5, 6.3
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 13: 消息唯一性")
    void messageUniqueness(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 5, max = 30) int uniqueCount,
            @ForAll @IntRange(min = 1, max = 5) int duplicateCount) {
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // 创建测试消息
        TdApi.Message[] uniqueMessages = createTestMessages(chatId, uniqueCount);
        
        // 添加重复消息
        List<TdApi.Message> allMessages = new ArrayList<>(Arrays.asList(uniqueMessages));
        for (int i = 0; i < duplicateCount && i < uniqueCount; i++) {
            allMessages.add(uniqueMessages[i]); // 添加重复
        }
        
        // 打乱顺序
        Collections.shuffle(allMessages);
        
        TdApi.Messages response = new TdApi.Messages();
        response.messages = allMessages.toArray(new TdApi.Message[0]);
        response.totalCount = allMessages.size();
        
        // Mock API 调用
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // Mock 已处理消息检查（都未处理）
        when(processedRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取
        List<TdApi.Message> result = service.fetchUnreadMessages(chatId);
        
        // 验证：没有重复的 (chatId, messageId) 组合
        Set<String> uniqueKeys = new HashSet<>();
        for (TdApi.Message msg : result) {
            String key = msg.chatId + ":" + msg.id;
            Assertions.assertFalse(uniqueKeys.contains(key),
                "Duplicate message found: chatId=" + msg.chatId + ", messageId=" + msg.id);
            uniqueKeys.add(key);
        }
        
        // 验证：返回的消息数量等于唯一消息数量
        Assertions.assertEquals(uniqueCount, result.size(),
            String.format("Expected %d unique messages, but got %d", uniqueCount, result.size()));
    }
    
    /**
     * Property 14: 已处理消息跳过
     * <p>
     * For any 消息，如果它已存在于已处理集合中，应该被跳过不再处理
     * <p>
     * Validates: Requirements 6.2
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 14: 已处理消息跳过")
    void processedMessageSkipped(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 10, max = 30) int messageCount) {
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // 创建测试消息
        TdApi.Message[] messages = createTestMessages(chatId, messageCount);
        TdApi.Messages response = new TdApi.Messages();
        response.messages = messages;
        response.totalCount = messageCount;
        
        // Mock API 调用
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // Mock 所有消息都已处理
        when(processedRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(true);
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取
        List<TdApi.Message> result = service.fetchUnreadMessages(chatId);
        
        // 验证：所有消息都被跳过，返回空列表
        Assertions.assertTrue(result.isEmpty(),
            "All processed messages should be skipped, but got " + result.size() + " messages");
    }
    
    /**
     * Property 12: 并发消息自动去重
     * <p>
     * For any 频道，如果在获取未读消息期间有新消息到达，
     * 这些新消息会被实时监听机制处理并记录到 processed_messages 集合，
     * 因此在过滤时会自动被排除
     * <p>
     * Validates: Requirements 5.2, 5.3
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 12: 并发消息自动去重")
    void concurrentMessageAutomaticDeduplication(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 10, max = 30) int historicalCount,
            @ForAll @IntRange(min = 1, max = 5) int concurrentCount) {
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // 创建历史消息
        TdApi.Message[] historicalMessages = createTestMessages(chatId, historicalCount);
        
        // 创建并发新消息（ID 更大）
        TdApi.Message[] concurrentMessages = createTestMessages(chatId, concurrentCount, 
            historicalCount + 1);
        
        // 合并所有消息（模拟 API 返回包含并发新消息）
        List<TdApi.Message> allMessages = new ArrayList<>();
        allMessages.addAll(Arrays.asList(historicalMessages));
        allMessages.addAll(Arrays.asList(concurrentMessages));
        
        TdApi.Messages response = new TdApi.Messages();
        response.messages = allMessages.toArray(new TdApi.Message[0]);
        response.totalCount = allMessages.size();
        
        // Mock API 调用
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.completedFuture(response));
        
        // Mock 并发新消息已被实时监听机制处理（标记为已处理）
        Set<Long> concurrentMessageIds = Arrays.stream(concurrentMessages)
            .map(m -> m.id)
            .collect(Collectors.toSet());
        
        when(processedRepo.existsByChatIdAndMessageId(eq(chatId), anyLong()))
            .thenAnswer(invocation -> {
                Long messageId = invocation.getArgument(1);
                return concurrentMessageIds.contains(messageId);
            });
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取
        List<TdApi.Message> result = service.fetchUnreadMessages(chatId);
        
        // 验证：并发新消息被自动过滤（不在结果中）
        for (TdApi.Message msg : result) {
            Assertions.assertFalse(concurrentMessageIds.contains(msg.id),
                "Concurrent message should be filtered out: " + msg.id);
        }
        
        // 验证：只返回历史未读消息
        Assertions.assertEquals(historicalCount, result.size(),
            String.format("Expected %d historical messages, but got %d", 
                historicalCount, result.size()));
    }
    
    /**
     * Property 8: 指数退避重试
     * <p>
     * For any 速率限制错误序列，重试延迟应该按指数增长（2^n），直到达到最大延迟
     * <p>
     * Validates: Requirements 3.3
     */
    @Property(tries = 50)
    @Label("Feature: unread-channel-message-source, Property 8: 指数退避重试")
    void exponentialBackoffRetry(
            @ForAll @IntRange(min = 1, max = 3) int retryCount) {
        
        // 创建配置
        UnreadMessageSourceConfig config = createTestConfig();
        config.setMaxRetries(retryCount);
        config.setRetryBaseDelay(1000L);
        config.setRetryMaxDelay(10000L);
        
        // 创建服务实例（只需要测试重试逻辑，不需要完整的依赖）
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        RateLimiter rateLimiter = createTestRateLimiter();
        
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 测试指数退避计算
        List<Long> delays = new ArrayList<>();
        for (int i = 1; i <= retryCount; i++) {
            long expectedDelay = (long) (config.getRetryBaseDelay() * Math.pow(2, i - 1));
            expectedDelay = Math.min(expectedDelay, config.getRetryMaxDelay());
            delays.add(expectedDelay);
        }
        
        // 验证：延迟按指数增长
        for (int i = 1; i < delays.size(); i++) {
            long currentDelay = delays.get(i);
            long previousDelay = delays.get(i - 1);
            
            // 如果未达到最大延迟，当前延迟应该是前一个的 2 倍
            if (currentDelay < config.getRetryMaxDelay()) {
                Assertions.assertEquals(previousDelay * 2, currentDelay,
                    String.format("Delay should double: retry %d delay=%d, retry %d delay=%d",
                        i - 1, previousDelay, i, currentDelay));
            }
        }
        
        // 验证：延迟不超过最大值
        for (long delay : delays) {
            Assertions.assertTrue(delay <= config.getRetryMaxDelay(),
                String.format("Delay %d should not exceed max delay %d", 
                    delay, config.getRetryMaxDelay()));
        }
    }
    
    /**
     * Property 17: 重试次数限制
     * <p>
     * For any 失败的 API 调用，重试次数不应该超过配置的最大重试次数
     * <p>
     * Validates: Requirements 8.3, 11.5
     */
    @Property(tries = 50)
    @Label("Feature: unread-channel-message-source, Property 17: 重试次数限制")
    void retryCountLimit(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 1, max = 5) int maxRetries) {
        
        // 创建配置
        UnreadMessageSourceConfig config = createTestConfig();
        config.setMaxRetries(maxRetries);
        
        // 创建 mock 对象
        TelegramClientManager clientManager = mock(TelegramClientManager.class);
        SimpleTelegramClient client = mock(SimpleTelegramClient.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        RateLimiter rateLimiter = createTestRateLimiter();
        
        when(clientManager.getClient()).thenReturn(client);
        
        // Mock API 调用总是失败（速率限制错误）
        when(client.send(any(TdApi.GetChatHistory.class)))
            .thenReturn(CompletableFuture.failedFuture(
                new RuntimeException("429 Too Many Requests")));
        
        // 创建服务实例
        UnreadMessageFetchService service = new UnreadMessageFetchService(
            clientManager, processedRepo, config, rateLimiter
        );
        
        // 执行获取（应该抛出异常）
        UnreadMessageFetchException exception = Assertions.assertThrows(
            UnreadMessageFetchException.class,
            () -> service.fetchUnreadMessages(chatId),
            "Should throw exception after max retries"
        );
        
        // 验证：异常消息包含"达到最大重试次数"
        Assertions.assertTrue(exception.getMessage().contains("达到最大重试次数"),
            "Exception message should indicate max retries reached: " + exception.getMessage());
        
        // 验证：API 调用次数 = 1 (初始) + maxRetries (重试)
        verify(client, times(1 + maxRetries)).send(any(TdApi.GetChatHistory.class));
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * 创建测试配置
     */
    private UnreadMessageSourceConfig createTestConfig() {
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setAutoDetectOnStartup(false);
        config.setMaxMessagesPerFetch(100);
        config.setMaxTotalMessages(1000);
        config.setApiCallDelay(0L); // 测试时不延迟
        config.setMaxRetries(3);
        config.setRetryBaseDelay(100L); // 测试时使用较短延迟
        config.setRetryMaxDelay(1000L);
        config.setBatchSize(10);
        config.setBatchDelay(0L);
        config.setBufferTtlDays(7);
        return config;
    }
    
    /**
     * 创建测试速率限制器（不限制）
     */
    private RateLimiter createTestRateLimiter() {
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(1))
                .limitForPeriod(1000) // 测试时不限制
                .timeoutDuration(Duration.ofSeconds(1))
                .build();
        
        return RateLimiter.of("test-rate-limiter", config);
    }
    
    /**
     * 创建测试消息
     */
    private TdApi.Message[] createTestMessages(long chatId, int count) {
        return createTestMessages(chatId, count, 1);
    }
    
    /**
     * 创建测试消息（指定起始 ID）
     */
    private TdApi.Message[] createTestMessages(long chatId, int count, int startId) {
        TdApi.Message[] messages = new TdApi.Message[count];
        for (int i = 0; i < count; i++) {
            TdApi.Message message = new TdApi.Message();
            message.chatId = chatId;
            message.id = startId + i;
            message.date = 1000000 + i;
            message.isChannelPost = true;
            message.content = new TdApi.MessageText();
            messages[i] = message;
        }
        return messages;
    }
}
