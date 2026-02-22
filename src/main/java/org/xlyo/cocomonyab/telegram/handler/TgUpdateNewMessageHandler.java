package org.xlyo.cocomonyab.telegram.handler;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.service.ChannelMonitorService;

/**
 * Telegram 新消息更新处理器
 * 接收 UpdateNewMessage 事件并处理频道消息
 * <p>
 * 注意：频道监控检查已移至 ChannelMonitoringFilter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgUpdateNewMessageHandler {
    
    private final ChannelMonitorService channelMonitorService;
    
    /**
     * 处理新消息更新
     */
    public void onNewMessageUpdate(TdApi.UpdateNewMessage update) {
        TdApi.Message message = update.message;
        
        // 只处理频道消息
        if (!message.isChannelPost) {
            return;
        }
        
        // 处理消息（监控检查由 ChannelMonitoringFilter 完成）
        channelMonitorService.handleNewMessage(message);
    }
}
