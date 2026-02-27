package org.xlyo.cocomonyab.domain.entity.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.xlyo.cocomonyab.domain.enums.MessageType;

/**
 * 文档消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentMessageEntity extends BaseMessageEntity {
    /**
     * 文档说明文字
     */
    private String caption;
    
    /**
     * 文档文件
     */
    private MediaFile document;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * MIME类型
     */
    private String mimeType;
    
    @Override
    public MessageType getType() {
        return MessageType.DOCUMENT;
    }
}
