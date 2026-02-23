package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    
    /**
     * 设备型号
     */
    private String deviceModel;
    
    /**
     * 数据库目录
     */
    private String databaseDirectory;
    
    /**
     * 下载目录
     */
    private String downloadDirectory;
    
    /**
     * 登录超时时间（分钟），默认 2 分钟
     */
    private int loginTimeoutMinutes = 2;
}
