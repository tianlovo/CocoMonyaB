package org.xlyo.cocomonyab.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 并发安全性配置属性
 * 用于配置媒体组处理、锁机制和缓存策略
 */
@Data
@Component
@ConfigurationProperties(prefix = "concurrent-safety")
public class ConcurrentSafetyProperties {
    
    /**
     * 媒体组配置
     */
    private MediaGroup mediaGroup = new MediaGroup();
    
    /**
     * 锁配置
     */
    private Lock lock = new Lock();
    
    /**
     * 缓存配置
     */
    private Cache cache = new Cache();
    
    /**
     * 媒体组配置
     */
    @Data
    public static class MediaGroup {
        /**
         * 媒体组超时时间（毫秒）
         * 默认值：2000ms（2秒）
         */
        private long timeout = 2000;
        
        /**
         * 最大缓冲区大小
         * 默认值：1000
         */
        private int maxBufferSize = 1000;
    }
    
    /**
     * 锁配置
     */
    @Data
    public static class Lock {
        /**
         * 分段锁数量（锁条带数）
         * 默认值：128
         */
        private int stripes = 128;
        
        /**
         * 锁超时时间（毫秒）
         * 默认值：5000ms（5秒）
         */
        private long timeout = 5000;
    }
    
    /**
     * 缓存配置
     */
    @Data
    public static class Cache {
        /**
         * 缓存过期时间（秒）
         * 默认值：10秒
         */
        private int ttl = 10;
        
        /**
         * 最大缓存大小
         * 默认值：10000
         */
        private int maxSize = 10000;
        
        /**
         * 失败消息缓存时间（秒）
         * 默认值：5秒
         */
        private int failedMessageTtl = 5;
    }
}
