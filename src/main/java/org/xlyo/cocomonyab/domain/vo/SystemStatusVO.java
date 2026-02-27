package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统状态响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusVO {
    
    /**
     * 系统是否就绪
     */
    private Boolean ready;
    
    /**
     * 未就绪原因（就绪时为null）
     */
    private String reason;
    
    /**
     * 当前时间戳（毫秒）
     */
    private Long timestamp;
}
