package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "env")
public class TgEnvProperties {
    
    /**
     * Telegram API ID
     */
    private String apiId;
    
    /**
     * Telegram API HASH
     */
    private String apiHash;
    
    /**
     * Telegram 手机号
     */
    private String tgPhone;
    
    /**
     * Telegram 两步验证密码
     */
    private String tg2fa;
}
