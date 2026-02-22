package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 贴纸消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StickerMessageEntity extends BaseMessageEntity {
    /**
     * 贴纸文件
     */
    private MediaFile sticker;
    
    /**
     * 贴纸表情符号
     */
    private String emoji;
    
    /**
     * 贴纸集名称
     */
    private String setName;
    
    @Override
    public MessageType getType() {
        return MessageType.STICKER;
    }
}
