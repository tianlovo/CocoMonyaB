package org.xlyo.cocomonyab.source.telegram;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.service.ChannelMonitorService;
import org.xlyo.cocomonyab.source.AbstractMessageSource;
import org.xlyo.cocomonyab.source.MessageSourceException;
import org.xlyo.cocomonyab.source.MessageSourceHealth;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

/**
 * Telegram 官方消息来源
 * <p>
 * 通过 TDLight 库监听 Telegram 的 UpdateNewMessage 事件，
 * 将接收到的频道消息转发给消息处理系统。
 * <p>
 * 这是系统的默认消息来源，也是最主要的消息来源。
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageSource extends AbstractMessageSource {
    
    private final TelegramClientManager telegramClientManager;
    private final ChannelMonitorService channelMonitorService;
    
    @Override
    public String getSourceId() {
        return "telegram-official";
    }
    
    @Override
    public String getSourceName() {
        return "Telegram 官方来源";
    }
    
    @Override
    public String getDescription() {
        return "通过 TDLight 库监听 Telegram 官方 API，接收频道消息更新";
    }
    
    @Override
    protected void doStart() throws Exception {
        // Telegram 客户端由 TelegramClientManager 管理，在应用启动时自动初始化
        // 这里只需要验证客户端是否就绪
        if (!telegramClientManager.isReady()) {
            throw new MessageSourceException("Telegram 客户端未就绪");
        }
        
        log.info("Telegram 消息来源已启动，当前用户: {}", 
            telegramClientManager.getCurrentUser().firstName);
    }
    
    @Override
    protected void doStop() {
        // Telegram 客户端由 TelegramClientManager 管理，在应用关闭时自动清理
        // 这里不需要额外操作
        log.info("Telegram 消息来源已停止");
    }
    
    @Override
    protected void addCustomHealthMetrics(MessageSourceHealth health) {
        // 添加 Telegram 特定的健康指标
        health.addMetric("telegram_ready", telegramClientManager.isReady());
        
        TdApi.User currentUser = telegramClientManager.getCurrentUser();
        if (currentUser != null) {
            health.addMetric("telegram_user_id", currentUser.id);
            health.addMetric("telegram_user_name", currentUser.firstName);
        }
        
        health.addMetric("monitoring_channels", channelMonitorService.getMonitoringChannelCount());
    }
    
    /**
     * 处理新消息更新
     * <p>
     * 此方法由 TgUpdateNewMessageHandler 调用
     * 
     * @param message Telegram 消息
     */
    public void handleNewMessage(TdApi.Message message) {
        try {
            // 记录接收到消息
            recordMessageReceived();
            
            log.info("📡 [Telegram来源] 接收到Telegram消息: chatId={}, messageId={}, isChannelPost={}", 
                message.chatId, message.id, message.isChannelPost);
            
            // 只处理频道消息
            if (!message.isChannelPost) {
                log.debug("跳过非频道消息: chatId={}, messageId={}", message.chatId, message.id);
                return;
            }
            
            log.info("➡️ [转发处理] 转发消息到处理服务: chatId={}, messageId={}", 
                message.chatId, message.id);
            
            // 转发给消息处理服务
            channelMonitorService.handleNewMessage(message);
            
            // 记录处理成功
            recordMessageProcessed();
            
            log.info("✅ [Telegram来源] 消息处理成功: chatId={}, messageId={}", 
                message.chatId, message.id);
            
        } catch (Exception e) {
            log.error("❌ [Telegram来源] 处理Telegram消息失败: chatId={}, messageId={}", 
                message.chatId, message.id, e);
            recordMessageFailed("处理消息失败: " + e.getMessage());
        }
    }
}
