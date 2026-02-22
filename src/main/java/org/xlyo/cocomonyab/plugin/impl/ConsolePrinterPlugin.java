package org.xlyo.cocomonyab.plugin.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.plugin.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 控制台打印插件
 * 将消息输出到控制台（默认插件）
 */
@Slf4j
@Component
public class ConsolePrinterPlugin extends AbstractMessagePlugin {
    
    private static final int PRIORITY = 0;  // 最低优先级
    
    @Override
    public String getName() {
        return "ConsolePrinterPlugin";
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
        switch (entity.getType()) {
            case MEDIA_GROUP -> printMediaGroup((MediaGroupMessageEntity) entity);
            case TELEGRAPH -> printTelegraph((TelegraphMessageEntity) entity);
            default -> printSingleMessage(entity);
        }
        
        return PluginResult.CONTINUE;
    }
    
    /**
     * 打印单条消息
     */
    private void printSingleMessage(BaseMessageEntity entity) {
        log.info("━".repeat(80));
        log.info("📨 收到新消息");
        log.info("━".repeat(80));
        log.info("频道: {} (@{})", entity.getChannelTitle(), entity.getChannelUsername());
        log.info("消息ID: {}", entity.getMessageId());
        log.info("频道ID: {}", entity.getChatId());
        log.info("类型: {}", entity.getType().getDescription());
        
        LocalDateTime messageTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(entity.getDate()), 
            ZoneId.systemDefault()
        );
        log.info("时间: {}", messageTime);
        
        // 打印消息内容
        printMessageContent(entity);
        
        // 打印互动信息
        if (entity.getViews() != null || entity.getForwards() != null) {
            log.info("━".repeat(80));
            log.info("📊 互动: 浏览 {} 次, 转发 {} 次", 
                entity.getViews() != null ? entity.getViews() : 0,
                entity.getForwards() != null ? entity.getForwards() : 0);
        }
        
