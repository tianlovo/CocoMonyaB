package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;

/**
 * 媒体文件信息
 * 用于图片、视频、文档等媒体类型消息
 */
@Data
public class MediaFile {
    /**
     * 文件ID
     */
    private Integer fileId;
    
    /**
     * 文件唯一ID
     */
    private String fileUniqueId;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件路径（本地路径）
     */
    private String filePath;
    
    /**
     * 文件宽度（图片/视频）
     */
    private Integer width;
    
    /**
     * 文件高度（图片/视频）
     */
    private Integer height;
    
    /**
     * 时长（视频/音频，秒）
     */
    private Integer duration;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 文件名
     */
    private String fileName;
}
