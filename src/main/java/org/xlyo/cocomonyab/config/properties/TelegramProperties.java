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
    
    /**
     * 登录方式
     * - code: 验证码登录（默认）
     * - qrcode: 二维码登录
     * - console: 控制台输入登录
     */
    private String loginType = "code";
    
    /**
     * 登录方式枚举
     */
    public enum LoginType {
        CODE,      // 验证码登录
        QRCODE     // 二维码登录
    }
    
    /**
     * 获取登录方式枚举
     */
    public LoginType getLoginTypeEnum() {
        try {
            return LoginType.valueOf(loginType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LoginType.CODE; // 默认使用验证码登录
        }
    }
}
