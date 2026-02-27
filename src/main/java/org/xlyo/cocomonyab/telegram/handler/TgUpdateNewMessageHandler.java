package org.xlyo.cocomonyab.telegram.handler;

import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.source.telegram.TelegramMessageSource;

/**
 * Telegram 新消息更新处理器
 * 接收 UpdateNewMessage 事件并转发给 TelegramMessageSource
 * <p>
 * 注意：此类作为 TDLight 和消息来源系统之间的桥梁
 * 使用 @Lazy 注解打破循环依赖
 */
@Slf4j
@Component
public class TgUpdateNewMessageHandler {
    
    private final TelegramMessageSource telegramMessageSource;
    
    /**
     * 构造函数，使用 @Lazy 注解延迟注入 TelegramMessageSource
     */
    public TgUpdateNewMessageHandler(@Lazy TelegramMessageSource telegramMessageSource) {
        this.telegramMessageSource = telegramMessageSource;
    }
    
    /**
     * 处理新消息更新
     * <p>
     * 将 Telegram 的 UpdateNewMessage 事件转发给消息来源处理
     */
    public void onNewMessageUpdate(TdApi.UpdateNewMessage update) {
        TdApi.Message message = update.message;
        
        // 转发给 Telegram 消息来源处理
        telegramMessageSource.handleNewMessage(message);
    }
}
