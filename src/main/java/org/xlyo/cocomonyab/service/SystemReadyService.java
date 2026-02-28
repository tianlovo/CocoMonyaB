package org.xlyo.cocomonyab.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.event.SystemReadyEvent;
import org.xlyo.cocomonyab.event.startup.*;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 系统就绪状态管理服务
 * <p>
 * 负责跟踪系统启动状态，确保所有关键组件初始化完成后才允许API接受请求
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemReadyService {
    
    private final ApplicationEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 系统是否完全就绪
     */
    @Getter
    private final AtomicBoolean systemReady = new AtomicBoolean(false);
    
    /**
     * 系统启动开始时间
     */
    private long startupStartTime;
    
    /**
     * 未就绪原因
     */
    @Getter
    private volatile String notReadyReason = "系统正在启动中...";
    
    /**
     * 当前启动状态
     */
    @Getter
    private volatile StartupStatus currentStatus = StartupStatus.NOT_STARTED;
    
    /**
     * 当前阶段名称
     */
    @Getter
    private volatile String currentPhase = "未启动";
    
    /**
     * 监听配置就绪事件
     */
    @EventListener(ConfigurationReadyEvent.class)
    public void onConfigurationReady(ConfigurationReadyEvent event) {
        currentStatus = StartupStatus.DATABASE_INIT;
        currentPhase = "数据库初始化";
        notReadyReason = "正在初始化数据库...";
    }
    
    /**
     * 监听数据库就绪事件
     */
    @EventListener(DatabaseReadyEvent.class)
    public void onDatabaseReady(DatabaseReadyEvent event) {
        currentStatus = StartupStatus.COLLECTIONS_INIT;
        currentPhase = "集合初始化";
        notReadyReason = "正在初始化数据库集合...";
    }
    
    /**
     * 监听集合就绪事件
     */
    @EventListener(CollectionsReadyEvent.class)
    public void onCollectionsReady(CollectionsReadyEvent event) {
        currentStatus = StartupStatus.PLUGINS_INIT;
        currentPhase = "插件初始化";
        notReadyReason = "正在初始化消息处理插件...";
    }
    
    /**
     * 监听插件就绪事件
     */
    @EventListener(PluginsReadyEvent.class)
    public void onPluginsReady(PluginsReadyEvent event) {
        currentStatus = StartupStatus.MESSAGE_SOURCES_INIT;
        currentPhase = "消息源初始化";
        notReadyReason = "正在初始化消息源...";
    }
    
    /**
     * 监听消息源就绪事件
     */
    @EventListener(MessageSourcesReadyEvent.class)
    public void onMessageSourcesReady(MessageSourcesReadyEvent event) {
        currentStatus = StartupStatus.API_INIT;
        currentPhase = "API初始化";
        notReadyReason = "正在初始化API...";
    }
    
    /**
     * 监听API就绪事件
     */
    @EventListener(ApiReadyEvent.class)
    public void onApiReady(ApiReadyEvent event) {
        currentStatus = StartupStatus.READY;
        currentPhase = "应用就绪";
        notReadyReason = null;
    }
    
    /**
     * 监听Spring应用就绪事件
     * <p>
     * 当Spring容器完全启动后，等待所有关键组件初始化完成
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        startupStartTime = event.getTimestamp();
        log.info("Spring应用已就绪，开始检查系统组件状态...");
        
        // 在新线程中异步检查系统状态，避免阻塞主线程
        new Thread(this::checkSystemReadiness, "SystemReadyChecker").start();
    }
    
    /**
     * 检查系统就绪状态
     * <p>
     * 等待所有关键组件初始化完成，然后标记系统为就绪状态
     * </p>
     */
    private void checkSystemReadiness() {
        try {
            log.info("开始检查系统组件就绪状态...");
            
            // 等待一小段时间，确保所有@EventListener(ApplicationReadyEvent.class)都执行完成
            // 这包括TagBasedMessageForwardingPlugin和TagFilterConfigServiceImpl的初始化
            Thread.sleep(2000);
            
            // TODO: 可以在这里添加更多的就绪检查
            // 例如：检查Telegram客户端连接状态、MongoDB连接状态等
            
            long startupTime = System.currentTimeMillis() - startupStartTime;
            
            // 标记系统为就绪状态
            systemReady.set(true);
            currentStatus = StartupStatus.READY;
            notReadyReason = null;
            currentPhase = "应用就绪";
            
            // 发布系统就绪事件
            eventPublisher.publishEvent(new SystemReadyEvent(this, startupTime));
            
            log.info("✓ 系统完全就绪，启动耗时: {}ms，API现在可以接受请求", startupTime);
            
        } catch (InterruptedException e) {
            log.error("系统就绪检查被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("系统就绪检查失败", e);
            notReadyReason = "系统初始化失败: " + e.getMessage();
            currentStatus = StartupStatus.FAILED;
        }
    }
    
    /**
     * 获取启动进度（0-100）
     * <p>
     * 根据当前启动状态计算进度百分比
     * </p>
     */
    public int getProgress() {
        return switch (currentStatus) {
            case NOT_STARTED -> 0;
            case CONFIGURATION_INIT -> 10;
            case DATABASE_INIT -> 25;
            case COLLECTIONS_INIT -> 40;
            case PLUGINS_INIT -> 55;
            case MESSAGE_SOURCES_INIT -> 70;
            case API_INIT -> 85;
            case READY -> 100;
            case FAILED -> -1;
        };
    }
    
    /**
     * 手动标记系统为就绪状态
     * <p>
     * 用于测试或特殊场景
     * </p>
     */
    public void markAsReady() {
        if (systemReady.compareAndSet(false, true)) {
            notReadyReason = null;
            currentStatus = StartupStatus.READY;
            currentPhase = "应用就绪";
            long startupTime = System.currentTimeMillis() - startupStartTime;
            eventPublisher.publishEvent(new SystemReadyEvent(this, startupTime));
            log.info("系统已手动标记为就绪状态");
        }
    }
    
    /**
     * 手动标记系统为未就绪状态
     * <p>
     * 用于维护模式或紧急情况
     * </p>
     */
    public void markAsNotReady(String reason) {
        if (systemReady.compareAndSet(true, false)) {
            notReadyReason = reason;
            currentStatus = StartupStatus.NOT_STARTED;
            currentPhase = "未启动";
            log.warn("系统已标记为未就绪状态: {}", reason);
        }
    }
}

