package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Telegram 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    
    /**
     * 设备型号标识
     */
    private String deviceModel = "Coco Monya";
    
    /**
     * 登录超时时间（分钟）
     */
    private int loginTimeoutMinutes = 2;
}
