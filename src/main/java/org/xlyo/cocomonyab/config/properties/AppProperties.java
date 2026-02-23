package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    /**
     * 应用配置文件存储位置
     */
    private String configDirectory = "data/config";
}
