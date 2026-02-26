package org.xlyo.cocomonyab.source.unread;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.source.AbstractMessageSource;
import org.xlyo.cocomonyab.source.MessageSourceHealth;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageStatistics;
import org.xlyo.cocomonyab.source.unread.service.UnreadMessageSourceService;

/**
 * 未读频道消息来源生成器
 * <p>
 * 实现 MessageSource 接口的主组件，负责在程序启动时或按需获取监控频道的未读消息。
 * 该组件确保程序未运行期间产生的消息不会被遗漏。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>启动时可选自动检测未读消息</li>
 *   <li>协调 UnreadMessageSourceService 执行检测流程</li>
 *   <li>提供健康状态监控和统计信息</li>
 *   <li>管理生命周期（启动/停止）</li>
 * </ul>
 *
 * @author CocoMonya Team
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnreadMessageSource extends AbstractMessageSource {
    
    private final UnreadMessageSourceService sourceService;
    private final UnreadMessageSourceConfig config;
    
    @Override
    public String getSourceId() {
        return "unread-channel-message-source";
    }
    
    @Override
    public String getSourceName() {
        return "未读频道消息来源生成器";
    }
    
    @Override
    public String getDescription() {
        return "获取监控频道的未读消息，确保程序未运行期间的消息不被遗漏";
    }
    
    @Override
    protected void doStart() throws Exception {
        log.info("初始化未读消息来源生成器");
        
        // 初始化服务
        sourceService.initialize();
        
        // 如果配置了启动时自动检测，则执行检测
        if (config.getAutoDetectOnStartup()) {
            log.info("启动时自动检测未读消息已启用");
            sourceService.detectUnreadMessages();
        } else {
            log.info("启动时自动检测未读消息已禁用");
        }
    }
    
    @Override
    protected void doStop() throws Exception {
        log.info("停止未读消息来源生成器");
        
        // 关闭服务
        sourceService.shutdown();
    }
    
    @Override
    protected void addCustomHealthMetrics(MessageSourceHealth health) {
        // 获取统计信息
        UnreadMessageStatistics stats = sourceService.getStatistics();
        
        // 添加统计指标
        health.addMetric("total_channels_scanned", stats.getTotalChannelsScanned());
        health.addMetric("total_unread_messages", stats.getTotalUnreadMessages());
        health.addMetric("total_processed_messages", stats.getTotalProcessedMessages());
        health.addMetric("total_failed_messages", stats.getTotalFailedMessages());
        health.addMetric("last_detection_time", stats.getLastDetectionTime());
    }
}
