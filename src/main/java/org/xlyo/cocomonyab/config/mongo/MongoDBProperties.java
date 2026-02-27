package org.xlyo.cocomonyab.config.mongo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoDBProperties {
    
    /**
     * MongoDB 运行模式：EMBEDDED（嵌入式）或 REMOTE（远程）
     */
    private MongoMode mode = MongoMode.EMBEDDED;
    
    /**
     * 远程 MongoDB 连接 URI（仅在 remote 模式下使用）
     */
    private String uri;
    
    /**
     * 嵌入式 MongoDB 配置
     */
    private Embedded embedded = new Embedded();
    
    @Data
    public static class Embedded {
        /**
         * MongoDB 版本号
         */
        private String version = "7.0.12";
        
        /**
         * MongoDB 监听端口
         */
        private int port = 27017;
        
        /**
         * MongoDB 绑定 IP 地址
         */
        private String bindIp = "127.0.0.1";
    }
}
