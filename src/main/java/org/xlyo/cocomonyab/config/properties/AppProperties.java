package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置属性
 * 注意：app.data 配置由 DataDirectoryManager 管理，不在此类中
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    // 此类保留用于未来可能的其他 app.* 配置
    // app.data.* 配置由 DataDirectoryManager 管理
}
