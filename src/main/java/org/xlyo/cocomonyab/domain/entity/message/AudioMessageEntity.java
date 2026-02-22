package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 音频消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioMessageEntity extends BaseMessageEntity {
    /**
     * 音频说明文字
     */
    private String caption;
    
    /**
     * 音频文件
     */
    private MediaFile audio;
    
    /**
     * 音频时长（秒）
     */
    private Integer duration;
    
    /**
     * 表演者
     */
    private String performer;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    @Override
    public MessageType getType() {
        return MessageType.AUDIO;
    }
}
