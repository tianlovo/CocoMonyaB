package org.xlyo.cocomonyab.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 使用Caffeine作为缓存实现
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * TG频道列表缓存名称
     */
    public static final String TG_CHANNELS_CACHE = "tgChannels";

    /**
     * TG频道总数缓存名称
     */
    public static final String TG_CHANNELS_COUNT_CACHE = "tgChannelsCount";

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                // 缓存5分钟后过期
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 最大缓存条目数
                .maximumSize(100)
                // 启用统计
                .recordStats();
    }

    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        org.springframework.cache.caffeine.CaffeineCacheManager cacheManager = 
                new org.springframework.cache.caffeine.CaffeineCacheManager(
                        TG_CHANNELS_CACHE,
                        TG_CHANNELS_COUNT_CACHE
                );
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
