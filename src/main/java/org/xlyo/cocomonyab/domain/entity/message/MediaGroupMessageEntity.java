package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.util.List;

/**
 * 媒体组消息实体
 * 包含多个媒体消息的组合
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaGroupMessageEntity extends BaseMessageEntity {
    /**
     * 媒体组中的所有消息实体
     */
    private List<BaseMessageEntity> items;
    
    @Override
    public MessageType getType() {
        return MessageType.MEDIA_GROUP;
    }
}
