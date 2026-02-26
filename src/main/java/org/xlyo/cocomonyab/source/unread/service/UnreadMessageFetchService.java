package org.xlyo.cocomonyab.source.unread.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.exception.UnreadMessageFetchException;
import org.xlyo.cocomonyab.source.unread.metrics.UnreadMessageMetrics;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 未读消息获取服务
 * <p>
 * 负责从 TDLib 获取未读消息，实现速率限制控制和错误处理。
 * <p>
 * 核心功能：
 * <ul>
 *   <li>调用 getChatHistory API 获取历史消息</li>
 *   <li>应用速率限制避免触发 Telegram API 限制</li>
 *   <li>过滤已处理的消息</li>
 *   <li>消息排序（按 messageId 升序）和去重</li>
 *   <li>错误处理和重试机制（指数退避）</li>
 * </ul>
 * <p>
 * 并发新消息处理策略：
 * 在获取未读消息期间，如果频道有新消息到达，这些消息会被实时监听机制捕获
 * 并自动交给 TagBasedMessageForwardingPlugin 处理。该插件会将处理的消息
 * 记录到 processed_messages 集合中。
 * <p>
 * 因此，本服务不需要主动检测和获取并发新消息，只需：
 * <ol>
 *   <li>记录开始获取时的位置（用于日志和调试）</li>
 *   <li>正常获取历史未读消息</li>
 *   <li>依赖 processed_messages 集合自动过滤已处理的消息</li>
 * </ol>
 * <p>
 * 这种设计避免了重复处理，简化了并发控制逻辑。
 * 
 * @author CocoMonya Team
 * @since 1.0
 * @see UnreadMessageSourceConfig
 * @see ProcessedMessageRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadMessageFetchService {
    
    private final TelegramClientManager telegramClientManager;
    private final ProcessedMessageRepository processedMessageRepository;
    private final UnreadMessageSourceConfig config;
    private final RateLimiter rateLimiter;
    private final UnreadMessageMetrics metrics;
    
    /**
     * 获取频道的未读消息
     * <p>
     * 从最新消息开始向历史方向查询，过滤已处理的消息，
     * 按消息 ID 升序排序并去重。
     * 
     * @param chatId 频道 ID
     * @return 未读消息列表，按 messageId 升序排序
     * @throws UnreadMessageFetchException 如果获取失败
     */
    public List<TdApi.Message> fetchUnreadMessages(long chatId) {
        List<TdApi.Message> allMessages = new ArrayList<>();
        long fromMessageId = 0; // 从最新消息开始
        int retryCount = 0;
        
        // 记录开始时的最新消息 ID（用于日志和调试）
        long startingLatestMessageId = getLatestMessageId(chatId);
        log.debug("开始获取未读消息: chatId={}, startingLatestMessageId={}", 
            chatId, startingLatestMessageId);
        
        while (true) {
            try {
                // 应用速率限制
                waitForRateLimit();
                
                // 记录 API 调用
                metrics.recordApiCall();
                
                // 调用 getChatHistory API
                TdApi.GetChatHistory request = new TdApi.GetChatHistory(
                    chatId,
                    fromMessageId,
                    0, // offset
                    config.getMaxMessagesPerFetch(),
                    false // onlyLocal
                );
                
                TdApi.Messages messages = metrics.timeApiCall(() -> sendRequest(request));
                
                if (messages.messages.length == 0) {
                    break; // 没有更多消息
                }
                
                // 过滤已处理的消息
                List<TdApi.Message> unprocessedMessages = filterUnprocessedMessages(
                    chatId, messages.messages
                );
                
                allMessages.addAll(unprocessedMessages);
                
                // 检查是否达到最大数量限制
                if (allMessages.size() >= config.getMaxTotalMessages()) {
                    log.warn("达到最大消息数量限制: chatId={}, limit={}", 
                        chatId, config.getMaxTotalMessages());
                    break;
                }
                
                // 更新 fromMessageId 为最旧消息的 ID
                fromMessageId = messages.messages[messages.messages.length - 1].id;
                
                // 如果返回的消息数少于请求数，说明已经到达历史底部
                if (messages.messages.length < config.getMaxMessagesPerFetch()) {
                    break;
                }
                
                retryCount = 0; // 重置重试计数
                
            } catch (UnreadMessageFetchException e) {
                // 检查是否为速率限制或临时错误
                if (isRateLimitError(e)) {
                    metrics.recordRateLimitError();
                    retryCount = handleRateLimitError(retryCount);
                } else if (isTemporaryError(e)) {
                    retryCount = handleTemporaryError(e, retryCount);
                } else {
                    throw e;
                }
            }
        }
        
        // 注意：不需要检查并发新消息
        // 获取期间产生的新消息会被实时监听机制捕获并处理
        // 这些消息会被记录到 processed_messages 集合
        // 因此在过滤时会自动被排除
        
        // 按消息 ID 升序排序
        allMessages.sort(Comparator.comparingLong(m -> m.id));
        
        // 去重（使用 chatId + messageId）
        allMessages = deduplicateMessages(allMessages);
        
        log.info("获取未读消息完成: chatId={}, count={}", chatId, allMessages.size());
        
        return allMessages;
    }
    
    /**
     * 获取频道的最新消息 ID
     * <p>
     * 用于记录开始获取时的位置（日志和调试）
     * 
     * @param chatId 频道 ID
     * @return 最新消息 ID，如果获取失败返回 0
     */
    private long getLatestMessageId(long chatId) {
        try {
            // 应用速率限制
            waitForRateLimit();
            
            // 记录 API 调用
            metrics.recordApiCall();
            
            // 获取最新的一条消息
            TdApi.GetChatHistory request = new TdApi.GetChatHistory(
                chatId,
                0, // fromMessageId = 0 表示从最新消息开始
                0, // offset
                1, // limit = 1，只获取一条
                false // onlyLocal
            );
            
            TdApi.Messages messages = metrics.timeApiCall(() -> sendRequest(request));
            
            if (messages.messages.length > 0) {
                return messages.messages[0].id;
            }
            
        } catch (Exception e) {
            log.warn("获取最新消息 ID 失败: chatId={}, error={}", chatId, e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * 过滤已处理的消息
     * <p>
     * 检查 processed_messages 集合，排除已处理的消息
     * 
     * @param chatId 频道 ID
     * @param messages 消息数组
     * @return 未处理的消息列表
     */
    private List<TdApi.Message> filterUnprocessedMessages(
        long chatId, TdApi.Message[] messages
    ) {
        List<TdApi.Message> unprocessed = new ArrayList<>();
        
        for (TdApi.Message message : messages) {
            if (!processedMessageRepository.existsByChatIdAndMessageId(
                chatId, message.id
            )) {
                unprocessed.add(message);
            }
        }
        
        return unprocessed;
    }
    
    /**
     * 消息去重
     * <p>
     * 使用 chatId:messageId 组合作为唯一标识，移除重复消息
     * 
     * @param messages 消息列表
     * @return 去重后的消息列表
     */
    private List<TdApi.Message> deduplicateMessages(List<TdApi.Message> messages) {
        Map<String, TdApi.Message> uniqueMessages = new LinkedHashMap<>();
        
        for (TdApi.Message message : messages) {
            String key = message.chatId + ":" + message.id;
            uniqueMessages.putIfAbsent(key, message);
        }
        
        return new ArrayList<>(uniqueMessages.values());
    }
    
    /**
     * 应用速率限制
     * <p>
     * 使用 Resilience4j RateLimiter 控制 API 调用频率
     */
    private void waitForRateLimit() {
        try {
            rateLimiter.acquirePermission();
        } catch (Exception e) {
            log.warn("速率限制器异常: {}", e.getMessage());
        }
    }
    
    /**
     * 发送请求到 TDLib
     * <p>
     * 使用 CompletableFuture 异步发送请求并等待响应
     * 
     * @param request GetChatHistory 请求
     * @return Messages 响应
     * @throws RuntimeException 如果请求失败，包装错误信息
     */
    private TdApi.Messages sendRequest(TdApi.GetChatHistory request) {
        try {
            // 使用 client.send() 返回 CompletableFuture
            return telegramClientManager.getClient()
                .send(request)
                .get(30, TimeUnit.SECONDS);
            
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // 将异常包装为 RuntimeException 并重新抛出
            throw new UnreadMessageFetchException("发送请求失败: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnreadMessageFetchException("请求被中断", e);
        } catch (TimeoutException e) {
            throw new UnreadMessageFetchException("请求超时", e);
        }
    }
    
    /**
     * 判断是否为速率限制错误
     * <p>
     * 检查异常消息中是否包含 429 错误码或速率限制关键字
     * 
     * @param exception 异常
     * @return true 如果是速率限制错误
     */
    boolean isRateLimitError(UnreadMessageFetchException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        // 检查是否包含 429 错误码或速率限制关键字
        return message.contains("429") || 
               message.toLowerCase().contains("rate limit") ||
               message.toLowerCase().contains("too many requests");
    }
    
    /**
     * 判断是否为临时错误
     * <p>
     * 检查异常消息中是否包含 5xx 错误码或临时错误关键字
     * 
     * @param exception 异常
     * @return true 如果是临时错误
     */
    boolean isTemporaryError(UnreadMessageFetchException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        // 检查是否包含 5xx 错误码或临时错误关键字
        return message.matches(".*5\\d{2}.*") ||
               message.toLowerCase().contains("timeout") ||
               message.toLowerCase().contains("temporary") ||
               message.toLowerCase().contains("unavailable");
    }
    
    /**
     * 处理速率限制错误
     * <p>
     * 使用指数退避策略重试
     * 
     * @param retryCount 当前重试次数
     * @return 更新后的重试次数
     * @throws UnreadMessageFetchException 如果达到最大重试次数
     */
    int handleRateLimitError(int retryCount) {
        retryCount++;
        
        if (retryCount > config.getMaxRetries()) {
            throw new UnreadMessageFetchException("达到最大重试次数");
        }
        
        // 指数退避：delay = baseDelay * 2^(retryCount - 1)
        long delay = (long) (config.getRetryBaseDelay() * Math.pow(2, retryCount - 1));
        delay = Math.min(delay, config.getRetryMaxDelay());
        
        log.warn("遇到速率限制，等待 {}ms 后重试 (第 {} 次)", delay, retryCount);
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnreadMessageFetchException("重试被中断", e);
        }
        
        return retryCount;
    }
    
    /**
     * 处理临时错误
     * <p>
     * 使用固定延迟重试
     * 
     * @param exception 异常
     * @param retryCount 当前重试次数
     * @return 更新后的重试次数
     * @throws UnreadMessageFetchException 如果达到最大重试次数
     */
    int handleTemporaryError(UnreadMessageFetchException exception, int retryCount) {
        retryCount++;
        
        if (retryCount > config.getMaxRetries()) {
            throw new UnreadMessageFetchException(
                "达到最大重试次数: " + exception.getMessage(), exception
            );
        }
        
        long delay = config.getRetryBaseDelay();
        
        log.warn("遇到临时错误，等待 {}ms 后重试 (第 {} 次): {}", 
            delay, retryCount, exception.getMessage());
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnreadMessageFetchException("重试被中断", e);
        }
        
        return retryCount;
    }
}
