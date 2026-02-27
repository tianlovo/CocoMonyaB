package org.xlyo.cocomonyab.common.exception;

import lombok.Getter;
import org.xlyo.cocomonyab.common.enums.ResponseCode;

/**
 * 标签唯一性冲突异常
 */
@Getter
public class TagUniquenessException extends BusinessException {
    /**
     * 冲突的实体类型
     */
    private final String conflictType;
    
    /**
     * 冲突实体的ID
     */
    private final String conflictId;
    
    /**
     * 冲突的名称或别名
     */
    private final String conflictValue;
    
    public TagUniquenessException(String conflictType, String conflictId, String conflictValue) {
        super(ResponseCode.DATA_ALREADY_EXISTS, 
              String.format("名称或别名已存在：冲突实体类型=%s, ID=%s, 值=%s", 
                          conflictType, conflictId, conflictValue));
        this.conflictType = conflictType;
        this.conflictId = conflictId;
        this.conflictValue = conflictValue;
    }
}
