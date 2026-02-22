package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.util.List;

/**
 * 图片消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PhotoMessageEntity extends BaseMessageEntity {
    /**
     * 图片说明文字
     */
    private String caption;
    
    /**
     * 图片文件列表（不同尺寸）
     */
    private List<MediaFile> photos;
    
    @Override
    public MessageType getType() {
        return MessageType.PHOTO;
    }
}
