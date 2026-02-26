package org.xlyo.cocomonyab.source.unread.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;
import org.xlyo.cocomonyab.service.ChannelMonitorService;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.metrics.UnreadMessageMetrics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 未读消息缓冲服务
 * <p>
 * 管理未读消息缓冲区，实现批量处理和去重。
 * 负责将获取到的未读消息保存到缓冲区，并按批次处理这些消息。
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadMessageBufferService {
    
    private final UnreadMessageBufferRepository bufferRepository;
    private final ChannelMonitorService channelMonitorService;
    private final UnreadMessageSourceConfig config;
    private final ObjectMapper objectMapper;
    private final UnreadMessageMetrics metrics;
    
    /**
     * 缓冲并处理消息
     * 
     * @param chatId 频道 ID
     * @param messages 消息列表
     * @param channelUsername 频道用户名
     * @param channelTitle 频道标题
     */
    public void bufferAndProcessMessages(
        long chatId,
        List<TdApi.Message> messages,
        String channelUsername,
        String channelTitle
    ) {
        log.info("📥 [未读消息] 开始缓冲消息: chatId={}, channel={}, 消息数量={}", 
            chatId, channelTitle != null ? channelTitle : "未知频道", messages.size());
        
        // 保存到缓冲区
        List<UnreadMessageBuffer> buffers = saveToBuffer(chatId, messages);
        
        log.info("💾 [未读消息] 已保存到缓冲区: chatId={}, 保存数量={}", chatId, buffers.size());
        
        // 分批处理
        processBatches(buffers, channelUsername, channelTitle);
        
        log.info("✅ [未读消息] 消息处理完成: chatId={}, 处理数量={}", chatId, buffers.size());
    }
    
    /**
     * 保存消息到缓冲区
     * 
     * @param chatId 频道 ID
     * @param messages 消息列表
     * @return 保存的缓冲消息列表
     */
    private List<UnreadMessageBuffer> saveToBuffer(
        long chatId, List<TdApi.Message> messages
    ) {
        List<UnreadMessageBuffer> buffers = new ArrayList<>();
        
        for (TdApi.Message message : messages) {
            // 检查是否已存在
            if (bufferRepository.existsByChatIdAndMessageId(chatId, message.id)) {
                log.debug("消息已在缓冲区中: chatId={}, messageId={}", 
                    chatId, message.id);
                continue;
            }
            
            UnreadMessageBuffer buffer = UnreadMessageBuffer.builder()
                .chatId(chatId)
                .messageId(message.id)
                .fetchTime(LocalDateTime.now())
                .status(BufferStatus.PENDING)
                .rawMessage(serializeMessage(message))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
            
            bufferRepository.save(buffer);
            buffers.add(buffer);
        }
        
        return buffers;
    }
    
    /**
     * 分批处理消息
     * 
     * @param buffers 缓冲消息列表
     * @param channelUsername 频道用户名
     * @param channelTitle 频道标题
     */
    private void processBatches(
        List<UnreadMessageBuffer> buffers,
        String channelUsername,
        String channelTitle
    ) {
        int batchSize = config.getBatchSize();
        int totalBatches = (int) Math.ceil((double) buffers.size() / batchSize);
        
        log.info("开始批量处理: 总数={}, 批次大小={}, 批次数={}", 
            buffers.size(), batchSize, totalBatches);
        
        for (int i = 0; i < totalBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, buffers.size());
            List<UnreadMessageBuffer> batch = buffers.subList(start, end);
            
            log.info("处理批次 {}/{}: 消息数={}", i + 1, totalBatches, batch.size());
            
            processBatch(batch, channelUsername, channelTitle);
            
            // 批次之间添加延迟
            if (i < totalBatches - 1) {
                sleepBetweenBatches();
            }
        }
    }
    
    /**
     * 处理单个批次
     * 
     * @param batch 批次消息列表
     * @param channelUsername 频道用户名
     * @param channelTitle 频道标题
     */
    private void processBatch(
        List<UnreadMessageBuffer> batch,
        String channelUsername,
        String channelTitle
    ) {
        for (UnreadMessageBuffer buffer : batch) {
            try {
                metrics.timeProcessing(() -> {
                    processMessage(buffer, channelUsername, channelTitle);
                    return null;
                });
                
                // 标记为已处理
                buffer.setStatus(BufferStatus.PROCESSED);
                buffer.setUpdateTime(LocalDateTime.now());
                bufferRepository.save(buffer);
                
                // 记录成功处理
                metrics.recordMessageProcessed();
                
            } catch (Exception e) {
                log.error("处理消息失败: chatId={}, messageId={}, error={}", 
                    buffer.getChatId(), buffer.getMessageId(), e.getMessage(), e);
                
                // 标记为失败
                buffer.setStatus(BufferStatus.FAILED);
                buffer.setErrorMessage(truncateErrorMessage(e.getMessage()));
                buffer.setUpdateTime(LocalDateTime.now());
                bufferRepository.save(buffer);
                
                // 记录失败
                metrics.recordMessageFailed();
            }
        }
        
        // 更新缓冲区大小指标
        updateBufferMetrics();
    }
    
    /**
     * 处理单条消息
     * 
     * @param buffer 缓冲消息
     * @param channelUsername 频道用户名
     * @param channelTitle 频道标题
     */
    private void processMessage(
        UnreadMessageBuffer buffer,
        String channelUsername,
        String channelTitle
    ) {
        // 反序列化消息
        TdApi.Message message = deserializeMessage(buffer.getRawMessage());
        
        // 调用 ChannelMonitorService 处理
        channelMonitorService.handleNewMessage(message);
        
        log.debug("消息处理完成: chatId={}, messageId={}", 
            buffer.getChatId(), buffer.getMessageId());
    }
    
    /**
     * 处理待处理的缓冲消息（程序重启后）
     */
    public void processPendingMessages() {
        List<UnreadMessageBuffer> pendingBuffers = bufferRepository
            .findByStatus(BufferStatus.PENDING);
        
        if (pendingBuffers.isEmpty()) {
            return;
        }
        
        log.info("开始处理待处理的缓冲消息: 数量={}", pendingBuffers.size());
        
        // 按频道分组
        Map<Long, List<UnreadMessageBuffer>> groupedByChannel = pendingBuffers.stream()
            .collect(Collectors.groupingBy(UnreadMessageBuffer::getChatId));
        
        // 对每个频道处理
        for (Map.Entry<Long, List<UnreadMessageBuffer>> entry : groupedByChannel.entrySet()) {
            long chatId = entry.getKey();
            List<UnreadMessageBuffer> buffers = entry.getValue();
            
            log.info("处理频道的待处理消息: chatId={}, 数量={}", chatId, buffers.size());
            
            // 获取频道信息（简化处理，使用空字符串）
            String channelUsername = "";
            String channelTitle = "";
            
            processBatches(buffers, channelUsername, channelTitle);
        }
    }
    
    /**
     * 统计待处理消息数量
     * 
     * @return 待处理消息数量
     */
    public long countPendingMessages() {
        return bufferRepository.countByStatus(BufferStatus.PENDING);
    }
    
    /**
     * 统计失败消息数量
     * 
     * @return 失败消息数量
     */
    public long countFailedMessages() {
        return bufferRepository.countByStatus(BufferStatus.FAILED);
    }
    
    /**
     * 批次之间的延迟
     */
    private void sleepBetweenBatches() {
        try {
            Thread.sleep(config.getBatchDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("批次延迟被中断", e);
        }
    }
    
    /**
     * 序列化消息为 JSON
     * 
     * @param message TdApi.Message 对象
     * @return JSON 字符串
     */
    private String serializeMessage(TdApi.Message message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化消息失败: chatId={}, messageId={}", 
                message.chatId, message.id, e);
            throw new RuntimeException("消息序列化失败", e);
        }
    }
    
    /**
     * 反序列化 JSON 为消息
     * 
     * @param json JSON 字符串
     * @return TdApi.Message 对象
     */
    private TdApi.Message deserializeMessage(String json) {
        try {
            return objectMapper.readValue(json, TdApi.Message.class);
        } catch (Exception e) {
            log.error("反序列化消息失败: json={}", json, e);
            throw new RuntimeException("消息反序列化失败", e);
        }
    }
    
    /**
     * 截断错误消息
     * 
     * @param errorMessage 错误消息
     * @return 截断后的错误消息
     */
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= 500) {
            return errorMessage;
        }
        return errorMessage.substring(0, 500);
    }
    
    /**
     * 更新缓冲区大小指标
     */
    private void updateBufferMetrics() {
        int pending = (int) countPendingMessages();
        int failed = (int) countFailedMessages();
        metrics.updateBufferSizes(pending, failed);
    }
}
