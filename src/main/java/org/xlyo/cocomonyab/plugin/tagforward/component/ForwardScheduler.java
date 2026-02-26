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
import java.util.ArrayList;
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
    
    /**
     * TDLib API 单次转发的最大消息数量限制
     */
    private static final int MAX_FORWARD_MESSAGES_PER_REQUEST = 100;
    
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
     * 从队列中获取待处理消息，批量转发以提高效率
     * <p>
     * 注意：确保媒体组的原子性，不会因为TDLib的100条消息限制而截断媒体组
     */
    private void processQueue() {
        try {
            List<ForwardQueueItem> items = queueManager.getPendingItems(properties.getBatchSize());
            
            if (items.isEmpty()) {
                log.trace("转发队列中无待处理项");
                return;
            }
            
            log.debug("正在处理转发队列中的 {} 个待处理项", items.size());
            
            // 批量转发：将多个队列项合并到一个请求中，但要确保不超过100条消息限制
            // 并且不会截断媒体组
            List<ForwardQueueItem> batch = new ArrayList<>();
            int totalMessageCount = 0;
            boolean rateLimitReached = false;
            
            for (ForwardQueueItem item : items) {
                // 如果已经达到频率限制，停止处理，让剩余消息等待下一次调度
                if (rateLimitReached) {
                    log.debug("已达到频率限制，剩余 {} 个队列项将在下一次调度周期处理", 
                            items.size() - items.indexOf(item));
                    break;
                }
                
                // 计算当前项的消息数量
                int itemMessageCount = getMessageCount(item);
                
                // 检查是否会超过100条消息限制
                if (totalMessageCount + itemMessageCount > MAX_FORWARD_MESSAGES_PER_REQUEST) {
                    // 如果加上当前项会超过限制，先转发已收集的批次
                    if (!batch.isEmpty()) {
                        boolean success = forwardBatch(batch);
                        if (!success) {
                            // 达到频率限制，停止处理
                            rateLimitReached = true;
                            continue;
                        }
                        batch.clear();
                        totalMessageCount = 0;
                    }
                    
                    // 如果当前项本身就超过限制（理论上不会发生，因为媒体组最大10条）
                    if (itemMessageCount > MAX_FORWARD_MESSAGES_PER_REQUEST) {
                        log.error("队列项消息数量 ({}) 超过TDLib限制 ({}): chatId={}, messageId={}", 
                                itemMessageCount, MAX_FORWARD_MESSAGES_PER_REQUEST,
                                item.getSourceChatId(), item.getSourceMessageId());
                        queueManager.updateStatus(item.getId(), ForwardStatus.FAILED, 
                                "消息数量超过TDLib限制");
                        continue;
                    }
                }
                
                // 将当前项加入批次
                batch.add(item);
                totalMessageCount += itemMessageCount;
            }
            
            // 转发剩余的批次（如果还没达到频率限制）
            if (!batch.isEmpty() && !rateLimitReached) {
                forwardBatch(batch);
            }
            
        } catch (Exception e) {
            log.error("处理转发队列时出错", e);
        }
    }
    
    /**
     * 获取队列项的消息数量
     * 
     * @param item 队列项
     * @return 消息数量
     */
    private int getMessageCount(ForwardQueueItem item) {
        if (item.getMediaGroupMessageIds() != null && !item.getMediaGroupMessageIds().isEmpty()) {
            return item.getMediaGroupMessageIds().size();
        }
        return 1;
    }
    
    /**
     * 批量转发一组队列项
     * <p>
     * 注意：所有队列项必须来自同一个源频道
     * 
     * @param batch 待转发的队列项列表
     * @return true 如果成功获取频率限制许可并发起转发，false 如果达到频率限制
     */
    private boolean forwardBatch(List<ForwardQueueItem> batch) {
        if (batch.isEmpty()) {
            return true;
        }
        
        // 检查频率限制（限制调用TG API的频率）
        if (!rateLimiter.acquirePermission()) {
            log.debug("已达到频率限制，本批次 {} 个队列项将在下一次调度周期处理", batch.size());
            return false;
        }
        
        // 检查所有项是否来自同一个源频道
        Long sourceChatId = batch.getFirst().getSourceChatId();
        boolean sameSource = batch.stream()
                .allMatch(item -> item.getSourceChatId().equals(sourceChatId));
        
        if (!sameSource) {
            // 如果来自不同源频道，逐个转发
            log.debug("批次中的消息来自不同源频道，逐个转发");
            boolean allSuccess = true;
            for (ForwardQueueItem item : batch) {
                if (!forwardMessage(item)) {
                    // 达到频率限制，停止处理
                    allSuccess = false;
                    break;
                }
            }
            return allSuccess;
        }
        
        // 合并所有消息ID
        List<Long> allMessageIds = new ArrayList<>();
        for (ForwardQueueItem item : batch) {
            if (item.getMediaGroupMessageIds() != null && !item.getMediaGroupMessageIds().isEmpty()) {
                allMessageIds.addAll(item.getMediaGroupMessageIds());
            } else {
                allMessageIds.add(item.getSourceMessageId());
            }
        }
        
        // 确保消息ID按递增顺序排序
        allMessageIds.sort(Long::compareTo);
        
        log.debug("批量转发 {} 个队列项，共 {} 条消息: sourceChatId={}", 
                batch.size(), allMessageIds.size(), sourceChatId);
        
        // 执行批量转发
        forwardMessages(sourceChatId, allMessageIds, batch);
        return true;
    }
    
    /**
     * 批量转发消息
     * 
     * @param sourceChatId 源频道ID
     * @param messageIds 消息ID列表（已排序）
     * @param items 对应的队列项列表
     */
    private void forwardMessages(Long sourceChatId, List<Long> messageIds, List<ForwardQueueItem> items) {
        try {
            // 检查客户端是否就绪
            if (!clientManager.isReady()) {
                log.warn("Telegram 客户端未就绪，跳过批次转发");
                return;
            }
            
            SimpleTelegramClient client = clientManager.getClient();
            
            // 构造转发请求
            TdApi.ForwardMessages request = new TdApi.ForwardMessages();
            request.chatId = properties.getTargetChannelId();
            request.fromChatId = sourceChatId;
            request.messageIds = messageIds.stream().mapToLong(Long::longValue).toArray();
            request.sendCopy = false;
            request.removeCaption = false;
            
            // 发送异步请求
            client.send(request).whenCompleteAsync((result, error) -> {
                if (error != null) {
                    // 批量转发失败，逐个重试
                    log.warn("批量转发失败，将逐个重试: sourceChatId={}, messageCount={}, error={}", 
                            sourceChatId, messageIds.size(), error.getMessage());
                    for (ForwardQueueItem item : items) {
                        handleForwardError(item, error);
                    }
                    return;
                }
                
                // 批量转发成功，更新所有队列项状态
                log.info("批量转发成功: sourceChatId={}, itemCount={}, messageCount={}", 
                        sourceChatId, items.size(), messageIds.size());
                for (ForwardQueueItem item : items) {
                    handleForwardSuccess(item);
                }
            });
            
        } catch (Exception e) {
            log.error("批量转发消息时出错: sourceChatId={}, messageCount={}", 
                    sourceChatId, messageIds.size(), e);
            for (ForwardQueueItem item : items) {
                handleForwardError(item, e);
            }
        }
    }
    
    /**
     * 转发单条消息或媒体组
     * 
     * @param item 待转发的队列项
     * @return true 如果成功获取频率限制许可并发起转发，false 如果达到频率限制
     */
    private boolean forwardMessage(ForwardQueueItem item) {
        try {
            // 检查频率限制（限制调用TG API的频率）
            if (!rateLimiter.acquirePermission()) {
                log.debug("已达到频率限制，消息将在下一次调度周期处理: chatId={}, messageId={}", 
                        item.getSourceChatId(), item.getSourceMessageId());
                return false;
            }
            
            // 检查客户端是否就绪
            if (!clientManager.isReady()) {
                log.warn("Telegram 客户端未就绪，跳过消息: chatId={}, messageId={}", 
                        item.getSourceChatId(), item.getSourceMessageId());
                return true;  // 返回 true 继续处理下一条消息
            }
            
            SimpleTelegramClient client = clientManager.getClient();
            
            // 确定要转发的消息ID列表
            long[] messageIds;
            if (item.getMediaGroupMessageIds() != null && !item.getMediaGroupMessageIds().isEmpty()) {
                // 媒体组：使用所有消息ID（已按递增顺序排序）
                int mediaGroupSize = item.getMediaGroupMessageIds().size();
                
                // 检查媒体组大小是否超过TDLib限制
                if (mediaGroupSize > MAX_FORWARD_MESSAGES_PER_REQUEST) {
                    String errorMsg = String.format(
                            "媒体组消息数量 (%d) 超过TDLib单次转发限制 (%d)，无法转发",
                            mediaGroupSize, MAX_FORWARD_MESSAGES_PER_REQUEST);
                    log.error("[ForwardScheduler] {}: chatId={}, messageIds={}", 
                            errorMsg, item.getSourceChatId(), item.getMediaGroupMessageIds());
                    
                    // 标记为失败，不再重试
                    queueManager.updateStatus(item.getId(), ForwardStatus.FAILED, errorMsg);
                    return true;  // 返回 true 继续处理下一条消息
                }
                
                messageIds = item.getMediaGroupMessageIds().stream()
                        .mapToLong(Long::longValue)
                        .toArray();
                log.debug("准备转发媒体组: chatId={}, messageIds={}, count={}", 
                        item.getSourceChatId(), item.getMediaGroupMessageIds(), messageIds.length);
            } else {
                // 普通消息：只有一个消息ID
                messageIds = new long[]{item.getSourceMessageId()};
                log.debug("准备转发单条消息: chatId={}, messageId={}", 
                        item.getSourceChatId(), item.getSourceMessageId());
            }
            
            // 构造转发请求
            TdApi.ForwardMessages request = new TdApi.ForwardMessages();
            request.chatId = properties.getTargetChannelId();
            request.fromChatId = item.getSourceChatId();
            request.messageIds = messageIds;
            request.sendCopy = false;  // 保留原始转发信息
            request.removeCaption = false;  // 保留原始说明文字
            
            // 发送异步请求（使用whenCompleteAsync避免阻塞TDLib响应线程）
            client.send(request).whenCompleteAsync((result, error) -> {
                if (error != null) {
                    // 处理错误
                    handleForwardError(item, error);
                    return;
                }
                
                // 成功：result是TdApi.Messages类型
                handleForwardSuccess(item);
            });
            
            return true;  // 成功发起转发请求
            
        } catch (Exception e) {
            log.error("转发消息时出错: chatId={}, messageId={}", 
                    item.getSourceChatId(), item.getSourceMessageId(), e);
            handleForwardError(item, e);
            return true;  // 返回 true 继续处理下一条消息
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
