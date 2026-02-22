package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    
    // 媒体组缓冲区：key = chatId:mediaAlbumId, value = 消息列表
    private final Map<String, List<TdApi.Message>> mediaGroupBuffer = new ConcurrentHashMap<>();
    
    // 媒体组最后更新时间：key = chatId:mediaAlbumId, value = 时间戳
    private final Map<String, Long> mediaGroupTimestamps = new ConcurrentHashMap<>();
    
    // 媒体组等待超时时间（毫秒）
    private static final long MEDIA_GROUP_TIMEOUT = 2000; // 2秒
    
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
            // 检查是否为媒体组消息
            if (message.mediaAlbumId != 0) {
                handleMediaGroupMessage(message);
            } else {
                // 普通消息，直接处理
                processSingleMessage(message);
            }
            
        } catch (Exception e) {
            log.error("处理频道消息失败: chatId={}, messageId={}", message.chatId, message.id, e);
        }
    }
    
    /**
     * 处理媒体组消息
     */
    private void handleMediaGroupMessage(TdApi.Message message) {
        String groupKey = message.chatId + ":" + message.mediaAlbumId;
        
        // 添加到缓冲区
        mediaGroupBuffer.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(message);
        
        // 更新时间戳
        mediaGroupTimestamps.put(groupKey, System.currentTimeMillis());
        
        log.debug("收到媒体组消息: chatId={}, mediaAlbumId={}, messageId={}, 当前组内消息数: {}", 
            message.chatId, message.mediaAlbumId, message.id, 
            mediaGroupBuffer.get(groupKey).size());
    }
    
    /**
     * 定时处理超时的媒体组
     * 每秒检查一次
     */
    @Scheduled(fixedDelay = 1000)
    public void processTimedOutMediaGroups() {
        long now = System.currentTimeMillis();
        List<String> timedOutGroups = new ArrayList<>();
        
        // 找出超时的媒体组
        mediaGroupTimestamps.forEach((groupKey, timestamp) -> {
            if (now - timestamp >= MEDIA_GROUP_TIMEOUT) {
                timedOutGroups.add(groupKey);
            }
        });
        
        // 处理超时的媒体组
        for (String groupKey : timedOutGroups) {
            List<TdApi.Message> messages = mediaGroupBuffer.remove(groupKey);
            mediaGroupTimestamps.remove(groupKey);
            
            if (messages != null && !messages.isEmpty()) {
                processMediaGroup(messages);
            }
        }
    }
    
    /**
     * 处理完整的媒体组
     */
    private void processMediaGroup(List<TdApi.Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        
        // 按消息ID排序
        messages.sort(Comparator.comparingLong(m -> m.id));
        
        TdApi.Message firstMessage = messages.get(0);
        long mediaAlbumId = firstMessage.mediaAlbumId;
        long chatId = firstMessage.chatId;
        
        log.info("处理媒体组: chatId={}, mediaAlbumId={}, 消息数量={}", 
            chatId, mediaAlbumId, messages.size());
        
        // 检查媒体组是否已存在
        long existingCount = messageRepository.countByChatIdAndMediaAlbumId(chatId, mediaAlbumId);
        if (existingCount > 0) {
            log.debug("媒体组已存在，跳过: chatId={}, mediaAlbumId={}", chatId, mediaAlbumId);
            return;
        }
        
        // 收集所有消息ID
        List<Long> messageIds = messages.stream()
            .map(m -> m.id)
            .collect(Collectors.toList());
        
        // 解析并保存每条消息
        List<ChannelMessage> savedMessages = new ArrayList<>();
        for (TdApi.Message message : messages) {
            ChannelMessage channelMessage = parseMessage(message);
            
            // 设置媒体组信息
            channelMessage.setMediaAlbumId(mediaAlbumId);
            channelMessage.setIsMediaGroup(true);
            channelMessage.setMediaGroupItemCount(messages.size());
            channelMessage.setMediaGroupMessageIds(messageIds);
            
            // 保存到数据库
            ChannelMessage saved = messageRepository.save(channelMessage);
            savedMessages.add(saved);
        }
        
        // 输出媒体组信息到控制台
        printMediaGroupToConsole(savedMessages, messages);
    }
    
    /**
     * 处理单条消息（非媒体组）
     */
    private void processSingleMessage(TdApi.Message message) {
        // 1. 检查消息是否已存在（去重）
        if (messageRepository.existsByChatIdAndMessageId(message.chatId, message.id)) {
            log.debug("消息已存在，跳过: chatId={}, messageId={}", message.chatId, message.id);
            return;
        }
        
        // 2. 解析消息内容
        ChannelMessage channelMessage = parseMessage(message);
        
        // 设置非媒体组标识
        channelMessage.setMediaAlbumId(0L);
        channelMessage.setIsMediaGroup(false);
        
        // 3. 保存到数据库
        ChannelMessage saved = messageRepository.save(channelMessage);
        
        // 4. 输出到控制台
        printMessageToConsole(saved, message);
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
     * 输出媒体组到控制台
     */
    private void printMediaGroupToConsole(List<ChannelMessage> savedMessages, List<TdApi.Message> originalMessages) {
        if (savedMessages.isEmpty()) {
            return;
        }
        
        ChannelMessage first = savedMessages.get(0);
        
        log.info("━".repeat(80));
        log.info("📨 收到媒体组消息");
        log.info("━".repeat(80));
        log.info("频道: {} (@{})", first.getChannelTitle(), first.getChannelUsername());
        log.info("媒体组ID: {}", first.getMediaAlbumId());
        log.info("频道ID: {}", first.getChatId());
        log.info("消息数量: {} 条", savedMessages.size());
        
        // 格式化时间
        LocalDateTime messageTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(first.getDate()), 
            ZoneId.systemDefault()
        );
        log.info("时间: {}", messageTime);
        
        // 输出每条消息的信息
        log.info("━".repeat(80));
        log.info("📎 媒体组内容:");
        
        for (int i = 0; i < savedMessages.size(); i++) {
            ChannelMessage msg = savedMessages.get(i);
            log.info("  [{}] 消息ID: {}, 类型: {}", i + 1, msg.getMessageId(), msg.getContentType());
            
            // 输出文本内容
            if (msg.getTextContent() != null && !msg.getTextContent().isEmpty()) {
                String text = msg.getTextContent();
                if (text.length() > 50) {
                    text = text.substring(0, 50) + "...";
                }
                log.info("      说明: {}", text);
            }
            
            // 输出媒体文件信息
            if (msg.getMediaFiles() != null && !msg.getMediaFiles().isEmpty()) {
                msg.getMediaFiles().forEach(file -> {
                    log.info("      - 文件类型: {}, 大小: {} bytes", 
                        file.getFileType(), 
                        file.getFileSize());
                });
            }
        }
        
        // 输出互动信息（使用第一条消息的数据）
        if (first.getViews() != null || first.getForwards() != null) {
            log.info("━".repeat(80));
            log.info("📊 互动: 浏览 {} 次, 转发 {} 次", 
                first.getViews() != null ? first.getViews() : 0,
                first.getForwards() != null ? first.getForwards() : 0);
        }
        
        log.info("━".repeat(80));
        log.info("💾 数据库ID列表:");
        savedMessages.forEach(msg -> log.info("   - {}", msg.getId()));
        log.info("━".repeat(80));
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
