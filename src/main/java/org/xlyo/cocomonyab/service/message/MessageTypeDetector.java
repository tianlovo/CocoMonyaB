package org.xlyo.cocomonyab.service.message;

import it.tdlight.jni.TdApi;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 消息类型检测器
 * 负责从TDLib消息中识别消息类型
 */
@Component
public class MessageTypeDetector {
    
    /**
     * 检测消息类型
     */
    public MessageType detectType(TdApi.Message message) {
        // 检查是否为媒体组
        if (message.mediaAlbumId != 0) {
            return MessageType.MEDIA_GROUP;
        }
        
        // 根据消息内容类型判断
        return detectFromContent(message.content);
    }
    
    /**
     * 从消息内容检测类型
     */
    public MessageType detectFromContent(TdApi.MessageContent content) {
        return switch (content) {
            case TdApi.MessageText text -> detectTextType(text);
            case TdApi.MessagePhoto photo -> MessageType.PHOTO;
            case TdApi.MessageVideo video -> MessageType.VIDEO;
            case TdApi.MessageDocument document -> MessageType.DOCUMENT;
            case TdApi.MessageAudio audio -> MessageType.AUDIO;
            case TdApi.MessageVoiceNote voice -> MessageType.VOICE;
            case TdApi.MessageVideoNote videoNote -> MessageType.VIDEO_NOTE;
            case TdApi.MessageAnimation animation -> MessageType.ANIMATION;
            case TdApi.MessageSticker sticker -> MessageType.STICKER;
            case TdApi.MessagePoll poll -> MessageType.POLL;
            default -> MessageType.OTHER;
        };
    }
    
    /**
     * 检测文本消息类型（可能是Telegraph）
     */
    private MessageType detectTextType(TdApi.MessageText text) {
        // 只要包含 webPage 就认为是 Telegraph 消息
        if (text.webPage != null) {
            return MessageType.TELEGRAPH;
        }
        return MessageType.TEXT;
    }
}
