package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 频道监控服务
 * 负责管理监控频道列表、处理新消息、消息持久化
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelMonitorService {
    
    private final ChannelRepository channelRepository;
    private final ChannelMessageRepository messageRepository;
    
    // 缓存监控中的频道ID（提高性能）
    private final Set<Long> monitoringChannels = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 初始化：从数据库加载启用监控的频道
     */
    @PostConstruct
    public void initialize() {
        try {
            List<Channel> channels = channelRepository.findByMonitoringStatus(true);
            
            channels.forEach(channel -> {
                monitoringChannels.add(channel.getChannelId());
                log.info("✓ 已加载监控频道: {} (@{}) [ID: {}]", 
                    channel.getChannelTitle(), 
                    channel.getChannelUsername(),
                    channel.getChannelId());
            });
            
            log.info("=".repeat(60));
            log.info("频道监控服务已启动，共监控 {} 个频道", monitoringChannels.size());
            log.info("=".repeat(60));
            
        } catch (Exception e) {
            log.error("加载监控频道列表失败", e);
        }
    }
    
    /**
     * 检查频道是否在监控列表中
     */
    public boolean isMonitoring(long chatId) {
        return monitoringChannels.contains(chatId);
    }
    
    /**
     * 处理新消息
     */
    public void handleNewMessage(TdApi.Message message) {
        try {
            // 1. 检查消息是否已存在（去重）
            if (messageRepository.existsByChatIdAndMessageId(message.chatId, message.id)) {
                log.debug("消息已存在，跳过: chatId={}, messageId={}", message.chatId, message.id);
                return;
            }
            
            // 2. 解析消息内容
            ChannelMessage channelMessage = parseMessage(message);
            
            // 3. 保存到数据库
            ChannelMessage saved = messageRepository.save(channelMessage);
            
            // 4. 输出到控制台
            printMessageToConsole(saved, message);
            
        } catch (Exception e) {
            log.error("处理频道消息失败: chatId={}, messageId={}", message.chatId, message.id, e);
        }
    }
    
    /**
     * 解析消息
     */
    private ChannelMessage parseMessage(TdApi.Message message) {
        ChannelMessage channelMessage = new ChannelMessage();
        channelMessage.setMessageId(message.id);
        channelMessage.setChatId(message.chatId);
        channelMessage.setDate(message.date);
        channelMessage.setEditDate(message.editDate);
        channelMessage.setStatus(ChannelMessage.MessageStatus.PENDING);
        channelMessage.setCreateTime(LocalDateTime.now());
        channelMessage.setUpdateTime(LocalDateTime.now());
        
        // 从缓存获取频道信息
        Optional<Channel> channelOpt = channelRepository.findByChannelId(message.chatId);
        channelOpt.ifPresent(channel -> {
            channelMessage.setChannelTitle(channel.getChannelTitle());
            channelMessage.setChannelUsername(channel.getChannelUsername());
        });
        
        // 解析消息内容
        parseMessageContent(message.content, channelMessage);
        
        // 解析互动信息
        if (message.interactionInfo != null) {
            channelMessage.setViews(message.interactionInfo.viewCount);
            channelMessage.setForwards(message.interactionInfo.forwardCount);
        }
        
        return channelMessage;
    }
    
    /**
     * 解析消息内容
     */
    private void parseMessageContent(TdApi.MessageContent content, ChannelMessage message) {
        switch (content) {
            case TdApi.MessageText text -> {
                message.setContentType("text");
                message.setTextContent(text.text.text);
                
                // 检查是否包含 WebPage（Telegraph 文章）
                if (text.webPage != null) {
                    message.setWebPage(parseWebPage(text.webPage));
                    
                    // 如果有即时预览，标记为 telegraph 类型
                    if (text.webPage.instantViewVersion > 0) {
                        message.setContentType("telegraph");
                    }
                }
            }
            
            case TdApi.MessagePhoto photo -> {
                message.setContentType("photo");
                message.setTextContent(photo.caption.text);
                message.setMediaFiles(parsePhotoSizes(photo.photo));
            }
            
            case TdApi.MessageVideo video -> {
                message.setContentType("video");
                message.setTextContent(video.caption.text);
                ChannelMessage.MediaFile file = parseFile(video.video.video);
                file.setFileType("video");
                file.setMimeType(video.video.mimeType);
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessageDocument document -> {
                message.setContentType("document");
                message.setTextContent(document.caption.text);
                ChannelMessage.MediaFile file = parseFile(document.document.document);
                file.setFileType("document");
                file.setMimeType(document.document.mimeType);
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessageAudio audio -> {
                message.setContentType("audio");
                message.setTextContent(audio.caption.text);
                ChannelMessage.MediaFile file = parseFile(audio.audio.audio);
                file.setFileType("audio");
                file.setMimeType(audio.audio.mimeType);
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessageVoiceNote voice -> {
                message.setContentType("voice");
                ChannelMessage.MediaFile file = parseFile(voice.voiceNote.voice);
                file.setFileType("voice");
                file.setMimeType(voice.voiceNote.mimeType);
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessageVideoNote videoNote -> {
                message.setContentType("video_note");
                ChannelMessage.MediaFile file = parseFile(videoNote.videoNote.video);
                file.setFileType("video_note");
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessageAnimation animation -> {
                message.setContentType("animation");
                message.setTextContent(animation.caption.text);
                ChannelMessage.MediaFile file = parseFile(animation.animation.animation);
                file.setFileType("animation");
                file.setMimeType(animation.animation.mimeType);
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessageSticker sticker -> {
                message.setContentType("sticker");
                ChannelMessage.MediaFile file = parseFile(sticker.sticker.sticker);
                file.setFileType("sticker");
                message.setMediaFiles(List.of(file));
            }
            
            case TdApi.MessagePoll poll -> {
                message.setContentType("poll");
                // Poll question 是 String 类型
                message.setTextContent(poll.poll.question);
            }
            
            default -> {
                message.setContentType("other");
                message.setTextContent(content.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * 解析图片尺寸
     */
    private List<ChannelMessage.MediaFile> parsePhotoSizes(TdApi.Photo photo) {
        return Arrays.stream(photo.sizes)
            .map(size -> {
                ChannelMessage.MediaFile file = new ChannelMessage.MediaFile();
                file.setFileId(String.valueOf(size.photo.id));
                file.setFileType("photo");
                file.setFileSize((long) size.photo.size);
                file.setDownloaded(size.photo.local.isDownloadingCompleted);
                file.setLocalPath(size.photo.local.path);
                return file;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 解析文件信息
     */
    private ChannelMessage.MediaFile parseFile(TdApi.File file) {
        ChannelMessage.MediaFile mediaFile = new ChannelMessage.MediaFile();
        mediaFile.setFileId(String.valueOf(file.id));
        mediaFile.setFileSize((long) file.size);
        mediaFile.setDownloaded(file.local.isDownloadingCompleted);
        mediaFile.setLocalPath(file.local.path);
        return mediaFile;
    }
    
    /**
     * 解析 WebPage 信息（Telegraph 文章）
     */
    private ChannelMessage.WebPageInfo parseWebPage(TdApi.WebPage webPage) {
        ChannelMessage.WebPageInfo info = new ChannelMessage.WebPageInfo();
        
        info.setUrl(webPage.url);
        info.setDisplayUrl(webPage.displayUrl);
        info.setType(webPage.type);
        info.setSiteName(webPage.siteName);
        info.setTitle(webPage.title);
        
        // 描述可能是 FormattedText 对象
        if (webPage.description != null && webPage.description.text != null) {
            info.setDescription(webPage.description.text);
        }
        
        info.setAuthor(webPage.author);
        info.setDuration(webPage.duration);
        
        // 检查是否有即时预览（Telegraph）
        if (webPage.instantViewVersion > 0) {
            info.setHasInstantView(true);
            info.setInstantViewVersion(String.valueOf(webPage.instantViewVersion));
        } else {
            info.setHasInstantView(false);
        }
        
        return info;
    }
    
    /**
     * 输出消息到控制台
     */
    private void printMessageToConsole(ChannelMessage saved, TdApi.Message original) {
        log.info("━".repeat(80));
        log.info("📨 收到新消息");
        log.info("━".repeat(80));
        log.info("频道: {} (@{})", saved.getChannelTitle(), saved.getChannelUsername());
        log.info("消息ID: {}", saved.getMessageId());
        log.info("频道ID: {}", saved.getChatId());
        log.info("类型: {}", saved.getContentType());
        
        // 格式化时间
        LocalDateTime messageTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(saved.getDate()), 
            ZoneId.systemDefault()
        );
        log.info("时间: {}", messageTime);
        
        // 输出文本内容
        if (saved.getTextContent() != null && !saved.getTextContent().isEmpty()) {
            log.info("内容: {}", saved.getTextContent());
        }
        
        // 输出 Telegraph/WebPage 信息
        if (saved.getWebPage() != null) {
            ChannelMessage.WebPageInfo webPage = saved.getWebPage();
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
        
        // 输出媒体文件信息
        if (saved.getMediaFiles() != null && !saved.getMediaFiles().isEmpty()) {
            log.info("━".repeat(80));
            log.info("📎 媒体文件: {} 个", saved.getMediaFiles().size());
            saved.getMediaFiles().forEach(file -> {
                log.info("  - 类型: {}, 大小: {} bytes, 文件ID: {}", 
                    file.getFileType(), 
                    file.getFileSize(), 
                    file.getFileId());
            });
        }
        
        // 输出互动信息
        if (saved.getViews() != null || saved.getForwards() != null) {
            log.info("━".repeat(80));
            log.info("📊 互动: 浏览 {} 次, 转发 {} 次", 
                saved.getViews() != null ? saved.getViews() : 0,
                saved.getForwards() != null ? saved.getForwards() : 0);
        }
        
        log.info("━".repeat(80));
        log.info("💾 数据库ID: {}", saved.getId());
        log.info("━".repeat(80));
    }
    
    /**
     * 启动监控
     */
    public void startMonitoring(long chatId) {
        monitoringChannels.add(chatId);
        log.info("✓ 已启动频道监控: chatId={}", chatId);
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring(long chatId) {
        monitoringChannels.remove(chatId);
        log.info("✓ 已停止频道监控: chatId={}", chatId);
    }
    
    /**
     * 重新加载监控列表
     */
    public void reloadMonitoringChannels() {
        monitoringChannels.clear();
        initialize();
    }
    
    /**
     * 获取监控频道数量
     */
    public int getMonitoringChannelCount() {
        return monitoringChannels.size();
    }
}
