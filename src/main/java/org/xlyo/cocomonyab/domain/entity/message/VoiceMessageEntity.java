package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 语音消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VoiceMessageEntity extends BaseMessageEntity {
    /**
     * 语音说明文字
     */
    private String caption;
    
    /**
     * 语音文件
     */
    private MediaFile voice;
    
    /**
     * 语音时长（秒）
     */
    private Integer duration;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    @Override
    public MessageType getType() {
        return MessageType.VOICE;
    }
}
