package org.xlyo.cocomonyab.common.exception;

import lombok.Getter;
import org.xlyo.cocomonyab.common.enums.ResponseCode;

import java.util.List;
import java.util.Map;

/**
 * 引用完整性异常
 */
@Getter
public class ReferenceIntegrityException extends BusinessException {
    /**
     * 引用信息
     */
    private final Map<String, List<String>> references;
    
    public ReferenceIntegrityException(String message, Map<String, List<String>> references) {
        super(ResponseCode.OPERATION_FAILED, message);
        this.references = references;
    }
}
