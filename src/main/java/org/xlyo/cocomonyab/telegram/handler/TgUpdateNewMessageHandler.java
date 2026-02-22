package org.xlyo.cocomonyab.telegram.handler;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.service.ChannelMonitorService;
import org.xlyo.cocomonyab.service.message.MessageStorageService;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.plugin.PluginManager;

/**
 * Telegram 新消息更新处理器
 * 接收 UpdateNewMessage 事件并处理频道消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TgUpdateNewMessageHandler {
    
    private final ChannelMonitorService channelMonitorService;
    private final MessageStorageService messageStorageService;
    private final MessageParser messageParser;
    private final PluginManager pluginManager;
    
    /**
     * 处理新消息更新
     */
    public void onNewMessageUpdate(TdApi.UpdateNewMessage update) {
        TdApi.Message message = update.message;
        
        // 只处理频道消息
        if (!message.isChannelPost) {
            return;
        }
        
        // 检查是否为监控频道
        if (!channelMonitorService.isMonitoring(message.chatId)) {
            log.debug("收到非监控频道的消息，跳过: chatId={}", message.chatId);
            return;
        }
        
        // 处理消息（使用新的插件系统）
        // 注意：媒体组消息仍然由ChannelMonitorService处理（缓冲机制）
        channelMonitorService.handleNewMessage(message);
    }
}
