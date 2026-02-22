package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 视频消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoMessageEntity extends BaseMessageEntity {
    /**
     * 视频说明文字
     */
    private String caption;
    
    /**
     * 视频文件
     */
    private MediaFile video;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    @Override
    public MessageType getType() {
        return MessageType.VIDEO;
    }
}
