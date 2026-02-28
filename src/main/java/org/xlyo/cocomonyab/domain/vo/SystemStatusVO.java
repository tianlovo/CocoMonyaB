package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xlyo.cocomonyab.event.startup.StartupStatus;

/**
 * 系统状态响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusVO {
    
    /**
     * 系统是否就绪
     */
    private Boolean ready;
    
    /**
     * 当前启动状态
     */
    private StartupStatus status;
    
    /**
     * 未就绪原因（就绪时为null）
     */
    private String reason;
    
    /**
     * 当前时间戳（毫秒）
     */
    private Long timestamp;
    
    /**
     * 启动进度（0-100）
     */
    private Integer progress;
    
    /**
     * 当前阶段名称
     */
    private String currentPhase;
}
