package org.xlyo.cocomonyab.telegram.handler;

import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.service.TgChannelService;

/**
 * Telegram 聊天位置更新处理器
 * 接收 UpdateChatPosition 事件，当频道位置变化时自动刷新频道列表缓存
 * <p>
 * UpdateChatPosition: 当聊天在聊天列表中的位置发生变化时触发
 * 这包括：
 * - 用户加入新频道（频道出现在列表中）
 * - 用户退出频道（频道从列表中移除，order=0）
 * - 频道位置因新消息等原因发生变化
 * <p>
 * 使用 @Lazy 注解打破循环依赖
 */
@Slf4j
@Component
public class TgUpdateChatPositionHandler {
    
    private final TgChannelService tgChannelService;
    
    /**
     * 构造函数，使用 @Lazy 注解延迟注入 TgChannelService
     */
    public TgUpdateChatPositionHandler(@Lazy TgChannelService tgChannelService) {
        this.tgChannelService = tgChannelService;
    }
    
    /**
     * 处理聊天位置更新
     * <p>
     * 当检测到聊天位置变化时，清除频道列表缓存
     * 特别是当order=0时，表示聊天从列表中移除（用户退出频道）
     */
    public void onChatPositionUpdate(TdApi.UpdateChatPosition update) {
        long chatId = update.chatId;
        TdApi.ChatPosition position = update.position;
        
        // 检查是否为主聊天列表
        if (position.list instanceof TdApi.ChatListMain) {
            if (position.order == 0) {
                // order=0 表示聊天从列表中移除
                log.info("检测到聊天从列表中移除: chatId={}", chatId);
            } else {
                // 聊天位置变化
                log.debug("检测到聊天位置变化: chatId={}, order={}", chatId, position.order);
            }
            
            // 清除频道列表缓存，以便下次查询时获取最新数据
            tgChannelService.evictChannelsCache();
            log.info("已清除频道列表缓存 (聊天位置变化)");
        }
    }
}
