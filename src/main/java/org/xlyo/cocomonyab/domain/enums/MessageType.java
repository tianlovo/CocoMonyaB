package org.xlyo.cocomonyab.domain.enums;

/**
 * 消息类型枚举
 * 涵盖所有TDLib支持的消息类型
 */
public enum MessageType {
    TEXT("text", "文本消息"),
    PHOTO("photo", "图片消息"),
    VIDEO("video", "视频消息"),
    DOCUMENT("document", "文档消息"),
    AUDIO("audio", "音频消息"),
    VOICE("voice", "语音消息"),
    VIDEO_NOTE("video_note", "视频笔记"),
    ANIMATION("animation", "动画消息"),
    STICKER("sticker", "贴纸消息"),
    POLL("poll", "投票消息"),
    TELEGRAPH("telegraph", "Telegraph文章"),
    MEDIA_GROUP("media_group", "媒体组"),
    OTHER("other", "其他类型");
    
    private final String code;
    private final String description;
    
    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 从字符串代码转换为消息类型
     * 
     * @param code 消息类型代码
     * @return 对应的消息类型，如果未找到则返回OTHER
     */
    public static MessageType fromCode(String code) {
        if (code == null) {
            return OTHER;
        }
        
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
