package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Data 目录配置属性
 * <p>
 * 注意：data 根目录和 config 目录路径固定，不可配置
 * - data 根目录：固定为应用同级的 "data" 目录
 * - config 目录：固定为 "data/config"
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.data")
public class DataDirectoryProperties {
    
    /**
     * 数据库目录相对路径（相对于 data 根目录）
     */
    private String databaseDirectory = "db";
    
    /**
     * MongoDB 数据存储目录相对路径
     */
    private String mongoDbDirectory = "db/mongo";
    
    /**
     * 会话数据目录相对路径
     */
    private String sessionDirectory = "session";
    
    /**
     * Telegram 会话数据目录相对路径
     */
    private String telegramSessionDirectory = "session/td";
    
    /**
     * 二进制文件目录相对路径
     */
    private String binDirectory = "bin";
    
    /**
     * MongoDB 二进制文件目录相对路径
     */
    private String mongoBinDirectory = "bin/mongo";
    
    /**
     * 临时文件目录相对路径
     */
    private String tmpDirectory = "tmp";
    
    /**
     * 日志文件目录相对路径
     */
    private String logsDirectory = "logs";
}
