package org.xlyo.cocomonyab.service;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.vo.TgChannelVO;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Telegram频道服务
 * 负责从TDLib获取已登录账号的频道列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TgChannelService {

    private final TelegramClientManager telegramClientManager;

    /**
     * 获取已登录账号的所有频道列表（分页）
     *
     * @param current 当前页码
     * @param size 每页大小
     * @return 频道列表
     */
    public List<TgChannelVO> getLoggedInChannels(Long current, Long size) {
        // 验证客户端是否就绪
        if (!telegramClientManager.isReady()) {
            throw new BusinessException(ResponseCode.TELEGRAM_ERROR, "Telegram客户端未就绪");
        }

        // 验证分页参数
        if (current == null || current < 1) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "页码必须大于等于1");
        }
        if (size == null || size < 1) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "每页大小必须大于等于1");
        }
        if (size > 100) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "每页大小不能超过100");
        }

        SimpleTelegramClient client = telegramClientManager.getClient();
        List<TgChannelVO> channels = new ArrayList<>();

        try {
            // 1. 获取聊天列表
            TdApi.Chats chats = client.send(new TdApi.GetChats(new TdApi.ChatListMain(), 100))
                    .get(30, TimeUnit.SECONDS);

            log.debug("获取到 {} 个聊天", chats.chatIds.length);

            // 2. 遍历聊天，筛选频道
            for (long chatId : chats.chatIds) {
                try {
                    TdApi.Chat chat = client.send(new TdApi.GetChat(chatId))
                            .get(5, TimeUnit.SECONDS);

                    // 只处理超级群组类型的聊天
                    if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
                        // 获取超级群组详细信息
                        TdApi.Supergroup supergroup = client.send(
                                new TdApi.GetSupergroup(supergroupType.supergroupId))
                                .get(5, TimeUnit.SECONDS);

                        // 只添加频道（isChannel=true）
                        if (supergroup.isChannel) {
                            TgChannelVO vo = convertToVO(chat, supergroup);
                            channels.add(vo);
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取聊天 {} 详情失败: {}", chatId, e.getMessage());
                }
            }

            log.info("共找到 {} 个频道", channels.size());

            // 3. 应用分页
            return applyPagination(channels, current, size);

        } catch (Exception e) {
            log.error("获取Telegram频道列表失败", e);
            throw new BusinessException(ResponseCode.TELEGRAM_ERROR, 
                    "获取Telegram频道列表失败: " + e.getMessage());
        }
    }

    /**
     * 统计已登录账号的频道总数
     *
     * @return 频道总数
     */
    public Long countLoggedInChannels() {
        // 验证客户端是否就绪
        if (!telegramClientManager.isReady()) {
            throw new BusinessException(ResponseCode.TELEGRAM_ERROR, "Telegram客户端未就绪");
        }

        SimpleTelegramClient client = telegramClientManager.getClient();
        int count = 0;

        try {
            // 1. 获取聊天列表
            TdApi.Chats chats = client.send(new TdApi.GetChats(new TdApi.ChatListMain(), 1000))
                    .get(30, TimeUnit.SECONDS);

            // 2. 遍历聊天，统计频道数量
            for (long chatId : chats.chatIds) {
                try {
                    TdApi.Chat chat = client.send(new TdApi.GetChat(chatId))
                            .get(5, TimeUnit.SECONDS);

                    if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
                        TdApi.Supergroup supergroup = client.send(
                                new TdApi.GetSupergroup(supergroupType.supergroupId))
                                .get(5, TimeUnit.SECONDS);

                        if (supergroup.isChannel) {
                            count++;
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取聊天 {} 详情失败: {}", chatId, e.getMessage());
                }
            }

            return (long) count;

        } catch (Exception e) {
            log.error("统计Telegram频道数量失败", e);
            throw new BusinessException(ResponseCode.TELEGRAM_ERROR,
                    "统计Telegram频道数量失败: " + e.getMessage());
        }
    }

    /**
     * 将TDLib的Chat和Supergroup转换为VO
     */
    private TgChannelVO convertToVO(TdApi.Chat chat, TdApi.Supergroup supergroup) {
        TgChannelVO vo = new TgChannelVO();
        vo.setChatId(chat.id);
        vo.setTitle(chat.title);
        vo.setUsername(supergroup.usernames != null && supergroup.usernames.activeUsernames != null 
                && supergroup.usernames.activeUsernames.length > 0 
                ? supergroup.usernames.activeUsernames[0] 
                : null);
        vo.setType("channel");
        vo.setIsChannel(supergroup.isChannel);
        vo.setMemberCount(supergroup.memberCount);
        vo.setDescription(null); // 基本信息中不包含描述，需要额外查询
        return vo;
    }

    /**
     * 应用分页逻辑
     */
    private List<TgChannelVO> applyPagination(List<TgChannelVO> allChannels, Long current, Long size) {
        int start = (int) ((current - 1) * size);
        int end = (int) Math.min(start + size, allChannels.size());

        if (start >= allChannels.size()) {
            return new ArrayList<>();
        }

        return allChannels.subList(start, end);
    }
}
