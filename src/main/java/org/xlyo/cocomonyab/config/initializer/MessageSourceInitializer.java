package org.xlyo.cocomonyab.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.PluginsReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;
import org.xlyo.cocomonyab.source.MessageSourceRegistry;
import org.xlyo.cocomonyab.source.telegram.TelegramMessageSource;
import org.xlyo.cocomonyab.source.unread.UnreadMessageSource;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

/**
 * 消息源初始化器
 * <p>
 * 负责应用启动时的消息源初始化阶段，包括：
 * <ul>
 *   <li>监听插件就绪事件</li>
 *   <li>初始化 TelegramClientManager</li>
 *   <li>验证 Telegram API 配置（API_ID、API_HASH、TG_PHONE）</li>
 *   <li>执行 Telegram 客户端登录</li>
 *   <li>注册消息源（TelegramMessageSource、UnreadMessageSource）</li>
 *   <li>启动所有消息源</li>
 *   <li>发布消息源就绪事件</li>
 * </ul>
 * </p>
 * <p>
 * 这是启动流程的第五个阶段，依赖于插件初始化阶段的完成
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSourceInitializer {
    
    private final TelegramClientManager telegramClientManager;
    private final MessageSourceRegistry messageSourceRegistry;
    private final TelegramMessageSource telegramMessageSource;
    private final UnreadMessageSource unreadMessageSource;
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 监听插件就绪事件，开始消息源初始化
     * <p>
     * 当插件初始化完成后，此方法会被自动调用。
     * 执行 Telegram 客户端初始化、消息源注册和启动，并在成功后发布消息源就绪事件。
     * </p>
     *
     * @param event 插件就绪事件
     */
    @EventListener
    public void onPluginsReady(PluginsReadyEvent event) {
        progressTracker.startPhase("消息源初始化");
        
        try {
            log.info("开始消息源初始化...");
            
            // 1. 初始化 Telegram 客户端
            // TelegramClientManager 的 @PostConstruct 方法已经执行了配置验证和登录
            // 这里只需要验证客户端是否就绪
            log.info("验证 Telegram 客户端状态...");
            if (!telegramClientManager.isReady()) {
                throw new StartupException("Telegram 客户端未就绪");
            }
            log.info("✅ Telegram 客户端已就绪");
            
            // 2. 注册消息源
            log.info("注册消息源...");
            registerMessageSources();
            log.info("✅ 消息源注册完成");
            
            // 3. 启动所有消息源
            log.info("启动所有消息源...");
            messageSourceRegistry.startAll();
            log.info("✅ 所有消息源已启动");
            
            // 4. 发布消息源就绪事件
            eventPublisher.publishMessageSourcesReady();
            
            progressTracker.completePhase("消息源初始化");
            log.info("✅ 消息源初始化完成");
            
        } catch (Exception e) {
            progressTracker.failPhase("消息源初始化", e.getMessage());
            log.error("❌ 消息源初始化失败", e);
            throw new StartupException("消息源初始化失败", e);
        }
    }
    
    /**
     * 注册消息源到注册表
     * <p>
     * 按顺序注册：
     * <ol>
     *   <li>TelegramMessageSource - Telegram 官方消息来源</li>
     *   <li>UnreadMessageSource - 未读消息来源</li>
     * </ol>
     * </p>
     */
    private void registerMessageSources() {
        // 注册 Telegram 消息来源
        messageSourceRegistry.register(telegramMessageSource);
        log.info("已注册消息源: {} ({})", 
            telegramMessageSource.getSourceName(), 
            telegramMessageSource.getSourceId());
        
        // 注册未读消息来源
        messageSourceRegistry.register(unreadMessageSource);
        log.info("已注册消息源: {} ({})", 
            unreadMessageSource.getSourceName(), 
            unreadMessageSource.getSourceId());
    }
}
