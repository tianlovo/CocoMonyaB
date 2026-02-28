package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启动阶段信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartupPhaseVO {
    
    /**
     * 阶段名称
     */
    private String name;
    
    /**
     * 阶段状态
     */
    private String status;
    
    /**
     * 开始时间（毫秒时间戳）
     */
    private Long startTime;
    
    /**
     * 结束时间（毫秒时间戳）
     */
    private Long endTime;
    
    /**
     * 耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 错误信息（仅在失败时有值）
     */
    private String errorMessage;
}
