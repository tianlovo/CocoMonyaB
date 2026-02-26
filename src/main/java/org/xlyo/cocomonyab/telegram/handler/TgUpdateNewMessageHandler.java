package org.xlyo.cocomonyab.telegram.handler;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.source.telegram.TelegramMessageSource;

/**
 * Telegram 新消息更新处理器
 * 接收 UpdateNewMessage 事件并转发给 TelegramMessageSource
 * <p>
 * 注意：此类作为 TDLight 和消息来源系统之间的桥梁
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgUpdateNewMessageHandler {
    
    private final TelegramMessageSource telegramMessageSource;
    
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
