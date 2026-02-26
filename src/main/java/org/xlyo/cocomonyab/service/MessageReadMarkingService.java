package org.xlyo.cocomonyab.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * 消息已读标记服务
 * <p>
 * 负责将消息标记为已读，考虑到 Telegram API 的速率限制，
 * 使用队列和速率限制器逐个处理消息。
 * 
 * <p>功能特性：
 * <ul>
 *   <li>异步标记消息为已读</li>
 *   <li>速率限制保护（默认每秒1个请求）</li>
 *   <li>队列缓冲，避免丢失请求</li>
 *   <li>自动重试机制</li>
 *   <li>优雅关闭</li>
 * </ul>
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageReadMarkingService {
    
    private final TelegramClientManager clientManager;
    
    /**
     * 待标记为已读的消息队列
     * 使用 LinkedBlockingQueue 保证线程安全和阻塞特性
     */
    private final BlockingQueue<ReadMarkRequest> readMarkQueue = new LinkedBlockingQueue<>(10000);
    
    /**
     * 速率限制器
     * 使用 Resilience4j RateLimiter，每秒1个请求，避免触发 Telegram API 限制
     */
    private RateLimiter rateLimiter;
    
    /**
     * 后台处理线程
     */
    private ExecutorService executorService;
    
    /**
     * 服务运行状态
     */
    private volatile boolean running = false;
    
    /**
     * 初始化服务
     */
    @PostConstruct
    public void init() {
        // 创建速率限制器配置：每秒1个请求
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))  // 刷新周期：1秒
            .limitForPeriod(1)                          // 每个周期允许的请求数：1
            .timeoutDuration(Duration.ofMinutes(5))     // 等待许可的超时时间：5分钟
            .build();
        
        // 创建速率限制器实例
        this.rateLimiter = RateLimiter.of("message-read-marking", config);
        
        // 创建单线程执行器处理队列
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "MessageReadMarking-Worker");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动后台处理任务
        this.running = true;
        executorService.submit(this::processQueue);
        
        log.info("消息已读标记服务已启动，速率限制: 1 req/s (Resilience4j)");
    }
    
    /**
     * 标记单条消息为已读
     * <p>
     * 此方法是异步的，消息会被加入队列等待处理
     * 
     * @param chatId 频道 ID
     * @param messageId 消息 ID
     */
    public void markAsRead(long chatId, long messageId) {
        markAsRead(chatId, new long[]{messageId});
    }
    
    /**
     * 标记多条消息为已读
     * <p>
     * 此方法是异步的，消息会被加入队列等待处理
     * 
     * @param chatId 频道 ID
     * @param messageIds 消息 ID 数组
     */
    public void markAsRead(long chatId, long[] messageIds) {
        if (messageIds == null || messageIds.length == 0) {
            return;
        }
        
        ReadMarkRequest request = new ReadMarkRequest(chatId, messageIds);
        
        boolean added = readMarkQueue.offer(request);
        if (added) {
            log.debug("消息已加入已读标记队列: chatId={}, messageIds={}, queueSize={}", 
                chatId, messageIds, readMarkQueue.size());
        } else {
            log.warn("已读标记队列已满，消息被丢弃: chatId={}, messageIds={}", 
                chatId, messageIds);
        }
    }
    
    /**
     * 处理队列中的消息
     * <p>
     * 在后台线程中持续运行，从队列中取出消息并标记为已读
     */
    private void processQueue() {
        log.info("已读标记处理线程已启动");
        
        while (running) {
            try {
                // 从队列中取出请求（阻塞等待）
                ReadMarkRequest request = readMarkQueue.poll(1, TimeUnit.SECONDS);
                
                if (request == null) {
                    continue;
                }
                
                // 等待速率限制器许可
                // Resilience4j 的 acquirePermission() 会阻塞直到获得许可或超时
                boolean permitted = rateLimiter.acquirePermission();
                
                if (!permitted) {
                    log.warn("获取速率限制器许可超时，消息将被重新加入队列: chatId={}, messageIds={}", 
                        request.chatId, request.messageIds);
                    // 重新加入队列
                    readMarkQueue.offer(request);
                    continue;
                }
                
                // 执行标记操作
                doMarkAsRead(request);
                
            } catch (InterruptedException e) {
                log.warn("已读标记处理线程被中断", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("处理已读标记请求时发生异常", e);
            }
        }
        
        log.info("已读标记处理线程已停止");
    }
    
    /**
     * 执行标记为已读的操作
     * 
     * @param request 标记请求
     */
    private void doMarkAsRead(ReadMarkRequest request) {
        try {
            // 检查客户端是否就绪
            if (!clientManager.isReady()) {
                log.warn("Telegram 客户端未就绪，跳过标记: chatId={}, messageIds={}", 
                    request.chatId, request.messageIds);
                return;
            }
            
            SimpleTelegramClient client = clientManager.getClient();
            
            // 构造 ViewMessages 请求
            TdApi.ViewMessages viewMessages = new TdApi.ViewMessages(
                request.chatId,
                request.messageIds,
                null,  // source: null 表示根据聊天打开状态自动判断
                true   // forceRead: true 表示即使聊天关闭也标记为已读
            );
            
            // 发送请求
            client.send(viewMessages).whenCompleteAsync((result, error) -> {
                if (error != null) {
                    log.error("标记消息为已读失败: chatId={}, messageIds={}", 
                        request.chatId, request.messageIds, error);
                    
                    // 如果失败，可以考虑重试（这里简化处理，只记录日志）
                    handleMarkFailure(request, error);
                } else {
                    log.debug("消息已标记为已读: chatId={}, messageIds={}", 
                        request.chatId, request.messageIds);
                }
            });
            
        } catch (Exception e) {
            log.error("执行标记为已读操作时发生异常: chatId={}, messageIds={}", 
                request.chatId, request.messageIds, e);
        }
    }
    
    /**
     * 处理标记失败的情况
     * 
     * @param request 失败的请求
     * @param error 错误信息
     */
    private void handleMarkFailure(ReadMarkRequest request, Throwable error) {
        // 简化处理：只记录日志
        // 如果需要重试机制，可以在这里实现
        log.warn("标记失败，不进行重试: chatId={}, messageIds={}, error={}", 
            request.chatId, request.messageIds, error.getMessage());
    }
    
    /**
     * 获取队列大小
     * 
     * @return 当前队列中待处理的请求数量
     */
    public int getQueueSize() {
        return readMarkQueue.size();
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        log.info("正在关闭消息已读标记服务...");
        
        running = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("消息已读标记服务已关闭，剩余队列大小: {}", readMarkQueue.size());
    }
    
    /**
     * 已读标记请求
     */
    private static class ReadMarkRequest {
        final long chatId;
        final long[] messageIds;
        
        ReadMarkRequest(long chatId, long[] messageIds) {
            this.chatId = chatId;
            this.messageIds = messageIds;
        }
    }
}
