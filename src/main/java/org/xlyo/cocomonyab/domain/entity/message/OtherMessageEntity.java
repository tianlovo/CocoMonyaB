package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 其他类型消息实体
 * 用于未明确分类的消息类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OtherMessageEntity extends BaseMessageEntity {
    /**
     * 消息内容类型名称
     */
    private String contentTypeName;
    
    /**
     * 原始内容描述
     */
    private String contentDescription;
    
    @Override
    public MessageType getType() {
        return MessageType.OTHER;
    }
}
