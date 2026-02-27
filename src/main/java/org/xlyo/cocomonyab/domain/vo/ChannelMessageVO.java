package org.xlyo.cocomonyab.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 频道消息视图对象
 * 用于API响应的频道消息数据
 */
@Data
public class ChannelMessageVO {
    
    /**
     * MongoDB文档ID
     */
    private String id;
    
    /**
     * 消息ID（Telegram）
     */
    private Long messageId;
    
    /**
     * 频道ID（Telegram）
     */
    private Long chatId;
    
    /**
     * 频道用户名
     */
    private String channelUsername;
    
    /**
     * 频道标题
     */
    private String channelTitle;
    
    /**
     * 消息日期（Unix时间戳）
     */
    private Integer date;
    
    /**
     * 编辑日期（Unix时间戳）
     */
    private Integer editDate;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 文本内容
     */
    private String textContent;
    
    /**
     * 媒体文件列表
     */
    private List<MediaFileVO> mediaFiles;
    
    /**
     * 网页信息
     */
    private WebPageInfoVO webPage;
    
    /**
     * 媒体组ID
     */
    private Long mediaAlbumId;
    
    /**
     * 是否为媒体组
     */
    private Boolean isMediaGroup;
    
    /**
     * 媒体组项目数量
     */
    private Integer mediaGroupItemCount;
    
    /**
     * 媒体组消息ID列表
     */
    private List<Long> mediaGroupMessageIds;
    
    /**
     * 浏览次数
     */
    private Integer views;
    
    /**
     * 转发次数
     */
    private Integer forwards;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 媒体文件信息
     */
    @Data
    public static class MediaFileVO {
        private String fileId;
        private String fileType;
        private Long fileSize;
        private String mimeType;
        private String localPath;
        private Boolean downloaded;
    }
    
    /**
     * WebPage/Telegraph 信息
     */
    @Data
    public static class WebPageInfoVO {
        private String url;
        private String displayUrl;
        private String type;
        private String siteName;
        private String title;
        private String description;
        private String author;
        private Integer duration;
        private Boolean hasInstantView;
        private String instantViewVersion;
    }
}