        log.info("━".repeat(80));
    }
    
    /**
     * 打印媒体组
     */
    private void printMediaGroup(MediaGroupMessageEntity entity) {
        log.info("━".repeat(80));
        log.info("📨 收到媒体组消息");
        log.info("━".repeat(80));
        log.info("频道: {} (@{})", entity.getChannelTitle(), entity.getChannelUsername());
        log.info("媒体组ID: {}", entity.getMediaAlbumId());
        log.info("频道ID: {}", entity.getChatId());
        log.info("消息数量: {} 条", entity.getItems() != null ? entity.getItems().size() : 0);
        
        LocalDateTime messageTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(entity.getDate()), 
            ZoneId.systemDefault()
        );
        log.info("时间: {}", messageTime);
        
        log.info("━".repeat(80));
        log.info("📎 媒体组内容:");
        
        if (entity.getItems() != null) {
            for (int i = 0; i < entity.getItems().size(); i++) {
                BaseMessageEntity item = entity.getItems().get(i);
                log.info("  [{}] 消息ID: {}, 类型: {}", 
                    i + 1, item.getMessageId(), item.getType().getDescription());
                printMessageContentIndented(item);
            }
        }
        
        // 打印互动信息（使用第一条消息的数据）
        if (entity.getViews() != null || entity.getForwards() != null) {
            log.info("━".repeat(80));
            log.info("📊 互动: 浏览 {} 次, 转发 {} 次", 
                entity.getViews() != null ? entity.getViews() : 0,
                entity.getForwards() != null ? entity.getForwards() : 0);
        }
        
        log.info("━".repeat(80));
    }
    
    /**
     * 打印Telegraph消息
     */
    private void printTelegraph(TelegraphMessageEntity entity) {
        log.info("━".repeat(80));
        log.info("📨 收到新消息");
        log.info("━".repeat(80));
        log.info("频道: {} (@{})", entity.getChannelTitle(), entity.getChannelUsername());
        log.info("消息ID: {}", entity.getMessageId());
        log.info("频道ID: {}", entity.getChatId());
        log.info("类型: {}", entity.getType().getDescription());
        
        LocalDateTime messageTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(entity.getDate()), 
            ZoneId.systemDefault()
        );
        log.info("时间: {}", messageTime);
        
        // 打印文本内容
        if (entity.getTextContent() != null && !entity.getTextContent().isEmpty()) {
            log.info("内容: {}", entity.getTextContent());
        }
        
        // 打印WebPage信息
        if (entity.getWebPage() != null) {
            WebPageInfo webPage = entity.getWebPage();
            log.info("━".repeat(80));
            log.info("🌐 WebPage 信息:");
            
            if (Boolean.TRUE.equals(webPage.getHasInstantView())) {
                log.info("  📰 Telegraph 文章");
            }
            
            if (webPage.getTitle() != null && !webPage.getTitle().isEmpty()) {
                log.info("  标题: {}", webPage.getTitle());
            }
            
            if (webPage.getSiteName() != null && !webPage.getSiteName().isEmpty()) {
                log.info("  网站: {}", webPage.getSiteName());
            }
            
            if (webPage.getAuthor() != null && !webPage.getAuthor().isEmpty()) {
                log.info("  作者: {}", webPage.getAuthor());
            }
            
            if (webPage.getDescription() != null && !webPage.getDescription().isEmpty()) {
                String desc = webPage.getDescription();
                if (desc.length() > 100) {
                    desc = desc.substring(0, 100) + "...";
                }
                log.info("  描述: {}", desc);
            }
            
            if (webPage.getUrl() != null && !webPage.getUrl().isEmpty()) {
                log.info("  链接: {}", webPage.getUrl());
            }
            
            if (Boolean.TRUE.equals(webPage.getHasInstantView())) {
                log.info("  即时预览: 可用 (版本: {})", webPage.getInstantViewVersion());
            }
        }
        
        // 打印互动信息
        if (entity.getViews() != null || entity.getForwards() != null) {
            log.info("━".repeat(80));
            log.info("📊 互动: 浏览 {} 次, 转发 {} 次", 
                entity.getViews() != null ? entity.getViews() : 0,
                entity.getForwards() != null ? entity.getForwards() : 0);
        }
        
        log.info("━".repeat(80));
    }
    
    /**
     * 打印消息内容（根据类型）
     */
    private void printMessageContent(BaseMessageEntity entity) {
        switch (entity) {
            case TextMessageEntity text -> {
                if (text.getTextContent() != null && !text.getTextContent().isEmpty()) {
                    log.info("内容: {}", text.getTextContent());
                }
            }
            case PhotoMessageEntity photo -> {
                if (photo.getCaption() != null && !photo.getCaption().isEmpty()) {
                    log.info("内容: {}", photo.getCaption());
                }
                if (photo.getPhotos() != null && !photo.getPhotos().isEmpty()) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: {} 个", photo.getPhotos().size());
                    photo.getPhotos().forEach(file -> {
                        log.info("  - 类型: 图片, 大小: {} bytes, 文件ID: {}", 
                            file.getFileSize(), 
                            file.getFileId());
                    });
                }
            }
            case VideoMessageEntity video -> {
                if (video.getCaption() != null && !video.getCaption().isEmpty()) {
                    log.info("内容: {}", video.getCaption());
                }
                if (video.getVideo() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 视频, 大小: {} bytes, 文件ID: {}", 
                        video.getVideo().getFileSize(), 
                        video.getVideo().getFileId());
                }
            }
            case DocumentMessageEntity doc -> {
                if (doc.getCaption() != null && !doc.getCaption().isEmpty()) {
                    log.info("内容: {}", doc.getCaption());
                }
                if (doc.getDocument() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 文档, 大小: {} bytes, 文件ID: {}", 
                        doc.getDocument().getFileSize(), 
                        doc.getDocument().getFileId());
                }
            }
            case AudioMessageEntity audio -> {
                if (audio.getCaption() != null && !audio.getCaption().isEmpty()) {
                    log.info("内容: {}", audio.getCaption());
                }
                if (audio.getAudio() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 音频, 大小: {} bytes, 文件ID: {}", 
                        audio.getAudio().getFileSize(), 
                        audio.getAudio().getFileId());
                }
            }
            case VoiceMessageEntity voice -> {
                if (voice.getCaption() != null && !voice.getCaption().isEmpty()) {
                    log.info("内容: {}", voice.getCaption());
                }
                if (voice.getVoice() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 语音, 大小: {} bytes, 文件ID: {}", 
                        voice.getVoice().getFileSize(), 
                        voice.getVoice().getFileId());
                }
            }
            case VideoNoteMessageEntity videoNote -> {
                if (videoNote.getVideoNote() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 视频笔记, 大小: {} bytes, 文件ID: {}", 
                        videoNote.getVideoNote().getFileSize(), 
                        videoNote.getVideoNote().getFileId());
                }
            }
            case AnimationMessageEntity animation -> {
                if (animation.getCaption() != null && !animation.getCaption().isEmpty()) {
                    log.info("内容: {}", animation.getCaption());
                }
                if (animation.getAnimation() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 动画, 大小: {} bytes, 文件ID: {}", 
                        animation.getAnimation().getFileSize(), 
                        animation.getAnimation().getFileId());
                }
            }
            case StickerMessageEntity sticker -> {
                if (sticker.getSticker() != null) {
                    log.info("━".repeat(80));
                    log.info("📎 媒体文件: 1 个");
                    log.info("  - 类型: 贴纸, 大小: {} bytes, 文件ID: {}", 
                        sticker.getSticker().getFileSize(), 
                        sticker.getSticker().getFileId());
                }
            }
            case PollMessageEntity poll -> {
                if (poll.getQuestion() != null && !poll.getQuestion().isEmpty()) {
                    log.info("投票问题: {}", poll.getQuestion());
                }
            }
            default -> {
                // 其他类型消息，不输出额外内容
            }
        }
    }
    
    /**
     * 打印消息内容（带缩进，用于媒体组）
     */
    private void printMessageContentIndented(BaseMessageEntity entity) {
        switch (entity) {
            case TextMessageEntity text -> {
                if (text.getTextContent() != null && !text.getTextContent().isEmpty()) {
                    String content = text.getTextContent();
                    if (content.length() > 50) {
                        content = content.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", content);
                }
            }
            case PhotoMessageEntity photo -> {
                if (photo.getCaption() != null && !photo.getCaption().isEmpty()) {
                    String caption = photo.getCaption();
                    if (caption.length() > 50) {
                        caption = caption.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", caption);
                }
                if (photo.getPhotos() != null && !photo.getPhotos().isEmpty()) {
                    photo.getPhotos().forEach(file -> {
                        log.info("      - 文件类型: 图片, 大小: {} bytes", file.getFileSize());
                    });
                }
            }
            case VideoMessageEntity video -> {
                if (video.getCaption() != null && !video.getCaption().isEmpty()) {
                    String caption = video.getCaption();
                    if (caption.length() > 50) {
                        caption = caption.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", caption);
                }
                if (video.getVideo() != null) {
                    log.info("      - 文件类型: 视频, 大小: {} bytes", video.getVideo().getFileSize());
                }
            }
            case DocumentMessageEntity doc -> {
                if (doc.getCaption() != null && !doc.getCaption().isEmpty()) {
                    String caption = doc.getCaption();
                    if (caption.length() > 50) {
                        caption = caption.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", caption);
                }
                if (doc.getDocument() != null) {
                    log.info("      - 文件类型: 文档, 大小: {} bytes", doc.getDocument().getFileSize());
                }
            }
            case AudioMessageEntity audio -> {
                if (audio.getCaption() != null && !audio.getCaption().isEmpty()) {
                    String caption = audio.getCaption();
                    if (caption.length() > 50) {
                        caption = caption.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", caption);
                }
                if (audio.getAudio() != null) {
                    log.info("      - 文件类型: 音频, 大小: {} bytes", audio.getAudio().getFileSize());
                }
            }
            case VoiceMessageEntity voice -> {
                if (voice.getCaption() != null && !voice.getCaption().isEmpty()) {
                    String caption = voice.getCaption();
                    if (caption.length() > 50) {
                        caption = caption.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", caption);
                }
                if (voice.getVoice() != null) {
                    log.info("      - 文件类型: 语音, 大小: {} bytes", voice.getVoice().getFileSize());
                }
            }
            case VideoNoteMessageEntity videoNote -> {
                if (videoNote.getVideoNote() != null) {
                    log.info("      - 文件类型: 视频笔记, 大小: {} bytes", videoNote.getVideoNote().getFileSize());
                }
            }
            case AnimationMessageEntity animation -> {
                if (animation.getCaption() != null && !animation.getCaption().isEmpty()) {
                    String caption = animation.getCaption();
                    if (caption.length() > 50) {
                        caption = caption.substring(0, 50) + "...";
                    }
                    log.info("      说明: {}", caption);
                }
                if (animation.getAnimation() != null) {
                    log.info("      - 文件类型: 动画, 大小: {} bytes", animation.getAnimation().getFileSize());
                }
            }
            case StickerMessageEntity sticker -> {
                if (sticker.getSticker() != null) {
                    log.info("      - 文件类型: 贴纸, 大小: {} bytes", sticker.getSticker().getFileSize());
                }
            }
            case PollMessageEntity poll -> {
                if (poll.getQuestion() != null && !poll.getQuestion().isEmpty()) {
                    log.info("      投票问题: {}", poll.getQuestion());
                }
            }
            default -> {
                // 其他类型消息，不输出额外内容
            }
        }
    }
}
