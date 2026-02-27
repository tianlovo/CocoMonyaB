package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * Telegraph消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TelegraphMessageEntity extends BaseMessageEntity {
    /**
     * 文本内容
     */
    private String textContent;
    
    /**
     * WebPage信息
     */
    private WebPageInfo webPage;
    
    @Override
    public MessageType getType() {
        return MessageType.TELEGRAPH;
    }
}
