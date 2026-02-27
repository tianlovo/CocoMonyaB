package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用于API响应的频道视图对象
 * 包含返回给客户端的所有频道信息
 */
@Data
public class ChannelVO {

    /**
     * MongoDB文档ID
     */
    private String id;

    /**
     * Telegram频道ID
     */
    private Long channelId;

    /**
     * 频道用户名
     */
    private String channelUsername;

    /**
     * 频道显示标题
     */
    private String channelTitle;

    /**
     * 此频道是否处于监控激活状态
     */
    private Boolean monitoringStatus;

    /**
     * 频道创建时间戳
     */
    private LocalDateTime createTime;

    /**
     * 频道最后更新时间戳
     */
    private LocalDateTime updateTime;
}