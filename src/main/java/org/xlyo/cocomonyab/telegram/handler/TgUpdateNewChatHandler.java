package org.xlyo.cocomonyab.telegram.handler;

import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.service.TgChannelService;

/**
 * Telegram 新聊天更新处理器
 * 接收 UpdateNewChat 事件，当新频道加载时自动刷新频道列表缓存
 * <p>
 * UpdateNewChat: 当新聊天被加载/创建时触发
 * 这个更新保证在聊天标识符返回给应用程序之前到达
 * <p>
 * 使用 @Lazy 注解打破循环依赖
 */
@Slf4j
@Component
public class TgUpdateNewChatHandler {
    
    private final TgChannelService tgChannelService;
    
    /**
     * 构造函数，使用 @Lazy 注解延迟注入 TgChannelService
     */
    public TgUpdateNewChatHandler(@Lazy TgChannelService tgChannelService) {
        this.tgChannelService = tgChannelService;
    }
    
    /**
     * 处理新聊天更新
     * <p>
     * 当检测到新的频道（超级群组且isChannel=true）时，清除频道列表缓存
     */
    public void onNewChatUpdate(TdApi.UpdateNewChat update) {
        TdApi.Chat chat = update.chat;
        
        // 只处理超级群组类型的聊天
        if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
            log.info("检测到新聊天: chatId={}, title={}, supergroupId={}", 
                    chat.id, chat.title, supergroupType.supergroupId);
            
            // 清除频道列表缓存，以便下次查询时获取最新数据
            // 注意：这里无法判断是否为频道，因为需要额外查询Supergroup信息
            // 为了确保数据一致性，统一清除缓存
            tgChannelService.evictChannelsCache();
            log.info("已清除频道列表缓存 (新聊天加载)");
        }
    }
}
