package org.xlyo.cocomonyab.plugin.tagforward.component;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 转发调度器
 * <p>
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
     * <p>
     * 根据配置的每分钟转发速率创建RateLimiter实例
     */
    @PostConstruct
    public void initialize() {
        // 配置频率限制器：每分钟允许的请求数
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.getRateLimitPerMinute())
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO) // 不等待，立即返回
                .build();
        
        this.rateLimiter = RateLimiter.of("forward-rate-limiter", config);
        log.info("频率限制器初始化完成: 每分钟 {} 次", properties.getRateLimitPerMinute());
    }
    
    /**
     * 启动转发调度器
     * <p>
     * 创建定时任务，按配置的间隔定期处理转发队列
     */
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            log.warn("转发调度器已在运行中");
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
        
        log.info("转发调度器已启动，间隔: {} 秒", 
                properties.getScheduleIntervalSeconds());
    }
    
    /**
     * 停止转发调度器
     * <p>
     * 优雅地关闭调度器，等待当前任务完成
     */
    public void stop() {
        if (scheduler == null || scheduler.isShutdown()) {
            log.debug("转发调度器未在运行");
            return;
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("转发调度器未能优雅关闭，强制停止");
                scheduler.shutdownNow();
            }
            log.info("转发调度器已停止");
        } catch (InterruptedException e) {
            log.error("等待调度器终止时被中断", e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 处理转发队列
     * <p>
     * 从队列中获取待处理消息，应用频率限制，并执行转发操作
     */
    private void processQueue() {
        try {
            List<ForwardQueueItem> items = queueManager.getPendingItems(properties.getBatchSize());
            
            if (items.isEmpty()) {
                log.trace("转发队列中无待处理项");
                return;
            }
            
            log.debug("正在处理转发队列中的 {} 个待处理项", items.size());
            
            for (ForwardQueueItem item : items) {
                // 检查频率限制
                if (!rateLimiter.acquirePermission()) {
                    log.debug("已达到频率限制，跳过本批次剩余消息");
                    break;
                }
                
                forwardMessage(item);
            }
        } catch (Exception e) {
            log.error("处理转发队列时出错", e);
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
                log.warn("Telegram 客户端未就绪，跳过消息: chatId={}, messageId={}", 
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
                } else if (result != null) {
                    handleForwardSuccess(item);
                } else {
                    // 处理意外的结果类型（包括TdApi.Error）
                    String errorMsg = "意外的结果类型: null";
                    log.warn("转发请求返回意外结果: chatId={}, messageId={}, result={}", 
                            item.getSourceChatId(), item.getSourceMessageId(), errorMsg);
                    handleForwardError(item, new RuntimeException(errorMsg));
                }
            });
            
        } catch (Exception e) {
            log.error("转发消息时出错: chatId={}, messageId={}", 
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
            log.info("消息转发成功: chatId={}, messageId={}, tags={}", 
                    item.getSourceChatId(), item.getSourceMessageId(), item.getMatchedTags());
        } catch (Exception e) {
            log.error("转发成功后更新状态时出错: itemId={}", item.getId(), e);
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
                log.error("消息转发失败，已重试 {} 次: chatId={}, messageId={}, error={}", 
                        properties.getMaxRetryCount(), 
                        item.getSourceChatId(), 
                        item.getSourceMessageId(), 
                        error.getMessage());
            } else {
                log.warn("消息转发失败，将重试（第 {}/{} 次尝试）: chatId={}, messageId={}, error={}", 
                        newRetryCount, 
                        properties.getMaxRetryCount(),
                        item.getSourceChatId(), 
                        item.getSourceMessageId(), 
                        error.getMessage());
            }
        } catch (Exception e) {
            log.error("处理转发错误时出错: itemId={}", item.getId(), e);
        }
    }
}
