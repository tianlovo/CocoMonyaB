package org.xlyo.cocomonyab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoDBProperties {
    
    /**
     * MongoDB 运行模式：embedded（嵌入式）或 remote（远程）
     */
    private String mode = "embedded";
    
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
        
        /**
         * 存储配置
         */
        private Storage storage = new Storage();
        
        @Data
        public static class Storage {
            /**
             * 嵌入式 MongoDB 数据存储目录
             */
            private String directory = "data/db/mongo";
        }
    }
}
