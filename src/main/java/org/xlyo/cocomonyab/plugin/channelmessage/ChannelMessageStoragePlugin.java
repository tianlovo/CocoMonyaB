package org.xlyo.cocomonyab.plugin.channelmessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.plugin.AbstractMessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 频道消息存储插件
 * 将监控频道的消息存储到 channel_messages 数据库集合中
 * 
 * <p>功能特性：</p>
 * <ul>
 *   <li>自动存储所有类型的频道消息</li>
 *   <li>支持媒体组消息的批量存储</li>
 *   <li>支持Telegraph文章信息存储</li>
 *   <li>自动去重，避免重复存储</li>
 *   <li>高优先级执行，确保消息及时保存</li>
 * </ul>
 * 
 * @author CocoMonya Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelMessageStoragePlugin extends AbstractMessagePlugin {
    
    /**
     * 插件优先级：1000（高优先级，比所有现有插件都高）
     * 确保消息在其他插件处理之前先被保存到数据库
     */
    private static final int PRIORITY = 1000;
    
    private final ChannelMessageRepository channelMessageRepository;
    private final ChannelMessageStorageProperties properties;
    
    @Override
    public String getName() {
        return "ChannelMessageStoragePlugin";
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    @Override
    protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
        try {
            // 处理媒体组消息
            if (entity instanceof MediaGroupMessageEntity mediaGroup) {
                handleMediaGroup(mediaGroup);
            } else {
                // 处理单条消息
                handleSingleMessage(entity);
            }
            
            return PluginResult.CONTINUE;
        } catch (Exception e) {
            log.error("存储消息失败: chatId={}, messageId={}, error={}", 
                entity.getChatId(), entity.getMessageId(), e.getMessage(), e);
            // 即使存储失败也继续执行其他插件
            return PluginResult.CONTINUE;
        }
    }
    
    /**
     * 处理单条消息
     */
    private void handleSingleMessage(BaseMessageEntity entity) {
        // 检查消息是否已存在
        if (channelMessageRepository.existsByChatIdAndMessageId(
                entity.getChatId(), entity.getMessageId())) {
            log.debug("消息已存在，跳过存储: chatId={}, messageId={}", 
                entity.getChatId(), entity.getMessageId());
            return;
        }
        
        // 转换并保存消息
        ChannelMessage channelMessage = convertToChannelMessage(entity);
        channelMessageRepository.save(channelMessage);
        
        log.info("✅ 消息已存储: chatId={}, messageId={}, type={}", 
            entity.getChatId(), entity.getMessageId(), entity.getType());
    }
    
    /**
     * 处理媒体组消息
     */
    private void handleMediaGroup(MediaGroupMessageEntity mediaGroup) {
        List<BaseMessageEntity> items = mediaGroup.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("媒体组为空: chatId={}, mediaAlbumId={}", 
                mediaGroup.getChatId(), mediaGroup.getMediaAlbumId());
            return;
        }
        
        // 检查媒体组是否已存在
        long existingCount = channelMessageRepository.countByChatIdAndMediaAlbumId(
            mediaGroup.getChatId(), mediaGroup.getMediaAlbumId());
        
        if (existingCount > 0) {
            log.debug("媒体组已存在，跳过存储: chatId={}, mediaAlbumId={}, count={}", 
                mediaGroup.getChatId(), mediaGroup.getMediaAlbumId(), existingCount);
            return;
        }
        
        // 批量转换并保存媒体组中的所有消息
        List<ChannelMessage> messages = items.stream()
            .map(this::convertToChannelMessage)
            .collect(Collectors.toList());
        
        channelMessageRepository.saveAll(messages);
        
        log.info("✅ 媒体组已存储: chatId={}, mediaAlbumId={}, count={}", 
            mediaGroup.getChatId(), mediaGroup.getMediaAlbumId(), messages.size());
    }
    
    /**
     * 将BaseMessageEntity转换为ChannelMessage
     */
    private ChannelMessage convertToChannelMessage(BaseMessageEntity entity) {
        ChannelMessage message = new ChannelMessage();
        
        // 基础字段
        message.setMessageId(entity.getMessageId());
        message.setChatId(entity.getChatId());
        message.setChannelUsername(entity.getChannelUsername());
        message.setChannelTitle(entity.getChannelTitle());
        message.setDate(entity.getDate());
        message.setEditDate(entity.getEditDate());
        message.setContentType(entity.getType().name());
        
        // 媒体组字段
        message.setMediaAlbumId(entity.getMediaAlbumId());
        message.setIsMediaGroup(entity.getIsMediaGroup());
        message.setMediaGroupItemCount(entity.getMediaGroupItemCount());
        message.setMediaGroupMessageIds(entity.getMediaGroupMessageIds());
        
        // 互动数据
        message.setViews(entity.getViews());
        message.setForwards(entity.getForwards());
        
        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        message.setCreateTime(now);
        message.setUpdateTime(now);
        
        // 根据消息类型提取内容
        extractContent(entity, message);
        
        return message;
    }
    
    /**
     * 提取消息内容
     */
    private void extractContent(BaseMessageEntity entity, ChannelMessage message) {
        switch (entity) {
            case TextMessageEntity text -> {
                message.setTextContent(text.getTextContent());
            }
            case PhotoMessageEntity photo -> {
                message.setTextContent(photo.getCaption());
                message.setMediaFiles(convertMediaFiles(photo.getPhotos()));
            }
            case VideoMessageEntity video -> {
                message.setTextContent(video.getCaption());
                if (video.getVideo() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(video.getVideo())));
                }
            }
            case DocumentMessageEntity doc -> {
                message.setTextContent(doc.getCaption());
                if (doc.getDocument() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(doc.getDocument())));
                }
            }
            case AudioMessageEntity audio -> {
                message.setTextContent(audio.getCaption());
                if (audio.getAudio() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(audio.getAudio())));
                }
            }
            case VoiceMessageEntity voice -> {
                message.setTextContent(voice.getCaption());
                if (voice.getVoice() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(voice.getVoice())));
                }
            }
            case VideoNoteMessageEntity videoNote -> {
                if (videoNote.getVideoNote() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(videoNote.getVideoNote())));
                }
            }
            case AnimationMessageEntity animation -> {
                message.setTextContent(animation.getCaption());
                if (animation.getAnimation() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(animation.getAnimation())));
                }
            }
            case StickerMessageEntity sticker -> {
                if (sticker.getSticker() != null) {
                    message.setMediaFiles(List.of(convertMediaFile(sticker.getSticker())));
                }
            }
            case PollMessageEntity poll -> {
                message.setTextContent(poll.getQuestion());
            }
            case TelegraphMessageEntity telegraph -> {
                message.setTextContent(telegraph.getTextContent());
                message.setWebPage(convertWebPage(telegraph.getWebPage()));
            }
            default -> {
                // 其他类型暂不处理
            }
        }
    }
    
    /**
     * 转换媒体文件列表
     */
    private List<ChannelMessage.MediaFile> convertMediaFiles(List<MediaFile> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        return files.stream()
            .map(this::convertMediaFile)
            .collect(Collectors.toList());
    }
    
    /**
     * 转换单个媒体文件
     */
    private ChannelMessage.MediaFile convertMediaFile(MediaFile file) {
        ChannelMessage.MediaFile mediaFile = new ChannelMessage.MediaFile();
        // 将Integer fileId转换为String
        mediaFile.setFileId(file.getFileId() != null ? file.getFileId().toString() : null);
        // 根据mimeType推断文件类型
        mediaFile.setFileType(inferFileType(file.getMimeType()));
        mediaFile.setFileSize(file.getFileSize());
        mediaFile.setMimeType(file.getMimeType());
        mediaFile.setLocalPath(file.getFilePath());
        mediaFile.setDownloaded(file.getFilePath() != null && !file.getFilePath().isEmpty());
        return mediaFile;
    }
    
    /**
     * 根据MIME类型推断文件类型
     */
    private String inferFileType(String mimeType) {
        if (mimeType == null) {
            return "unknown";
        }
        if (mimeType.startsWith("image/")) {
            return "photo";
        } else if (mimeType.startsWith("video/")) {
            return "video";
        } else if (mimeType.startsWith("audio/")) {
            return "audio";
        } else {
            return "document";
        }
    }
    
    /**
     * 转换WebPage信息
     */
    private ChannelMessage.WebPageInfo convertWebPage(WebPageInfo webPage) {
        if (webPage == null) {
            return null;
        }
        
        ChannelMessage.WebPageInfo info = new ChannelMessage.WebPageInfo();
        info.setUrl(webPage.getUrl());
        info.setDisplayUrl(webPage.getUrl()); // 使用url作为displayUrl
        info.setType("article"); // Telegraph通常是文章类型
        info.setSiteName(webPage.getSiteName());
        info.setTitle(webPage.getTitle());
        info.setDescription(webPage.getDescription());
        info.setAuthor(webPage.getAuthor());
        info.setDuration(null); // WebPageInfo中没有duration字段
        info.setHasInstantView(webPage.getHasInstantView());
        // 将Integer转换为String
        info.setInstantViewVersion(webPage.getInstantViewVersion() != null ? 
            webPage.getInstantViewVersion().toString() : null);
        return info;
    }
}
