package org.xlyo.cocomonyab.plugin.impl.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.event.ChannelMonitoringEvent;
import org.xlyo.cocomonyab.plugin.AbstractMessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.impl.websocket.config.WebSocketBroadcastProperties;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.ChannelMonitoringNotificationDTO;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.MediaFileDTO;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.MessageBroadcastDTO;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.WebPageDTO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket消息广播插件
 * 
 * <p>该插件负责将Telegram频道监控系统接收到的消息通过STOMP+WebSocket实时广播给所有订阅的客户端。
 * 这是WebSocket功能的核心和唯一用途 - 专门用于广播监听频道的消息。</p>
 * 
 * <h2>主要功能</h2>
 * <ul>
 *   <li>将接收到的Telegram消息转换为WebSocket兼容的DTO格式</li>
 *   <li>通过STOMP协议广播消息到特定频道的topic</li>
 *   <li>处理频道监控事件并通知客户端</li>
 *   <li>支持所有消息类型（文本、图片、视频、文档、音频、投票等）</li>
 * </ul>
 * 
 * <h2>Topic命名规范</h2>
 * <ul>
 *   <li>频道消息: {@code /topic/channel/real/{channelId}}</li>
 *   <li>频道添加: {@code /topic/channel/monitoring/added}</li>
 *   <li>频道移除: {@code /topic/channel/monitoring/removed}</li>
 *   <li>频道更新: {@code /topic/channel/monitoring/updated}</li>
 * </ul>
 * 
 * <h2>插件特性</h2>
 * <ul>
 *   <li>优先级: 50（在ConsolePrinterPlugin之后执行）</li>
 *   <li>错误处理: 所有异常都会被捕获，不影响其他插件的执行</li>
 *   <li>返回值: 始终返回 {@link PluginResult#CONTINUE}，确保插件链继续执行</li>
 * </ul>
 * 
 * <h2>配置</h2>
 * <p>通过 {@link WebSocketBroadcastProperties} 配置类进行配置，支持以下属性：</p>
 * <ul>
 *   <li>{@code plugin.websocket-broadcast.enabled}: 是否启用插件</li>
 *   <li>{@code plugin.websocket-broadcast.topic-prefix}: 消息广播topic前缀</li>
 *   <li>{@code plugin.websocket-broadcast.monitoring-topic-prefix}: 监控事件topic前缀</li>
 *   <li>{@code plugin.websocket-broadcast.async-broadcast}: 是否启用异步广播</li>
 * </ul>
 * 
 * @author CocoMonyaB Team
 * @version 1.0
 * @see AbstractMessagePlugin
 * @see MessageBroadcastDTO
 * @see WebSocketBroadcastProperties
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketBroadcastPlugin extends AbstractMessagePlugin {
    
    /**
     * Spring消息发送模板，用于通过STOMP协议发送WebSocket消息
     */
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 插件配置属性
     */
    private final WebSocketBroadcastProperties properties;
    
    /**
     * 获取插件名称
     * 
     * @return 插件名称 "WebSocketBroadcastPlugin"
     */
    @Override
    public String getName() {
        return "WebSocketBroadcastPlugin";
    }
    
    /**
     * 获取插件优先级
     * 
     * <p>优先级为50，确保在ConsolePrinterPlugin（优先级0）之后执行。
     * 数值越大，优先级越低，执行顺序越靠后。</p>
     * 
     * @return 优先级值 50
     */
    @Override
    public int getPriority() {
        return 50;
    }
    
    /**
     * 检查插件是否启用
     * 
     * @return 如果插件启用则返回true，否则返回false
     */
    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    /**
     * 处理消息的核心方法
     * 
     * <p>该方法执行以下步骤：</p>
     * <ol>
     *   <li>验证消息实体是否为null</li>
     *   <li>将消息实体转换为WebSocket兼容的DTO格式</li>
     *   <li>构建目标topic路径</li>
     *   <li>通过SimpMessagingTemplate广播消息</li>
     *   <li>记录处理日志</li>
     * </ol>
     * 
     * <p><strong>错误处理策略：</strong></p>
     * <ul>
     *   <li>所有异常都会被捕获并记录日志</li>
     *   <li>无论成功或失败，都返回 {@link PluginResult#CONTINUE}</li>
     *   <li>不会抛出异常影响其他插件的执行</li>
     * </ul>
     * 
     * @param entity 消息实体，包含Telegram消息的所有信息
     * @param context 插件上下文，包含处理过程中的共享数据
     * @return 始终返回 {@link PluginResult#CONTINUE}，确保插件链继续执行
     */
    @Override
    protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
        try {
            // 验证输入
            if (entity == null) {
                log.warn("消息实体为 null，跳过处理");
                return PluginResult.CONTINUE;
            }
            
            // 转换为DTO
            MessageBroadcastDTO dto;
            try {
                dto = convertToDTO(entity);
            } catch (Exception e) {
                log.error("DTO 转换失败: chatId={}, messageId={}", 
                        entity.getChatId(), entity.getMessageId(), e);
                return PluginResult.CONTINUE;
            }
            
            // 构建topic并广播消息
            try {
                String topic = buildTopic(entity.getChatId());
                messagingTemplate.convertAndSend(topic, dto);
                log.info("消息已广播: chatId={}, messageId={}, topic={}", 
                        entity.getChatId(), entity.getMessageId(), topic);
            } catch (Exception e) {
                log.error("广播消息失败: chatId={}, messageId={}, topic={}", 
                        entity.getChatId(), entity.getMessageId(), 
                        buildTopic(entity.getChatId()), e);
            }
            
            return PluginResult.CONTINUE;
            
        } catch (Exception e) {
            log.error("插件处理异常", e);
            return PluginResult.CONTINUE;  // 确保不影响其他插件
        }
    }

    /**
     * 处理频道监控事件
     * 
     * <p>当频道监控配置发生变化时（添加、移除、更新频道），该方法会被Spring事件机制自动调用，
     * 并通过WebSocket广播通知给所有订阅的客户端。</p>
     * 
     * <p><strong>支持的事件类型：</strong></p>
     * <ul>
     *   <li>{@code CHANNEL_ADDED}: 频道被添加到监控列表，广播到 {@code /topic/channel/monitoring/added}</li>
     *   <li>{@code CHANNEL_REMOVED}: 频道从监控列表移除，广播到 {@code /topic/channel/monitoring/removed}</li>
     *   <li>{@code CHANNEL_UPDATED}: 频道监控状态更新，广播到 {@code /topic/channel/monitoring/updated}</li>
     *   <li>{@code RELOAD_ALL}: 重新加载所有频道，广播到 {@code /topic/channel/monitoring/reload}</li>
     * </ul>
     * 
     * <p><strong>通知内容：</strong></p>
     * <ul>
     *   <li>事件类型（eventType）</li>
     *   <li>频道ID（channelId）</li>
     *   <li>监控状态（monitoringStatus）</li>
     *   <li>时间戳（timestamp）</li>
     * </ul>
     * 
     * <p><strong>错误处理：</strong></p>
     * <ul>
     *   <li>捕获所有异常并记录日志</li>
     *   <li>不抛出异常，避免影响事件发布者</li>
     * </ul>
     *
     * @param event 频道监控事件，包含事件类型、频道ID和监控状态
     */
    @EventListener
    public void handleChannelMonitoringEvent(ChannelMonitoringEvent event) {
        try {
            log.debug("收到频道监控事件: eventType={}, channelId={}", 
                    event.getEventType(), event.getChannelId());
            
            // 创建通知DTO
            ChannelMonitoringNotificationDTO notification = ChannelMonitoringNotificationDTO.builder()
                    .eventType(event.getEventType().name())
                    .channelId(event.getChannelId())
                    .monitoringStatus(event.getMonitoringStatus())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            // 构建topic并广播通知
            String topic = buildMonitoringTopic(event.getEventType());
            messagingTemplate.convertAndSend(topic, notification);
            
            log.info("频道监控事件已广播: eventType={}, channelId={}, topic={}", 
                    event.getEventType(), event.getChannelId(), topic);
            
        } catch (Exception e) {
            log.error("处理频道监控事件失败: eventType={}, channelId={}", 
                    event.getEventType(), event.getChannelId(), e);
            // 不抛出异常，避免影响事件发布者
        }
    }

    /**
     * 构建消息广播的topic路径
     * 
     * <p>根据频道ID构建WebSocket topic路径，客户端需要订阅此路径才能接收该频道的消息。</p>
     * 
     * <p><strong>示例：</strong></p>
     * <pre>
     * buildTopic(-1001234567890L) 
     * // 返回: "/topic/channel/real/-1001234567890"
     * </pre>
     *
     * @param chatId 频道ID（Telegram chat ID，通常为负数）
     * @return topic路径，格式为 {@code {topicPrefix}/{chatId}}
     */
    private String buildTopic(Long chatId) {
        return properties.getTopicPrefix() + "/" + chatId;
    }
    
    /**
     * 转换BaseMessageEntity为MessageBroadcastDTO
     * 
     * <p>该方法将内部的消息实体对象转换为适合WebSocket传输的DTO对象。
     * 转换过程包括：</p>
     * <ol>
     *   <li>复制基础字段（messageId, chatId, channelUsername等）</li>
     *   <li>根据消息类型设置特定字段（媒体文件、WebPage、投票等）</li>
     *   <li>转换嵌套对象（MediaFile, WebPageInfo等）</li>
     * </ol>
     * 
     * <p><strong>支持的消息类型：</strong></p>
     * <ul>
     *   <li>{@code TEXT}: 文本消息</li>
     *   <li>{@code PHOTO}: 图片消息（包含photos列表）</li>
     *   <li>{@code VIDEO}: 视频消息（包含video对象）</li>
     *   <li>{@code DOCUMENT}: 文档消息（包含document对象）</li>
     *   <li>{@code AUDIO}: 音频消息（包含audio对象）</li>
     *   <li>{@code VOICE}: 语音消息（包含voice对象）</li>
     *   <li>{@code VIDEO_NOTE}: 视频笔记（包含videoNote对象）</li>
     *   <li>{@code ANIMATION}: 动画消息（包含animation对象）</li>
     *   <li>{@code STICKER}: 贴纸消息（包含sticker对象）</li>
     *   <li>{@code TELEGRAPH}: Telegraph消息（包含webPage对象）</li>
     *   <li>{@code MEDIA_GROUP}: 媒体组消息（包含items列表）</li>
     *   <li>{@code POLL}: 投票消息（包含pollQuestion和pollOptions）</li>
     * </ul>
     * 
     * @param entity 消息实体，不能为null
     * @return 转换后的DTO对象
     * @throws Exception 如果转换过程中发生错误
     */
    private MessageBroadcastDTO convertToDTO(BaseMessageEntity entity) {
        try {
            // 构建基础DTO
            MessageBroadcastDTO.MessageBroadcastDTOBuilder builder = MessageBroadcastDTO.builder()
                    .messageId(entity.getMessageId())
                    .chatId(entity.getChatId())
                    .channelUsername(entity.getChannelUsername())
                    .channelTitle(entity.getChannelTitle())
                    .date(entity.getDate())
                    .contentType(entity.getType().name())
                    .views(entity.getViews())
                    .forwards(entity.getForwards());
            
            // 根据消息类型设置特定字段
            switch (entity) {
                case TextMessageEntity text -> {
                    builder.textContent(text.getTextContent());
                }
                case PhotoMessageEntity photo -> {
                    builder.textContent(photo.getCaption());
                    builder.photos(convertMediaFiles(photo.getPhotos()));
                }
                case VideoMessageEntity video -> {
                    builder.textContent(video.getCaption());
                    builder.video(convertMediaFile(video.getVideo()));
                }
                case DocumentMessageEntity doc -> {
                    builder.textContent(doc.getCaption());
                    builder.document(convertMediaFile(doc.getDocument()));
                }
                case AudioMessageEntity audio -> {
                    builder.textContent(audio.getCaption());
                    builder.audio(convertMediaFile(audio.getAudio()));
                }
                case VoiceMessageEntity voice -> {
                    builder.textContent(voice.getCaption());
                    builder.voice(convertMediaFile(voice.getVoice()));
                }
                case VideoNoteMessageEntity videoNote -> {
                    builder.videoNote(convertMediaFile(videoNote.getVideoNote()));
                }
                case AnimationMessageEntity animation -> {
                    builder.textContent(animation.getCaption());
                    builder.animation(convertMediaFile(animation.getAnimation()));
                }
                case StickerMessageEntity sticker -> {
                    builder.sticker(convertMediaFile(sticker.getSticker()));
                }
                case TelegraphMessageEntity telegraph -> {
                    builder.textContent(telegraph.getTextContent());
                    builder.webPage(convertWebPage(telegraph.getWebPage()));
                }
                case MediaGroupMessageEntity mediaGroup -> {
                    builder.mediaAlbumId(entity.getMediaAlbumId());
                    builder.isMediaGroup(true);
                    builder.itemCount(mediaGroup.getItems() != null ? mediaGroup.getItems().size() : 0);
                    builder.items(mediaGroup.getItems() != null ?
                            mediaGroup.getItems().stream()
                                    .map(this::convertToDTO)
                                    .collect(Collectors.toList()) : null);
                }
                case PollMessageEntity poll -> {
                    builder.pollQuestion(poll.getQuestion());
                    builder.pollOptions(poll.getOptions());
                }
                default -> {
                    // 其他类型使用基础字段
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("DTO 转换失败: chatId={}, messageId={}", 
                    entity.getChatId(), entity.getMessageId(), e);
            throw e;
        }
    }
    
    /**
     * 转换单个MediaFile为MediaFileDTO
     * 
     * <p>将内部的MediaFile对象转换为适合WebSocket传输的MediaFileDTO对象。
     * 包含文件ID、大小、MIME类型、尺寸、时长等信息。</p>
     * 
     * @param file 媒体文件对象，可以为null
     * @return 转换后的MediaFileDTO对象，如果输入为null则返回null
     */
    private MediaFileDTO convertMediaFile(MediaFile file) {
        if (file == null) {
            return null;
        }
        
        return MediaFileDTO.builder()
                .fileId(file.getFileId() != null ? file.getFileId().toString() : null)
                .fileUniqueId(file.getFileUniqueId())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileName(file.getFileName())
                .width(file.getWidth())
                .height(file.getHeight())
                .duration(file.getDuration())
                .build();
    }
    
    /**
     * 转换MediaFile列表为MediaFileDTO列表
     * 
     * <p>批量转换媒体文件列表，主要用于图片消息（一条消息可能包含多张不同尺寸的图片）。</p>
     * 
     * @param files 媒体文件列表，可以为null
     * @return 转换后的MediaFileDTO列表，如果输入为null则返回null
     */
    private List<MediaFileDTO> convertMediaFiles(List<MediaFile> files) {
        if (files == null) {
            return null;
        }
        
        return files.stream()
                .map(this::convertMediaFile)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换WebPageInfo为WebPageDTO
     * 
     * <p>将Telegraph消息中的网页预览信息转换为DTO对象。
     * 包含URL、标题、描述、作者、即时预览等信息。</p>
     * 
     * @param webPage WebPage信息对象，可以为null
     * @return 转换后的WebPageDTO对象，如果输入为null则返回null
     */
    private WebPageDTO convertWebPage(WebPageInfo webPage) {
        if (webPage == null) {
            return null;
        }
        
        return WebPageDTO.builder()
                .url(webPage.getUrl())
                .displayUrl(webPage.getUrl())  // displayUrl使用url
                .siteName(webPage.getSiteName())
                .title(webPage.getTitle())
                .description(webPage.getDescription())
                .author(webPage.getAuthor())
                .hasInstantView(webPage.getHasInstantView())
                .instantViewVersion(webPage.getInstantViewVersion() != null ? 
                        webPage.getInstantViewVersion().toString() : null)
                .build();
    }


    /**
     * 构建频道监控事件的topic路径
     * 
     * <p>根据事件类型构建WebSocket topic路径，客户端需要订阅此路径才能接收监控事件通知。</p>
     * 
     * <p><strong>事件类型映射：</strong></p>
     * <ul>
     *   <li>{@code CHANNEL_ADDED} → {@code /topic/channel/monitoring/added}</li>
     *   <li>{@code CHANNEL_REMOVED} → {@code /topic/channel/monitoring/removed}</li>
     *   <li>{@code CHANNEL_UPDATED} → {@code /topic/channel/monitoring/updated}</li>
     *   <li>{@code RELOAD_ALL} → {@code /topic/channel/monitoring/reload}</li>
     * </ul>
     *
     * @param eventType 事件类型枚举
     * @return topic路径，格式为 {@code {monitoringTopicPrefix}/{eventType}}
     */
    private String buildMonitoringTopic(org.xlyo.cocomonyab.event.ChannelMonitoringEvent.EventType eventType) {
        String suffix = switch (eventType) {
            case CHANNEL_ADDED -> "added";
            case CHANNEL_REMOVED -> "removed";
            case CHANNEL_UPDATED -> "updated";
            case RELOAD_ALL -> "reload";
        };
        return properties.getMonitoringTopicPrefix() + "/" + suffix;
    }

}
