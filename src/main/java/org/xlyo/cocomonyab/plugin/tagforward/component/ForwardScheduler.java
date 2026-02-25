package org.xlyo.cocomonyab.plugin.tagforward.component;

import com.google.common.util.concurrent.RateLimiter;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 转发调度器
 * 
 * 负责定时处理转发队列，使用频率限制器控制转发速率，
 * 并通过TelegramClient执行实际的消息转发操作
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForwardScheduler {
    
    private final QueueManager queueManager;
    private final TelegramClientManager clientManager;
    private final TagBasedForwardingProperties properties;
    
    private RateLimiter rateLimiter;
    private ScheduledExecutorService scheduler;
    
    /**
     * 初始化频率限制器
     * 
     * 根据配置的每分钟转发速率创建RateLimiter实例
     */
    @PostConstruct
    public void initialize() {
        // 将每分钟的速率转换为每秒的速率
        double permitsPerSecond = properties.getRateLimitPerMinute() / 60.0;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        log.info("Initialized rate limiter: {} permits per minute", properties.getRateLimitPerMinute());
    }
    
    /**
     * 启动转发调度器
     * 
     * 创建定时任务，按配置的间隔定期处理转发队列
     */
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            log.warn("Forward scheduler is already running");
            return;
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "forward-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        
        scheduler.scheduleAtFixedRate(
                this::processQueue,
                0,
                properties.getScheduleIntervalSeconds(),
                TimeUnit.SECONDS
        );
        
        log.info("Forward scheduler started with interval: {} seconds", 
                properties.getScheduleIntervalSeconds());
    }
    
    /**
     * 停止转发调度器
     * 
     * 优雅地关闭调度器，等待当前任务完成
     */
    public void stop() {
        if (scheduler == null || scheduler.isShutdown()) {
            log.debug("Forward scheduler is not running");
            return;
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Forward scheduler did not terminate gracefully, forcing shutdown");
                scheduler.shutdownNow();
            }
            log.info("Forward scheduler stopped");
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for scheduler to terminate", e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 处理转发队列
     * 
     * 从队列中获取待处理消息，应用频率限制，并执行转发操作
     */
    private void processQueue() {
        try {
            List<ForwardQueueItem> items = queueManager.getPendingItems(properties.getBatchSize());
            
            if (items.isEmpty()) {
                log.trace("No pending items in forward queue");
                return;
            }
            
            log.debug("Processing {} pending items from forward queue", items.size());
            
            for (ForwardQueueItem item : items) {
                // 检查频率限制
                if (!rateLimiter.tryAcquire()) {
                    log.debug("Rate limit reached, skipping remaining messages in this batch");
                    break;
                }
                
                forwardMessage(item);
            }
        } catch (Exception e) {
            log.error("Error processing forward queue", e);
        }
    }
    
    /**
     * 转发单条消息
     * 
     * @param item 待转发的队列项
     */
    private void forwardMessage(ForwardQueueItem item) {
        try {
            // 检查客户端是否就绪
            if (!clientManager.isReady()) {
                log.warn("Telegram client is not ready, skipping message: chatId={}, messageId={}", 
                        item.getSourceChatId(), item.getSourceMessageId());
                return;
            }
            
            SimpleTelegramClient client = clientManager.getClient();
            
            // 构造转发请求
            TdApi.ForwardMessages request = new TdApi.ForwardMessages();
            request.chatId = properties.getTargetChannelId();
            request.fromChatId = item.getSourceChatId();
            request.messageIds = new long[]{item.getSourceMessageId()};
            request.sendCopy = false;  // 保留原始转发信息
            request.removeCaption = false;  // 保留原始说明文字
            
            // 发送异步请求
            client.send(request).whenComplete((result, error) -> {
                if (error != null) {
                    handleForwardError(item, error);
                } else if (result instanceof TdApi.Messages) {
                    handleForwardSuccess(item);
                } else {
                    // 处理意外的结果类型（包括TdApi.Error）
                    String errorMsg = "Unexpected result type: " + 
                            (result != null ? result.getClass().getName() : "null");
                    log.warn("Forward request returned unexpected result: chatId={}, messageId={}, result={}", 
                            item.getSourceChatId(), item.getSourceMessageId(), errorMsg);
                    handleForwardError(item, new RuntimeException(errorMsg));
                }
            });
            
        } catch (Exception e) {
            log.error("Error forwarding message: chatId={}, messageId={}", 
                    item.getSourceChatId(), item.getSourceMessageId(), e);
            handleForwardError(item, e);
        }
    }
    
    /**
     * 处理转发成功
     * 
     * @param item 成功转发的队列项
     */
    private void handleForwardSuccess(ForwardQueueItem item) {
        try {
            queueManager.updateStatus(item.getId(), ForwardStatus.SUCCESS, null);
            log.info("Message forwarded successfully: chatId={}, messageId={}, tags={}", 
                    item.getSourceChatId(), item.getSourceMessageId(), item.getMatchedTags());
        } catch (Exception e) {
            log.error("Error updating status after successful forward: itemId={}", item.getId(), e);
        }
    }
    
    /**
     * 处理转发错误
     * 
     * @param item 转发失败的队列项
     * @param error 错误信息
     */
    private void handleForwardError(ForwardQueueItem item, Throwable error) {
        try {
            // 递增重试计数
            queueManager.incrementRetryCount(item.getId());
            
            int newRetryCount = item.getRetryCount() + 1;
            
            // 检查是否达到最大重试次数
            if (newRetryCount >= properties.getMaxRetryCount()) {
                queueManager.updateStatus(item.getId(), ForwardStatus.FAILED, error.getMessage());
                log.error("Message forward failed after {} retries: chatId={}, messageId={}, error={}", 
                        properties.getMaxRetryCount(), 
                        item.getSourceChatId(), 
                        item.getSourceMessageId(), 
                        error.getMessage());
            } else {
                log.warn("Message forward failed, will retry (attempt {}/{}): chatId={}, messageId={}, error={}", 
                        newRetryCount, 
                        properties.getMaxRetryCount(),
                        item.getSourceChatId(), 
                        item.getSourceMessageId(), 
                        error.getMessage());
            }
        } catch (Exception e) {
            log.error("Error handling forward error for itemId={}", item.getId(), e);
        }
    }
}
