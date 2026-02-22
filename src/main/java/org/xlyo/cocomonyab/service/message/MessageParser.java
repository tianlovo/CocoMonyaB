package org.xlyo.cocomonyab.service.message;

import it.tdlight.jni.TdApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息解析器
 * 将TDLib消息解析为对应的实体类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageParser {
    
    private final MessageTypeDetector typeDetector;
    
    /**
     * 解析消息
     */
    public BaseMessageEntity parse(TdApi.Message message) {
        return parse(message, null, null);
    }
    
    /**
     * 解析消息（带频道信息）
     */
    public BaseMessageEntity parse(TdApi.Message message, String channelUsername, String channelTitle) {
        MessageType type = typeDetector.detectType(message);
        
        BaseMessageEntity entity = switch (type) {
            case TEXT -> parseTextMessage(message);
            case TELEGRAPH -> parseTelegraphMessage(message);
            case PHOTO -> parsePhotoMessage(message);
            case VIDEO -> parseVideoMessage(message);
            case DOCUMENT -> parseDocumentMessage(message);
            case AUDIO -> parseAudioMessage(message);
            case VOICE -> parseVoiceMessage(message);
            case VIDEO_NOTE -> parseVideoNoteMessage(message);
            case ANIMATION -> parseAnimationMessage(message);
            case STICKER -> parseStickerMessage(message);
            case POLL -> parsePollMessage(message);
            case MEDIA_GROUP -> throw new IllegalStateException("Media group should be handled separately");
            default -> parseOtherMessage(message);
        };
        
        // 设置频道信息
        if (channelUsername != null) {
            entity.setChannelUsername(channelUsername);
        }
        if (channelTitle != null) {
            entity.setChannelTitle(channelTitle);
        }
        
        return entity;
    }
    
    /**
     * 填充基础字段
     */
    private void fillBaseFields(BaseMessageEntity entity, TdApi.Message message) {
        entity.setMessageId(message.id);
        entity.setChatId(message.chatId);
        entity.setDate(message.date);
        entity.setEditDate(message.editDate);
        entity.setMediaAlbumId(message.mediaAlbumId);
        entity.setIsMediaGroup(message.mediaAlbumId != 0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        
        if (message.interactionInfo != null) {
            entity.setViews(message.interactionInfo.viewCount);
            entity.setForwards(message.interactionInfo.forwardCount);
        }
    }
    
    private TextMessageEntity parseTextMessage(TdApi.Message message) {
        TextMessageEntity entity = new TextMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageText text) {
            entity.setTextContent(text.text.text);
        }
        
        return entity;
    }
    
    private TelegraphMessageEntity parseTelegraphMessage(TdApi.Message message) {
        TelegraphMessageEntity entity = new TelegraphMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageText text) {
            entity.setTextContent(text.text.text);
            if (text.webPage != null) {
                entity.setWebPage(parseWebPage(text.webPage));
            }
        }
        
        return entity;
    }
    
    private PhotoMessageEntity parsePhotoMessage(TdApi.Message message) {
        PhotoMessageEntity entity = new PhotoMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessagePhoto photo) {
            if (photo.caption != null) {
                entity.setCaption(photo.caption.text);
            }
            entity.setPhotos(parsePhotoSizes(photo.photo.sizes));
        }
        
        return entity;
    }
    
    private VideoMessageEntity parseVideoMessage(TdApi.Message message) {
        VideoMessageEntity entity = new VideoMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageVideo video) {
            if (video.caption != null) {
                entity.setCaption(video.caption.text);
            }
            entity.setVideo(parseVideoFile(video.video));
            entity.setMimeType(video.video.mimeType);
            entity.setDuration(video.video.duration);
        }
        
        return entity;
    }
    
    private DocumentMessageEntity parseDocumentMessage(TdApi.Message message) {
        DocumentMessageEntity entity = new DocumentMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageDocument document) {
            if (document.caption != null) {
                entity.setCaption(document.caption.text);
            }
            entity.setDocument(parseDocumentFile(document.document));
            entity.setFileName(document.document.fileName);
            entity.setMimeType(document.document.mimeType);
        }
        
        return entity;
    }
    
    private AudioMessageEntity parseAudioMessage(TdApi.Message message) {
        AudioMessageEntity entity = new AudioMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageAudio audio) {
            if (audio.caption != null) {
                entity.setCaption(audio.caption.text);
            }
            entity.setAudio(parseAudioFile(audio.audio));
            entity.setDuration(audio.audio.duration);
            entity.setTitle(audio.audio.title);
            entity.setPerformer(audio.audio.performer);
        }
        
        return entity;
    }
    
    private VoiceMessageEntity parseVoiceMessage(TdApi.Message message) {
        VoiceMessageEntity entity = new VoiceMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageVoiceNote voice) {
            if (voice.caption != null) {
                entity.setCaption(voice.caption.text);
            }
            entity.setVoice(parseVoiceFile(voice.voiceNote));
            entity.setDuration(voice.voiceNote.duration);
        }
        
        return entity;
    }
    
    private VideoNoteMessageEntity parseVideoNoteMessage(TdApi.Message message) {
        VideoNoteMessageEntity entity = new VideoNoteMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageVideoNote videoNote) {
            entity.setVideoNote(parseVideoNoteFile(videoNote.videoNote));
            entity.setDuration(videoNote.videoNote.duration);
        }
        
        return entity;
    }
    
    private AnimationMessageEntity parseAnimationMessage(TdApi.Message message) {
        AnimationMessageEntity entity = new AnimationMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageAnimation animation) {
            if (animation.caption != null) {
                entity.setCaption(animation.caption.text);
            }
            entity.setAnimation(parseAnimationFile(animation.animation));
            entity.setDuration(animation.animation.duration);
        }
        
        return entity;
    }
    
    private StickerMessageEntity parseStickerMessage(TdApi.Message message) {
        StickerMessageEntity entity = new StickerMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessageSticker sticker) {
            entity.setSticker(parseStickerFile(sticker.sticker));
            entity.setEmoji(sticker.sticker.emoji);
        }
        
        return entity;
    }
    
    private PollMessageEntity parsePollMessage(TdApi.Message message) {
        PollMessageEntity entity = new PollMessageEntity();
        fillBaseFields(entity, message);
        
        if (message.content instanceof TdApi.MessagePoll poll) {
            entity.setQuestion(poll.poll.question);
            entity.setTotalVoterCount(poll.poll.totalVoterCount);
            entity.setIsClosed(poll.poll.isClosed);
        }
        
        return entity;
    }
    
    private OtherMessageEntity parseOtherMessage(TdApi.Message message) {
        OtherMessageEntity entity = new OtherMessageEntity();
        fillBaseFields(entity, message);
        entity.setContentTypeName(message.content.getClass().getSimpleName());
        return entity;
    }
    
    /**
     * 解析WebPage信息
     */
    private WebPageInfo parseWebPage(TdApi.WebPage webPage) {
        WebPageInfo info = new WebPageInfo();
        info.setUrl(webPage.url);
        info.setTitle(webPage.title);
        info.setAuthor(webPage.author);
        if (webPage.description != null) {
            info.setDescription(webPage.description.text);
        }
        info.setSiteName(webPage.siteName);
        info.setHasInstantView(webPage.instantViewVersion > 0);
        info.setInstantViewVersion(webPage.instantViewVersion);
        return info;
    }
    
    /**
     * 解析图片尺寸列表
     */
    private List<MediaFile> parsePhotoSizes(TdApi.PhotoSize[] sizes) {
        List<MediaFile> files = new ArrayList<>();
        if (sizes != null) {
            for (TdApi.PhotoSize size : sizes) {
                MediaFile file = new MediaFile();
                file.setFileId(size.photo.id);
                file.setFileUniqueId(size.photo.remote.uniqueId);
                file.setFileSize((long) size.photo.size);
                file.setFilePath(size.photo.local.path);
                file.setWidth(size.width);
                file.setHeight(size.height);
                files.add(file);
            }
        }
        return files;
    }
    
    /**
     * 解析视频文件
     */
    private MediaFile parseVideoFile(TdApi.Video video) {
        MediaFile file = new MediaFile();
        file.setFileId(video.video.id);
        file.setFileUniqueId(video.video.remote.uniqueId);
        file.setFileSize((long) video.video.size);
        file.setFilePath(video.video.local.path);
        file.setWidth(video.width);
        file.setHeight(video.height);
        file.setDuration(video.duration);
        file.setMimeType(video.mimeType);
        file.setFileName(video.fileName);
        return file;
    }
    
    /**
     * 解析文档文件
     */
    private MediaFile parseDocumentFile(TdApi.Document document) {
        MediaFile file = new MediaFile();
        file.setFileId(document.document.id);
        file.setFileUniqueId(document.document.remote.uniqueId);
        file.setFileSize((long) document.document.size);
        file.setFilePath(document.document.local.path);
        file.setMimeType(document.mimeType);
        file.setFileName(document.fileName);
        return file;
    }
    
    /**
     * 解析音频文件
     */
    private MediaFile parseAudioFile(TdApi.Audio audio) {
        MediaFile file = new MediaFile();
        file.setFileId(audio.audio.id);
        file.setFileUniqueId(audio.audio.remote.uniqueId);
        file.setFileSize((long) audio.audio.size);
        file.setFilePath(audio.audio.local.path);
        file.setDuration(audio.duration);
        file.setMimeType(audio.mimeType);
        file.setFileName(audio.fileName);
        return file;
    }
    
    /**
     * 解析语音文件
     */
    private MediaFile parseVoiceFile(TdApi.VoiceNote voice) {
        MediaFile file = new MediaFile();
        file.setFileId(voice.voice.id);
        file.setFileUniqueId(voice.voice.remote.uniqueId);
        file.setFileSize((long) voice.voice.size);
        file.setFilePath(voice.voice.local.path);
        file.setDuration(voice.duration);
        file.setMimeType(voice.mimeType);
        return file;
    }
    
    /**
     * 解析视频笔记文件
     */
    private MediaFile parseVideoNoteFile(TdApi.VideoNote videoNote) {
        MediaFile file = new MediaFile();
        file.setFileId(videoNote.video.id);
        file.setFileUniqueId(videoNote.video.remote.uniqueId);
        file.setFileSize((long) videoNote.video.size);
        file.setFilePath(videoNote.video.local.path);
        file.setDuration(videoNote.duration);
        return file;
    }
    
    /**
     * 解析动画文件
     */
    private MediaFile parseAnimationFile(TdApi.Animation animation) {
        MediaFile file = new MediaFile();
        file.setFileId(animation.animation.id);
        file.setFileUniqueId(animation.animation.remote.uniqueId);
        file.setFileSize((long) animation.animation.size);
        file.setFilePath(animation.animation.local.path);
        file.setWidth(animation.width);
        file.setHeight(animation.height);
        file.setDuration(animation.duration);
        file.setMimeType(animation.mimeType);
        file.setFileName(animation.fileName);
        return file;
    }
    
    /**
     * 解析贴纸文件
     */
    private MediaFile parseStickerFile(TdApi.Sticker sticker) {
        MediaFile file = new MediaFile();
        file.setFileId(sticker.sticker.id);
        file.setFileUniqueId(sticker.sticker.remote.uniqueId);
        file.setFileSize((long) sticker.sticker.size);
        file.setFilePath(sticker.sticker.local.path);
        file.setWidth(sticker.width);
        file.setHeight(sticker.height);
        return file;
    }
}
