package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统信息响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoVO {
    
    /**
     * 项目名称
     */
    private String projectName;
    
    /**
     * 项目版本号
     */
    private String version;
    
    /**
     * 项目组ID
     */
    private String group;
    
    /**
     * 项目描述
     */
    private String description;
    
    /**
     * 构建时间（ISO 8601格式）
     */
    private String buildTime;
    
    /**
     * Java版本
     */
    private String javaVersion;
    
    /**
     * Gradle版本
     */
    private String gradleVersion;
    
    /**
     * 完整的版本信息字符串
     */
    private String fullVersionInfo;
}
