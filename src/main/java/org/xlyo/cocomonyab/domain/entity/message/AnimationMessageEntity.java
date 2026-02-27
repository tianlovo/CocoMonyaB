package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 动画消息实体（GIF等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnimationMessageEntity extends BaseMessageEntity {
    /**
     * 动画说明文字
     */
    private String caption;
    
    /**
     * 动画文件
     */
    private MediaFile animation;
    
    /**
     * 动画时长（秒）
     */
    private Integer duration;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    @Override
    public MessageType getType() {
        return MessageType.ANIMATION;
    }
}
