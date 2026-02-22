package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 文本消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TextMessageEntity extends BaseMessageEntity {
    /**
     * 文本内容
     */
    private String textContent;
    
    @Override
    public MessageType getType() {
        return MessageType.TEXT;
    }
}
