package org.xlyo.cocomonyab.plugin.impl.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebSocket消息广播数据传输对象
 * 
 * <p>该DTO用于通过STOMP+WebSocket将Telegram频道消息广播给客户端。
 * 包含消息的所有必要信息，支持多种消息类型（文本、图片、视频、文档等）。</p>
 * 
 * <h2>字段分类</h2>
 * <ul>
 *   <li><strong>基础字段</strong>: 所有消息类型都包含的字段（messageId, chatId等）</li>
 *   <li><strong>互动信息</strong>: 消息的互动数据（views, forwards）</li>
 *   <li><strong>媒体信息</strong>: 根据消息类型可选的媒体文件信息</li>
 *   <li><strong>WebPage信息</strong>: Telegraph消息的网页预览信息</li>
 *   <li><strong>媒体组信息</strong>: 媒体组消息的特殊字段</li>
 *   <li><strong>投票信息</strong>: 投票消息的问题和选项</li>
 * </ul>
 * 
 * <h2>消息类型支持</h2>
 * <ul>
 *   <li>{@code TEXT}: 纯文本消息，只包含textContent</li>
 *   <li>{@code PHOTO}: 图片消息，包含photos列表和可选的caption</li>
 *   <li>{@code VIDEO}: 视频消息，包含video对象和可选的caption</li>
 *   <li>{@code DOCUMENT}: 文档消息，包含document对象和可选的caption</li>
 *   <li>{@code AUDIO}: 音频消息，包含audio对象和可选的caption</li>
 *   <li>{@code VOICE}: 语音消息，包含voice对象和可选的caption</li>
 *   <li>{@code VIDEO_NOTE}: 视频笔记，包含videoNote对象</li>
 *   <li>{@code ANIMATION}: 动画消息，包含animation对象和可选的caption</li>
 *   <li>{@code STICKER}: 贴纸消息，包含sticker对象</li>
 *   <li>{@code TELEGRAPH}: Telegraph消息，包含webPage对象</li>
 *   <li>{@code MEDIA_GROUP}: 媒体组消息，包含items列表</li>
 *   <li>{@code POLL}: 投票消息，包含pollQuestion和pollOptions</li>
 * </ul>
 * 
 * <h2>JSON序列化</h2>
 * <p>该DTO会被自动序列化为JSON格式通过WebSocket发送给客户端。
 * 所有字段都使用标准的Java命名规范，序列化后会转换为驼峰命名。</p>
 * 
 * <h2>使用示例</h2>
 * <pre>
 * // 创建文本消息DTO
 * MessageBroadcastDTO dto = MessageBroadcastDTO.builder()
 *     .messageId(12345L)
 *     .chatId(-1001234567890L)
 *     .channelUsername("example_channel")
 *     .channelTitle("Example Channel")
 *     .date(1234567890)
 *     .contentType("TEXT")
 *     .textContent("Hello, World!")
 *     .views(100)
 *     .forwards(5)
 *     .build();
 * </pre>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @see MediaFileDTO
 * @see WebPageDTO
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageBroadcastDTO {
    
    // ==================== 基础字段 ====================
    
    /**
     * 消息ID
     * <p>Telegram消息的唯一标识符，在频道内唯一。</p>
     */
    private Long messageId;
    
    /**
     * 频道ID
     * <p>Telegram频道的唯一标识符（chat ID），通常为负数。</p>
     * <p>示例: {@code -1001234567890}</p>
     */
    private Long chatId;
    
    /**
     * 频道用户名
     * <p>频道的公开用户名（不包含@符号）。</p>
     * <p>示例: {@code "example_channel"}</p>
     */
    private String channelUsername;
    
    /**
     * 频道标题
     * <p>频道的显示名称。</p>
     */
    private String channelTitle;
    
    /**
     * 消息发送时间
     * <p>Unix时间戳（秒），表示消息发送的时间。</p>
     */
    private Integer date;
    
    /**
     * 消息内容类型
     * <p>消息类型的字符串表示，对应MessageType枚举。</p>
     * <p>可能的值: TEXT, PHOTO, VIDEO, DOCUMENT, AUDIO, VOICE, VIDEO_NOTE, 
     * ANIMATION, STICKER, POLL, TELEGRAPH, MEDIA_GROUP</p>
     */
    private String contentType;
    
    /**
     * 文本内容
     * <p>消息的文本内容或媒体消息的caption。</p>
     * <p>对于纯文本消息，这是消息的主要内容；
     * 对于媒体消息，这是可选的说明文字。</p>
     */
    private String textContent;
    
    // ==================== 互动信息 ====================
    
    /**
     * 浏览次数
     * <p>消息被查看的次数（仅公开频道可用）。</p>
     */
    private Integer views;
    
    /**
     * 转发次数
     * <p>消息被转发的次数（仅公开频道可用）。</p>
     */
    private Integer forwards;
    
    // ==================== 媒体信息（根据消息类型可选） ====================
    
    /**
     * 图片列表
     * <p>图片消息包含的所有图片（不同尺寸）。</p>
     * <p>仅当contentType为PHOTO时有值。</p>
     */
    private List<MediaFileDTO> photos;
    
    /**
     * 视频文件
     * <p>视频消息包含的视频文件信息。</p>
     * <p>仅当contentType为VIDEO时有值。</p>
     */
    private MediaFileDTO video;
    
    /**
     * 文档文件
     * <p>文档消息包含的文档文件信息。</p>
     * <p>仅当contentType为DOCUMENT时有值。</p>
     */
    private MediaFileDTO document;
    
    /**
     * 音频文件
     * <p>音频消息包含的音频文件信息。</p>
     * <p>仅当contentType为AUDIO时有值。</p>
     */
    private MediaFileDTO audio;
    
    /**
     * 语音文件
     * <p>语音消息包含的语音文件信息。</p>
     * <p>仅当contentType为VOICE时有值。</p>
     */
    private MediaFileDTO voice;
    
    /**
     * 视频笔记文件
     * <p>视频笔记消息包含的视频文件信息（圆形视频）。</p>
     * <p>仅当contentType为VIDEO_NOTE时有值。</p>
     */
    private MediaFileDTO videoNote;
    
    /**
     * 动画文件
     * <p>动画消息包含的GIF或动画文件信息。</p>
     * <p>仅当contentType为ANIMATION时有值。</p>
     */
    private MediaFileDTO animation;
    
    /**
     * 贴纸文件
     * <p>贴纸消息包含的贴纸文件信息。</p>
     * <p>仅当contentType为STICKER时有值。</p>
     */
    private MediaFileDTO sticker;
    
    // ==================== WebPage信息（TELEGRAPH类型） ====================
    
    /**
     * 网页预览信息
     * <p>Telegraph消息包含的网页预览信息。</p>
     * <p>仅当contentType为TELEGRAPH时有值。</p>
     */
    private WebPageDTO webPage;
    
    // ==================== 媒体组信息（MEDIA_GROUP类型） ====================
    
    /**
     * 媒体组ID
     * <p>媒体组的唯一标识符，同一组的所有消息共享此ID。</p>
     * <p>仅当contentType为MEDIA_GROUP时有值。</p>
     */
    private Long mediaAlbumId;
    
    /**
     * 是否为媒体组
     * <p>标识该消息是否为媒体组消息。</p>
     * <p>仅当contentType为MEDIA_GROUP时为true。</p>
     */
    private Boolean isMediaGroup;
    
    /**
     * 媒体组项目数量
     * <p>媒体组包含的消息数量。</p>
     * <p>仅当contentType为MEDIA_GROUP时有值。</p>
     */
    private Integer itemCount;
    
    /**
     * 媒体组项目列表
     * <p>媒体组包含的所有消息（递归结构）。</p>
     * <p>每个项目都是一个完整的MessageBroadcastDTO对象。</p>
     * <p>仅当contentType为MEDIA_GROUP时有值。</p>
     */
    private List<MessageBroadcastDTO> items;
    
    // ==================== 投票信息（POLL类型） ====================
    
    /**
     * 投票问题
     * <p>投票消息的问题文本。</p>
     * <p>仅当contentType为POLL时有值。</p>
     */
    private String pollQuestion;
    
    /**
     * 投票选项列表
     * <p>投票消息的所有选项。</p>
     * <p>仅当contentType为POLL时有值。</p>
     */
    private List<String> pollOptions;
}
