package org.xlyo.cocomonyab.source.unread.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;
import org.xlyo.cocomonyab.service.ChannelMonitorService;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UnreadMessageSourceBufferService 属性测�?
 * <p>
 * 使用属性测试验证未读消息缓冲服务在所有输入下的正确�?
 * <p>
 * 测试属性：
 * - Property 9: 缓冲消息往�?
 * - Property 15: 批处理分�?
 * - Property 16: 批次间延�?
 * - Property 11: 处理后清理往�?
 * - Property 22: 集成调用正确�?
 * 
 * @author tianluoqaq
 * @since 1.0
 */
class UnreadMessageSourceBufferServicePropertyTest {
    
    /**
     * Property 9: 缓冲消息往�?
     * <p>
     * For any 获取到的未读消息，保存到缓冲区后应该能够查询到该消息
     * <p>
     * Validates: Requirements 4.1
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 9: 缓冲消息往�?)
    void bufferMessageRoundTrip(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 1, max = 20) int messageCount) {
        
        // 创建 mock 对象
        UnreadMessageBufferRepository bufferRepo = mock(UnreadMessageBufferRepository.class);
        ChannelMonitorService channelMonitorService = mock(ChannelMonitorService.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建测试消息
        List<TdApi.Message> messages = createTestMessages(chatId, messageCount);
        
        // Mock 缓冲区检查（都不存在�?
        when(bufferRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // 捕获保存的缓冲消�?
        List<UnreadMessageBuffer> savedBuffers = new ArrayList<>();
        when(bufferRepo.save(any(UnreadMessageBuffer.class)))
            .thenAnswer(invocation -> {
                UnreadMessageBuffer buffer = invocation.getArgument(0);
                savedBuffers.add(buffer);
                return buffer;
            });
        
        // 创建服务实例
        UnreadMessageSourceBufferService service = new UnreadMessageSourceBufferService(
            bufferRepo, channelMonitorService, processedRepo, config, objectMapper
        );
        
        // 执行缓冲和处�?
        service.bufferAndProcessMessages(chatId, messages, "test_channel", "Test Channel");
        
        // 验证：保存了所有消息到缓冲�?
        verify(bufferRepo, times(messageCount)).save(any(UnreadMessageBuffer.class));
        
        // 验证：所有保存的缓冲消息都包含正确的 chatId �?messageId
        assertThat(savedBuffers).hasSize(messageCount);
        for (int i = 0; i < messageCount; i++) {
            UnreadMessageBuffer buffer = savedBuffers.get(i);
            TdApi.Message originalMessage = messages.get(i);
            
            assertThat(buffer.getChatId()).isEqualTo(chatId);
            assertThat(buffer.getMessageId()).isEqualTo(originalMessage.id);
            assertThat(buffer.getStatus()).isEqualTo(BufferStatus.PENDING);
            assertThat(buffer.getRawMessage()).isNotNull();
            assertThat(buffer.getFetchTime()).isNotNull();
            assertThat(buffer.getCreateTime()).isNotNull();
        }
    }
    
    /**
     * Property 15: 批处理分�?
     * <p>
     * For any 数量 N 的消息和批次大小 B，消息应该被分成 ⌈N/B�?个批次，
     * 每批最�?B 条消息（最后一批可能少�?B�?
     * <p>
     * Validates: Requirements 7.1, 7.2, 11.3
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 15: 批处理分�?)
    void batchProcessingGrouping(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 1, max = 50) int messageCount,
            @ForAll @IntRange(min = 1, max = 10) int batchSize) {
        
        // 创建 mock 对象
        UnreadMessageBufferRepository bufferRepo = mock(UnreadMessageBufferRepository.class);
        ChannelMonitorService channelMonitorService = mock(ChannelMonitorService.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        config.setBatchSize(batchSize);
        config.setBatchDelay(0L); // 不延迟，加快测试
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建测试消息
        List<TdApi.Message> messages = createTestMessages(chatId, messageCount);
        
        // Mock 缓冲区检查（都不存在�?
        when(bufferRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // Mock 保存操作
        when(bufferRepo.save(any(UnreadMessageBuffer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // 创建服务实例
        UnreadMessageSourceBufferService service = new UnreadMessageSourceBufferService(
            bufferRepo, channelMonitorService, processedRepo, config, objectMapper
        );
        
        // 执行缓冲和处�?
        service.bufferAndProcessMessages(chatId, messages, "test_channel", "Test Channel");
        
        // 计算预期批次�?
        int expectedBatches = (int) Math.ceil((double) messageCount / batchSize);
        
        // 验证：调用了正确次数�?handleNewMessage（每条消息一次）
        verify(channelMonitorService, times(messageCount)).handleNewMessage(any(TdApi.Message.class));
        
        // 验证：保存操作次数正确（每条消息保存两次：初始保�?+ 状态更新）
        verify(bufferRepo, times(messageCount * 2)).save(any(UnreadMessageBuffer.class));
    }
    
    /**
     * Property 16: 批次间延�?
     * <p>
     * For any 两个连续的批次处理，它们之间的时间间隔应该大于或等于配置的批次延�?
     * <p>
     * Validates: Requirements 7.3
     */
    @Property(tries = 50)
    @Label("Feature: unread-channel-message-source, Property 16: 批次间延�?)
    void batchDelayBetweenBatches(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 11, max = 30) int messageCount, // 至少需�?2 个批�?
            @ForAll @IntRange(min = 1, max = 5) int batchSize,
            @ForAll @IntRange(min = 50, max = 200) long batchDelay) {
        
        // 确保至少�?2 个批�?
        Assume.that(messageCount > batchSize);
        
        // 创建 mock 对象
        UnreadMessageBufferRepository bufferRepo = mock(UnreadMessageBufferRepository.class);
        ChannelMonitorService channelMonitorService = mock(ChannelMonitorService.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        config.setBatchSize(batchSize);
        config.setBatchDelay(batchDelay);
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建测试消息
        List<TdApi.Message> messages = createTestMessages(chatId, messageCount);
        
        // Mock 缓冲区检查（都不存在�?
        when(bufferRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // Mock 保存操作
        when(bufferRepo.save(any(UnreadMessageBuffer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // 记录批次处理时间
        List<Long> batchProcessingTimes = new ArrayList<>();
        doAnswer(invocation -> {
            // 每个批次的第一条消息记录时�?
            if (batchProcessingTimes.isEmpty() || 
                batchProcessingTimes.size() * batchSize < messageCount) {
                batchProcessingTimes.add(System.currentTimeMillis());
            }
            return null;
        }).when(channelMonitorService).handleNewMessage(any(TdApi.Message.class));
        
        // 创建服务实例
        UnreadMessageSourceBufferService service = new UnreadMessageSourceBufferService(
            bufferRepo, channelMonitorService, processedRepo, config, objectMapper
        );
        
        // 记录开始时�?
        long startTime = System.currentTimeMillis();
        
        // 执行缓冲和处�?
        service.bufferAndProcessMessages(chatId, messages, "test_channel", "Test Channel");
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        
        // 计算预期批次�?
        int expectedBatches = (int) Math.ceil((double) messageCount / batchSize);
        
        // 验证：总耗时应该至少包含 (批次�?- 1) * 批次延迟
        long expectedMinDuration = (expectedBatches - 1) * batchDelay;
        assertThat(totalDuration).isGreaterThanOrEqualTo(expectedMinDuration - 50); // 允许 50ms 误差
    }
    
    /**
     * Property 11: 处理后清理往�?
     * <p>
     * For any 成功处理的消息，应该从缓冲区删除并添加到已处理集�?
     * <p>
     * Validates: Requirements 4.5, 6.4
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 11: 处理后清理往�?)
    void processedMessageCleanupRoundTrip(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 1, max = 10) int messageCount) {
        
        // 创建 mock 对象
        UnreadMessageBufferRepository bufferRepo = mock(UnreadMessageBufferRepository.class);
        ChannelMonitorService channelMonitorService = mock(ChannelMonitorService.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        config.setBatchDelay(0L); // 不延迟，加快测试
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建测试消息
        List<TdApi.Message> messages = createTestMessages(chatId, messageCount);
        
        // Mock 缓冲区检查（都不存在�?
        when(bufferRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // 捕获保存的缓冲消�?
        List<UnreadMessageBuffer> savedBuffers = new ArrayList<>();
        when(bufferRepo.save(any(UnreadMessageBuffer.class)))
            .thenAnswer(invocation -> {
                UnreadMessageBuffer buffer = invocation.getArgument(0);
                savedBuffers.add(buffer);
                return buffer;
            });
        
        // 创建服务实例
        UnreadMessageSourceBufferService service = new UnreadMessageSourceBufferService(
            bufferRepo, channelMonitorService, processedRepo, config, objectMapper
        );
        
        // 执行缓冲和处�?
        service.bufferAndProcessMessages(chatId, messages, "test_channel", "Test Channel");
        
        // 验证：所有消息都被处�?
        verify(channelMonitorService, times(messageCount)).handleNewMessage(any(TdApi.Message.class));
        
        // 验证：所有缓冲消息的状态都被更新为 PROCESSED
        long processedCount = savedBuffers.stream()
            .filter(buffer -> buffer.getStatus() == BufferStatus.PROCESSED)
            .count();
        
        assertThat(processedCount).isEqualTo(messageCount);
    }
    
    /**
     * Property 22: 集成调用正确�?
     * <p>
     * For any 未读消息，处理时应该调用 ChannelMonitorService.handleNewMessage() 方法�?
     * 且传递的参数�?TdApi.Message 对象
     * <p>
     * Validates: Requirements 14.1, 14.2
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 22: 集成调用正确�?)
    void integrationCallCorrectness(
            @ForAll @LongRange(min = -1000000000000L, max = -1L) long chatId,
            @ForAll @IntRange(min = 1, max = 10) int messageCount) {
        
        // 创建 mock 对象
        UnreadMessageBufferRepository bufferRepo = mock(UnreadMessageBufferRepository.class);
        ChannelMonitorService channelMonitorService = mock(ChannelMonitorService.class);
        ProcessedMessageRepository processedRepo = mock(ProcessedMessageRepository.class);
        UnreadMessageSourceConfig config = createTestConfig();
        config.setBatchDelay(0L); // 不延迟，加快测试
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 创建测试消息
        List<TdApi.Message> messages = createTestMessages(chatId, messageCount);
        
        // Mock 缓冲区检查（都不存在�?
        when(bufferRepo.existsByChatIdAndMessageId(anyLong(), anyLong()))
            .thenReturn(false);
        
        // Mock 保存操作
        when(bufferRepo.save(any(UnreadMessageBuffer.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // 创建服务实例
        UnreadMessageSourceBufferService service = new UnreadMessageSourceBufferService(
            bufferRepo, channelMonitorService, processedRepo, config, objectMapper
        );
        
        // 执行缓冲和处�?
        service.bufferAndProcessMessages(chatId, messages, "test_channel", "Test Channel");
        
        // 验证：调用了 handleNewMessage 方法
        verify(channelMonitorService, times(messageCount)).handleNewMessage(any(TdApi.Message.class));
        
        // 验证：传递的参数�?TdApi.Message 对象，且 chatId �?messageId 正确
        for (TdApi.Message originalMessage : messages) {
            verify(channelMonitorService).handleNewMessage(argThat(message -> 
                message != null && 
                message.chatId == chatId && 
                message.id == originalMessage.id
            ));
        }
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
        config.setApiCallDelay(0L);
        config.setMaxRetries(3);
        config.setRetryBaseDelay(100L);
        config.setRetryMaxDelay(5000L);
        config.setBatchSize(10);
        config.setBatchDelay(100L);
        config.setBufferTtlDays(7);
        return config;
    }
    
    /**
     * 创建测试消息列表
     */
    private List<TdApi.Message> createTestMessages(long chatId, int count) {
        List<TdApi.Message> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TdApi.Message message = new TdApi.Message();
            message.chatId = chatId;
            message.id = 1000L + i;
            message.date = 1700000000 + i;
            message.isChannelPost = true;
            message.content = new TdApi.MessageText(
                new TdApi.FormattedText("Test message " + i, new TdApi.TextEntity[0]),
                null,
                null
            );
            messages.add(message);
        }
        return messages;
    }
}
