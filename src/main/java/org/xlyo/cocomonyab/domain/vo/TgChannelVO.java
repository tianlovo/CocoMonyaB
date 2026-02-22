package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

/**
 * Telegram频道视图对象
 * 用于返回从TDLib获取的频道信息
 */
@Data
public class TgChannelVO {

    /**
     * 聊天ID（频道ID）
     */
    private Long chatId;

    /**
     * 频道标题
     */
    private String title;

    /**
     * 频道用户名（不含@符号）
     */
    private String username;

    /**
     * 频道类型（channel或supergroup）
     */
    private String type;

    /**
     * 是否为频道（true=频道，false=超级群组）
     */
    private Boolean isChannel;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 频道描述
     */
    private String description;
}
