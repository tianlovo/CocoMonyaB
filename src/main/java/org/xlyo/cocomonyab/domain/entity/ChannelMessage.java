package org.xlyo.cocomonyab.domain.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 频道消息实体
 * 存储从监控频道接收到的消息
 */
@Document(collection = "channel_messages")
@CompoundIndexes({
    @CompoundIndex(
        name = "chat_message_unique", 
        def = "{'chatId': 1, 'messageId': 1}", 
        unique = true
    ),
    @CompoundIndex(
        name = "chat_date_idx", 
        def = "{'chatId': 1, 'date': -1}"
    ),
    @CompoundIndex(
        name = "status_date_idx", 
        def = "{'status': 1, 'createTime': -1}"
    ),
    @CompoundIndex(
        name = "media_album_idx", 
        def = "{'chatId': 1, 'mediaAlbumId': 1, 'date': 1}"
    )
})
@Data
public class ChannelMessage {
    
    @Id
    private String id;
    
    @Indexed
    private Long messageId;
    
    @Indexed
    private Long chatId;
    
    private String channelUsername;
    private String channelTitle;
    
    private Integer date;
    private Integer editDate;
    
    private String contentType;
    private String textContent;
    private List<MediaFile> mediaFiles;
    
    // Telegraph/WebPage 相关字段
    private WebPageInfo webPage;
    
    // 媒体组相关字段
    @Indexed
    private Long mediaAlbumId;           // 媒体组ID，0表示不属于任何组
    private Boolean isMediaGroup;        // 是否为媒体组消息
    private Integer mediaGroupItemCount; // 媒体组中的项目数量
    private List<Long> mediaGroupMessageIds; // 媒体组中所有消息的ID列表
    
    private Integer views;
    private Integer forwards;
    
    @Indexed
    private MessageStatus status;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    /**
     * 媒体文件信息
     */
    @Data
    public static class MediaFile {
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
    public static class WebPageInfo {
        private String url;                  // 网页URL
        private String displayUrl;           // 显示URL
        private String type;                 // 类型：article, photo, video等
        private String siteName;             // 网站名称
        private String title;                // 标题
        private String description;          // 描述
        private String author;               // 作者
        private Integer duration;            // 视频/音频时长
        private Boolean hasInstantView;      // 是否有即时预览（Telegraph）
        private String instantViewVersion;   // 即时预览版本
    }
    
    /**
     * 频道消息状态枚举
     */
    public enum MessageStatus {
        PENDING,    // 待审核
        APPROVED,   // 已通过
        REJECTED    // 已拒绝
    }
}
