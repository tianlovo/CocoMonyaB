package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.event.ChannelMonitoringEvent;
import org.xlyo.cocomonyab.filter.AbstractMessageFilter;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 频道监控过滤器
 * 只允许监控列表中的频道消息通过
 * <p>
 * 这是最高优先级的过滤器，如果消息不来自监控频道，
 * 后续所有过滤器和处理逻辑都不会执行
 * <p>
 * 动态更新支持：
 * - 监听 ChannelMonitoringEvent 事件，自动更新缓存
 * - 支持增量更新（添加/删除/更新单个频道）
 * - 支持全量重载（重新加载所有监控频道）
 * - 线程安全：使用 ConcurrentHashMap 和 volatile 保证并发安全
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelMonitoringFilter extends AbstractMessageFilter {
    
    private static final int PRIORITY = 10; // 最高优先级
    
    private final ChannelRepository channelRepository;
    
    /**
     * 监控中的频道ID缓存（提高性能）
     */
    private final Set<Long> monitoringChannels = ConcurrentHashMap.newKeySet();
    
    /**
     * 缓存是否已初始化
     */
    private volatile boolean initialized = false;
    
    @Override
    public String getName() {
        return "ChannelMonitoringFilter";
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
        // 延迟初始化：第一次调用时从数据库加载监控频道列表
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    loadMonitoringChannels();
                    initialized = true;
                }
            }
        }
        
        // 检查是否为监控频道
        if (!monitoringChannels.contains(message.chatId)) {
            context.setRejectReason(String.format(
                "非监控频道: chatId=%d", message.chatId
            ));
            log.debug("过滤非监控频道的消息: chatId={}", message.chatId);
            return FilterResult.REJECT;
        }
        
        // 是监控频道，允许通过
        return FilterResult.ACCEPT;
    }
    
    /**
     * 从数据库加载监控频道列表
     */
    private void loadMonitoringChannels() {
        try {
            List<Channel> channels = channelRepository.findByMonitoringStatus(true);
            
            channels.forEach(channel -> {
                monitoringChannels.add(channel.getChannelId());
                log.info("✓ 已加载监控频道: {} (@{}) [ID: {}]", 
                    channel.getChannelTitle(), 
                    channel.getChannelUsername(),
                    channel.getChannelId());
            });
            
            log.info("=".repeat(60));
            log.info("频道监控过滤器已初始化，共监控 {} 个频道", monitoringChannels.size());
            log.info("=".repeat(60));
            
        } catch (Exception e) {
            log.error("加载监控频道列表失败", e);
        }
    }
    
    /**
     * 检查频道是否在监控列表中
     */
    public boolean isMonitoring(long chatId) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    loadMonitoringChannels();
                    initialized = true;
                }
            }
        }
        return monitoringChannels.contains(chatId);
    }
    
    /**
     * 启动监控
     */
    public void startMonitoring(long chatId) {
        monitoringChannels.add(chatId);
        log.info("✓ 已启动频道监控: chatId={}", chatId);
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring(long chatId) {
        monitoringChannels.remove(chatId);
        log.info("✓ 已停止频道监控: chatId={}", chatId);
    }
    
    /**
     * 重新加载监控列表
     */
    public void reloadMonitoringChannels() {
        monitoringChannels.clear();
        loadMonitoringChannels();
    }
    
    /**
     * 获取监控频道数量
     */
    public int getMonitoringChannelCount() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    loadMonitoringChannels();
                    initialized = true;
                }
            }
        }
        return monitoringChannels.size();
    }
    
    /**
     * 监听频道监控事件，动态更新缓存
     * 
     * @param event 频道监控事件
     */
    @EventListener
    public void handleChannelMonitoringEvent(ChannelMonitoringEvent event) {
        log.info("收到频道监控事件: {}", event);
        
        switch (event.getEventType()) {
            case CHANNEL_ADDED:
                handleChannelAdded(event.getChannelId(), event.getMonitoringStatus());
                break;
                
            case CHANNEL_REMOVED:
                handleChannelRemoved(event.getChannelId());
                break;
                
            case CHANNEL_UPDATED:
                handleChannelUpdated(event.getChannelId(), event.getMonitoringStatus());
                break;
                
            case RELOAD_ALL:
                handleReloadAll();
                break;
                
            default:
                log.warn("未知的事件类型: {}", event.getEventType());
        }
    }
    
    /**
     * 处理频道添加事件
     */
    private void handleChannelAdded(Long channelId, Boolean monitoringStatus) {
        if (channelId == null) {
            log.warn("频道ID为空，忽略添加事件");
            return;
        }
        
        // 如果监控状态为 true，添加到监控列表
        if (Boolean.TRUE.equals(monitoringStatus)) {
            monitoringChannels.add(channelId);
            log.info("✓ 频道已添加到监控列表: chatId={}", channelId);
        } else {
            log.debug("频道监控状态为 false，不添加到监控列表: chatId={}", channelId);
        }
    }
    
    /**
     * 处理频道移除事件
     */
    private void handleChannelRemoved(Long channelId) {
        if (channelId == null) {
            log.warn("频道ID为空，忽略移除事件");
            return;
        }
        
        boolean removed = monitoringChannels.remove(channelId);
        if (removed) {
            log.info("✓ 频道已从监控列表移除: chatId={}", channelId);
        } else {
            log.debug("频道不在监控列表中: chatId={}", channelId);
        }
    }
    
    /**
     * 处理频道更新事件
     */
    private void handleChannelUpdated(Long channelId, Boolean monitoringStatus) {
        if (channelId == null) {
            log.warn("频道ID为空，忽略更新事件");
            return;
        }
        
        if (Boolean.TRUE.equals(monitoringStatus)) {
            // 监控状态为 true，确保在监控列表中
            boolean added = monitoringChannels.add(channelId);
            if (added) {
                log.info("✓ 频道已启用监控: chatId={}", channelId);
            } else {
                log.debug("频道已在监控列表中: chatId={}", channelId);
            }
        } else {
            // 监控状态为 false，从监控列表移除
            boolean removed = monitoringChannels.remove(channelId);
            if (removed) {
                log.info("✓ 频道已停用监控: chatId={}", channelId);
            } else {
                log.debug("频道不在监控列表中: chatId={}", channelId);
            }
        }
    }
    
    /**
     * 处理重新加载所有频道事件
     */
    private void handleReloadAll() {
        log.info("开始重新加载所有监控频道...");
        monitoringChannels.clear();
        loadMonitoringChannels();
        log.info("✓ 监控频道列表已重新加载，当前监控 {} 个频道", monitoringChannels.size());
    }
}
