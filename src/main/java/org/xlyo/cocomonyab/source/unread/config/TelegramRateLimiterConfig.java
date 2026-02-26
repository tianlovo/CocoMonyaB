package org.xlyo.cocomonyab.source.unread.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 速率限制器配置类
 * 
 * 使用 Resilience4j RateLimiter 控制 Telegram API 调用频率，
 * 避免触发 Telegram 的速率限制（429 错误）
 * 
 * 配置策略：
 * - 每秒最多 1 个请求（保守策略）
 * - 刷新周期：1 秒
 * - 超时时间：5 秒
 */
@Configuration
public class TelegramRateLimiterConfig {
    
    /**
     * 创建 Telegram API 速率限制器 Bean
     * 
     * 配置说明：
     * - limitRefreshPeriod: 限制刷新周期，每 1 秒刷新一次
     * - limitForPeriod: 每个周期内允许的最大请求数，设置为 1（保守策略）
     * - timeoutDuration: 等待许可的超时时间，设置为 5 秒
     * 
     * @return RateLimiter 实例
     */
    @Bean
    public RateLimiter telegramApiRateLimiter() {
        // 配置每秒最多 1 个请求（保守策略）
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))  // 每 1 秒刷新一次限制
                .limitForPeriod(1)                          // 每个周期内最多 1 个请求
                .timeoutDuration(Duration.ofSeconds(5))     // 等待许可的超时时间
                .build();
        
        return RateLimiter.of("telegram-api", config);
    }
}
