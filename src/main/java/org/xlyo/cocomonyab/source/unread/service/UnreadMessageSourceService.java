package org.xlyo.cocomonyab.source.unread.service;

import it.tdlight.jni.TdApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.metrics.UnreadMessageMetrics;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageDetectionResult;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageStatistics;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 未读消息来源服务
 * <p>
 * 核心服务，协调整个未读消息检测流程。负责：
 * <ul>
 *   <li>初始化服务并处理待处理的缓冲消息</li>
 *   <li>检测未读消息（启动时自动或手动触发）</li>
 *   <li>并发控制，防止多个检测任务同时运行</li>
 *   <li>遍历所有监控频道并获取未读消息</li>
 *   <li>错误隔离，单个频道的错误不影响其他频道</li>
 *   <li>统计信息收集和报告</li>
 * </ul>
 *
 * @author tianluoqaq
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadMessageSourceService {
    
    private final ChannelRepository channelRepository;
    private final UnreadMessageFetchService fetchService;
    private final UnreadMessageSourceBufferService bufferService;
    private final UnreadMessageSourceConfig config;
    private final UnreadMessageMetrics metrics;
    
    /**
     * 并发检测标志，防止多个检测任务同时运行
     */
    private final AtomicBoolean isDetecting = new AtomicBoolean(false);

    @Getter
    private final UnreadMessageStatistics statistics = new UnreadMessageStatistics();
    
    /**
     * 初始化服务
     * <p>
     * 检查缓冲区中是否有未处理的消息，如果有则继续处理
     */
    public void initialize() {
        log.info("初始化未读消息来源生成器");
        
        // 检查缓冲区中是否有未处理的消息
        long pendingCount = bufferService.countPendingMessages();
        if (pendingCount > 0) {
            log.info("发现 {} 条待处理的缓冲消息", pendingCount);
            bufferService.processPendingMessages();
        }
    }
    
    /**
     * 检测未读消息（支持手动触发）
     * <p>
     * 使用并发保护机制，防止多个检测任务同时运行。
     * 记录开始和结束时间，返回检测结果统计信息。
     * 
     * @return 检测结果统计信息
     * @throws IllegalStateException 如果已有检测任务正在运行
     */
    public UnreadMessageDetectionResult detectUnreadMessages() {
        // 防止并发执行
        if (!isDetecting.compareAndSet(false, true)) {
            log.warn("⚠️ [未读消息检测] 检测任务已在运行中，拒绝新的检测请求");
            throw new IllegalStateException("未读消息检测正在进行中");
        }
        
        log.info("🚀 [未读消息检测] 开始检测未读消息");
        
        try {
            return metrics.timeDetection(this::doDetectUnreadMessages);
        } finally {
            isDetecting.set(false);
            log.info("🏁 [未读消息检测] 检测任务结束");
        }
    }
    
    /**
     * 执行未读消息检测的核心逻辑
     * <p>
     * 获取所有启用监控的频道，遍历每个频道调用 processChannel。
     * 单个频道的错误不影响其他频道的处理。
     * 
     * @return 检测结果统计信息
     */
    private UnreadMessageDetectionResult doDetectUnreadMessages() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("📊 [未读消息检测] 开始检测，时间: {}", startTime);
        
        // 获取所有启用监控的频道
        List<Channel> monitoringChannels = channelRepository
            .findByMonitoringStatus(true);
        
        log.info("📡 [未读消息检测] 找到监控频道: 数量={}", monitoringChannels.size());
        
        UnreadMessageDetectionResult result = new UnreadMessageDetectionResult();
        result.setStartTime(startTime);
        result.setTotalChannels(monitoringChannels.size());
        
        // 对每个频道获取未读消息
        for (Channel channel : monitoringChannels) {
            try {
                log.info("🔍 [未读消息检测] 处理频道: chatId={}, title={}", 
                    channel.getChannelId(), channel.getChannelTitle());
                processChannel(channel, result);
            } catch (Exception e) {
                // 单个频道的错误不影响其他频道
                log.error("❌ [未读消息检测] 处理频道失败: chatId={}, title={}, error={}", 
                    channel.getChannelId(), channel.getChannelTitle(), e.getMessage(), e);
                result.incrementFailedChannels();
            }
        }
        
        LocalDateTime endTime = LocalDateTime.now();
        result.setEndTime(endTime);
        
        // 更新统计信息
        updateStatistics(result);
        
        log.info("✅ [未读消息检测] 检测完成: 总频道={}, 成功={}, 失败={}, 未读消息={}, 耗时={}ms",
            result.getTotalChannels(),
            result.getSuccessChannels(),
            result.getFailedChannels(),
            result.getTotalUnreadMessages(),
            Duration.between(startTime, endTime).toMillis());
        
        return result;
    }
    
    /**
     * 处理单个频道
     * <p>
     * 获取频道的未读消息，保存到缓冲区并处理
     * 
     * @param channel 频道实体
     * @param result 检测结果（用于更新统计）
     */
    private void processChannel(Channel channel, UnreadMessageDetectionResult result) {
        log.debug("处理频道: channelId={}, title={}", 
            channel.getChannelId(), channel.getChannelTitle());
        
        // 记录扫描的频道
        metrics.recordChannelScanned();
        
        // 获取未读消息
        List<TdApi.Message> unreadMessages = fetchService.fetchUnreadMessages(
            channel.getChannelId()
        );
        
        if (unreadMessages.isEmpty()) {
            log.debug("频道无未读消息: channelId={}", channel.getChannelId());
            result.incrementSuccessChannels();
            return;
        }
        
        log.info("频道发现 {} 条未读消息: channelId={}", 
            unreadMessages.size(), channel.getChannelId());
        
        // 记录检测到的消息数
        metrics.recordMessagesDetected(unreadMessages.size());
        
        // 保存到缓冲区并处理
        bufferService.bufferAndProcessMessages(
            channel.getChannelId(),
            unreadMessages,
            channel.getChannelUsername(),
            channel.getChannelTitle()
        );
        
        result.incrementSuccessChannels();
        result.addUnreadMessages(unreadMessages.size());
    }
    
    /**
     * 更新统计信息
     * <p>
     * 将检测结果记录到统计信息中
     * 
     * @param result 检测结果
     */
    private void updateStatistics(UnreadMessageDetectionResult result) {
        statistics.recordDetection(result);
    }

    /**
     * 关闭服务
     * <p>
     * 清理资源
     */
    public void shutdown() {
        log.info("关闭未读消息来源生成器");
        // 清理资源（如果需要）
    }
}
