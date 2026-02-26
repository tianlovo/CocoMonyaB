package org.xlyo.cocomonyab.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.xlyo.cocomonyab.source.MessageSourceException;
import org.xlyo.cocomonyab.source.MessageSourceRegistry;
import org.xlyo.cocomonyab.source.telegram.TelegramMessageSource;

/**
 * 消息来源配置类
 * <p>
 * 负责在应用启动时注册和启动所有消息来源，
 * 在应用关闭时停止所有消息来源。
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MessageSourceConfiguration {
    
    private final MessageSourceRegistry messageSourceRegistry;
    private final TelegramMessageSource telegramMessageSource;
    
    /**
     * 应用启动时注册和启动所有消息来源
     */
    @PostConstruct
    public void initMessageSources() {
        log.info("初始化消息来源系统");
        
        try {
            // 注册 Telegram 官方消息来源
            messageSourceRegistry.register(telegramMessageSource);
            log.info("已注册消息来源: {}", telegramMessageSource.getSourceName());
            
            // 启动 Telegram 消息来源
            messageSourceRegistry.startSource(telegramMessageSource.getSourceId());
            log.info("已启动消息来源: {}", telegramMessageSource.getSourceName());
            
            // 输出统计信息
            log.info("消息来源系统初始化完成，已注册 {} 个消息来源，运行中 {} 个",
                messageSourceRegistry.getSourceCount(),
                messageSourceRegistry.getRunningSources().size());
            
        } catch (MessageSourceException e) {
            log.error("初始化消息来源失败", e);
            throw new RuntimeException("消息来源系统初始化失败", e);
        }
    }
    
    /**
     * 应用关闭时停止所有消息来源
     */
    @PreDestroy
    public void shutdownMessageSources() {
        log.info("关闭消息来源系统");
        
        try {
            messageSourceRegistry.stopAll();
            log.info("所有消息来源已停止");
        } catch (Exception e) {
            log.error("关闭消息来源失败", e);
        }
    }
}
