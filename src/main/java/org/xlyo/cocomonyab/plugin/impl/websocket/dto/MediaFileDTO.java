package org.xlyo.cocomonyab.plugin.impl.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体文件信息数据传输对象
 * 
 * <p>该DTO用于传输图片、视频、文档、音频等媒体文件的元数据信息。
 * 不包含实际的文件内容，只包含文件的描述信息和Telegram文件ID。</p>
 * 
 * <h2>适用场景</h2>
 * <ul>
 *   <li>图片消息: 包含多个不同尺寸的图片</li>
 *   <li>视频消息: 包含视频文件的尺寸、时长等信息</li>
 *   <li>文档消息: 包含文件名、大小、MIME类型等信息</li>
 *   <li>音频消息: 包含音频时长、MIME类型等信息</li>
 *   <li>语音消息: 包含语音时长等信息</li>
 *   <li>其他媒体类型: 动画、贴纸、视频笔记等</li>
 * </ul>
 * 
 * <h2>文件获取</h2>
 * <p>客户端可以使用fileId通过Telegram Bot API获取实际的文件内容。
 * fileUniqueId是文件的永久唯一标识符，即使文件被重新上传也不会改变。</p>
 * 
 * <h2>使用示例</h2>
 * <pre>
 * // 创建视频文件DTO
 * MediaFileDTO video = MediaFileDTO.builder()
 *     .fileId("BAACAgIAAxkBAAIBCmXxxx...")
 *     .fileUniqueId("AgADxxx")
 *     .fileSize(1024000L)
 *     .mimeType("video/mp4")
 *     .fileName("example.mp4")
 *     .width(1920)
 *     .height(1080)
 *     .duration(120)
 *     .build();
 * </pre>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileDTO {
    
    /**
     * 文件ID
     * <p>Telegram文件的唯一标识符，可用于通过Bot API下载文件。</p>
     * <p>注意: 此ID可能会在文件重新上传后改变。</p>
     */
    private String fileId;
    
    /**
     * 文件唯一ID
     * <p>文件的永久唯一标识符，即使文件被重新上传也不会改变。</p>
     * <p>可用于识别相同的文件。</p>
     */
    private String fileUniqueId;
    
    /**
     * 文件大小
     * <p>文件的字节大小。</p>
     * <p>示例: {@code 1024000L} 表示约1MB</p>
     */
    private Long fileSize;
    
    /**
     * MIME类型
     * <p>文件的MIME类型。</p>
     * <p>示例: {@code "video/mp4"}, {@code "image/jpeg"}, {@code "application/pdf"}</p>
     */
    private String mimeType;
    
    /**
     * 文件名
     * <p>文件的原始名称（如果有）。</p>
     * <p>主要用于文档类型的消息。</p>
     */
    private String fileName;
    
    // ==================== 图片/视频特有字段 ====================
    
    /**
     * 宽度
     * <p>图片或视频的宽度（像素）。</p>
     * <p>仅适用于图片、视频、动画等类型。</p>
     */
    private Integer width;
    
    /**
     * 高度
     * <p>图片或视频的高度（像素）。</p>
     * <p>仅适用于图片、视频、动画等类型。</p>
     */
    private Integer height;
    
    /**
     * 时长
     * <p>视频或音频的时长（秒）。</p>
     * <p>仅适用于视频、音频、语音、视频笔记等类型。</p>
     */
    private Integer duration;
    
    // ==================== 缩略图 ====================
    
    /**
     * 缩略图文件ID
     * <p>媒体文件缩略图的文件ID（如果有）。</p>
     * <p>可用于通过Bot API获取缩略图。</p>
     */
    private String thumbnailFileId;
}
