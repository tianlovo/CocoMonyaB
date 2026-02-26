package org.xlyo.cocomonyab.source.unread.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import it.tdlight.jni.TdApi;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;
import org.xlyo.cocomonyab.source.unread.exception.PermanentErrorException;
import org.xlyo.cocomonyab.source.unread.exception.RateLimitException;
import org.xlyo.cocomonyab.source.unread.exception.TemporaryErrorException;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageStatistics;

/**
 * 错误处理器
 * <p>
 * 负责处理未读消息获取和处理过程中的各类错误，包括：
 * <ul>
 *   <li>API 错误分类和处理（速率限制、临时错误、永久错误）</li>
 *   <li>消息处理错误（标记失败并更新统计）</li>
 *   <li>频道处理错误（记录错误不中断流程）</li>
 * </ul>
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {
    
    private final UnreadMessageBufferRepository bufferRepository;
    
    /**
     * 处理 API 错误
     * <p>
     * 根据错误代码分类处理：
     * <ul>
     *   <li>429: 速率限制错误，抛出 RateLimitException 进行指数退避重试</li>
     *   <li>5xx: 临时错误，抛出 TemporaryErrorException 进行固定延迟重试</li>
     *   <li>4xx: 永久错误，抛出 PermanentErrorException 记录并跳过</li>
     * </ul>
     * 
     * @param error Telegram API 错误对象
     * @param chatId 频道 ID
     * @throws RateLimitException 速率限制错误
     * @throws TemporaryErrorException 临时错误
     * @throws PermanentErrorException 永久错误
     */
    public void handleApiError(TdApi.Error error, long chatId) {
        if (isRateLimitError(error)) {
            log.warn("速率限制: chatId={}, code={}", chatId, error.code);
            throw new RateLimitException(error);
            
        } else if (isTemporaryError(error)) {
            log.warn("临时错误: chatId={}, code={}, message={}", 
                chatId, error.code, error.message);
            throw new TemporaryErrorException(error);
            
        } else {
            log.error("永久错误: chatId={}, code={}, message={}", 
                chatId, error.code, error.message);
            throw new PermanentErrorException(error);
        }
    }
    
    /**
     * 处理消息处理错误
     * <p>
     * 当消息处理失败时：
     * <ul>
     *   <li>记录详细的错误日志</li>
     *   <li>将缓冲区消息标记为 FAILED 状态</li>
     *   <li>保存错误信息（截断到 500 字符）</li>
     *   <li>更新统计信息中的失败计数</li>
     * </ul>
     * 
     * @param e 处理异常
     * @param buffer 缓冲区消息
     * @param statistics 统计信息对象
     */
    public void handleProcessingError(Exception e, UnreadMessageBuffer buffer, 
                                     UnreadMessageStatistics statistics) {
        log.error("处理消息失败: chatId={}, messageId={}, error={}", 
            buffer.getChatId(), buffer.getMessageId(), e.getMessage(), e);
        
        // 标记为失败
        buffer.setStatus(BufferStatus.FAILED);
        buffer.setErrorMessage(truncate(e.getMessage(), 500));
        bufferRepository.save(buffer);
        
        // 更新统计
        statistics.incrementFailedMessages();
    }
    
    /**
     * 处理频道错误
     * <p>
     * 当处理频道失败时：
     * <ul>
     *   <li>记录详细的错误日志</li>
     *   <li>不抛出异常，允许继续处理下一个频道</li>
     *   <li>更新统计信息中的失败频道计数</li>
     * </ul>
     * 这确保单个频道的错误不会中断整体处理流程
     * 
     * @param e 处理异常
     * @param channel 频道对象
     * @param statistics 统计信息对象
     */
    public void handleChannelError(Exception e, Channel channel, 
                                  UnreadMessageStatistics statistics) {
        log.error("处理频道失败: channelId={}, error={}", 
            channel.getChannelId(), e.getMessage(), e);
        
        // 不抛出异常，继续处理下一个频道
        statistics.incrementFailedChannels();
    }
    
    /**
     * 判断是否为速率限制错误
     * 
     * @param error Telegram API 错误对象
     * @return true 如果是速率限制错误（429），false 否则
     */
    private boolean isRateLimitError(TdApi.Error error) {
        return error.code == 429;
    }
    
    /**
     * 判断是否为临时错误
     * 
     * @param error Telegram API 错误对象
     * @return true 如果是临时错误（5xx），false 否则
     */
    private boolean isTemporaryError(TdApi.Error error) {
        return error.code >= 500 && error.code < 600;
    }
    
    /**
     * 截断字符串到指定长度
     * 
     * @param str 原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串，如果原始字符串为 null 则返回 null
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
