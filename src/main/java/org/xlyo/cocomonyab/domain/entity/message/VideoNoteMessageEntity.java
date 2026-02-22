package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 视频笔记消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoNoteMessageEntity extends BaseMessageEntity {
    /**
     * 视频笔记文件
     */
    private MediaFile videoNote;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    @Override
    public MessageType getType() {
        return MessageType.VIDEO_NOTE;
    }
}
