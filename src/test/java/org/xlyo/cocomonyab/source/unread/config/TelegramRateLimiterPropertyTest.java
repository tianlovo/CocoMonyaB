package org.xlyo.cocomonyab.source.unread.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * TelegramRateLimiter 属性测试
 * 
 * 使用属性测试验证速率限制器在所有输入下都能正确限制 API 调用频率
 * 
 * Property 7: API 调用延迟
 * 
 * Validates: Requirements 3.1, 11.4
 */
class TelegramRateLimiterPropertyTest {
    
    /**
     * Property 7: API 调用延迟
     * 
     * For any 两次连续的 API 调用，它们之间的时间间隔应该大于或等于配置的延迟时间。
     * 
     * 此测试验证速率限制器能够正确控制 API 调用频率：
     * - 配置为每秒最多 1 个请求
     * - 连续调用之间的时间间隔应该 >= 1000ms
     * 
     * Validates: Requirements 3.1, 11.4
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 7: API 调用延迟")
    void apiCallDelayEnforced(
            @ForAll @IntRange(min = 2, max = 10) int numberOfCalls) {
        
        // 创建速率限制器配置：每秒最多 1 个请求
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))  // 每 1 秒刷新一次限制
                .limitForPeriod(1)                          // 每个周期内最多 1 个请求
                .timeoutDuration(Duration.ofSeconds(5))     // 等待许可的超时时间
                .build();
        
        RateLimiter rateLimiter = RateLimiter.of("test-rate-limiter", config);
        
        // 记录每次调用的时间戳
        List<Long> timestamps = new ArrayList<>();
        
        // 执行多次 API 调用
        for (int i = 0; i < numberOfCalls; i++) {
            // 获取许可（会阻塞直到可以执行）
            rateLimiter.acquirePermission();
            
            // 记录时间戳
            timestamps.add(System.currentTimeMillis());
        }
        
        // 验证：连续调用之间的时间间隔应该 >= 1000ms（允许一些误差）
        for (int i = 1; i < timestamps.size(); i++) {
            long interval = timestamps.get(i) - timestamps.get(i - 1);
            
            // 允许 50ms 的误差（考虑到系统调度和时钟精度）
            Assertions.assertTrue(interval >= 950,
                    String.format("Interval between calls %d and %d should be >= 950ms, but was %dms",
                            i - 1, i, interval));
        }
        
        // 验证：总耗时应该大约等于 (numberOfCalls - 1) * 1000ms
        long totalDuration = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
        long expectedMinDuration = (numberOfCalls - 1) * 950; // 允许 50ms 误差
        
        Assertions.assertTrue(totalDuration >= expectedMinDuration,
                String.format("Total duration should be >= %dms, but was %dms",
                        expectedMinDuration, totalDuration));
    }
    
    /**
     * 附加属性测试：速率限制器配置验证
     * 
     * 验证速率限制器的配置参数是否正确
     */
    @Property(tries = 10)
    @Label("Feature: unread-channel-message-source, Additional Property: 速率限制器配置验证")
    void rateLimiterConfigValidation() {
        
        // 创建配置实例
        TelegramRateLimiterConfig config = new TelegramRateLimiterConfig();
        RateLimiter rateLimiter = config.telegramApiRateLimiter();
        
        // 验证：速率限制器不为空
        Assertions.assertNotNull(rateLimiter, "RateLimiter should not be null");
        
        // 验证：速率限制器名称正确
        Assertions.assertEquals("telegram-api", rateLimiter.getName(),
                "RateLimiter name should be 'telegram-api'");
        
        // 验证：速率限制器配置正确
        io.github.resilience4j.ratelimiter.RateLimiterConfig rateLimiterConfig = 
            rateLimiter.getRateLimiterConfig();
        
        Assertions.assertEquals(Duration.ofSeconds(1), rateLimiterConfig.getLimitRefreshPeriod(),
                "Limit refresh period should be 1 second");
        Assertions.assertEquals(1, rateLimiterConfig.getLimitForPeriod(),
                "Limit for period should be 1");
        Assertions.assertEquals(Duration.ofSeconds(5), rateLimiterConfig.getTimeoutDuration(),
                "Timeout duration should be 5 seconds");
    }
    
    /**
     * 附加属性测试：单次调用不延迟
     * 
     * 验证第一次调用不会被延迟
     */
    @Property(tries = 10)
    @Label("Feature: unread-channel-message-source, Additional Property: 单次调用不延迟")
    void firstCallNotDelayed() {
        
        // 创建速率限制器
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        
        RateLimiter rateLimiter = RateLimiter.of("test-first-call", config);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 第一次调用
        rateLimiter.acquirePermission();
        
        // 记录结束时间
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 验证：第一次调用应该立即返回（允许 100ms 的系统开销）
        Assertions.assertTrue(duration < 100,
                String.format("First call should not be delayed, but took %dms", duration));
    }
    
    /**
     * 附加属性测试：并发调用序列化
     * 
     * 验证并发调用会被正确序列化，保持速率限制
     */
    @Property(tries = 50)
    @Label("Feature: unread-channel-message-source, Additional Property: 并发调用序列化")
    void concurrentCallsSerialized(
            @ForAll @IntRange(min = 2, max = 5) int numberOfThreads) throws InterruptedException {
        
        // 创建速率限制器
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(10))
                .build();
        
        RateLimiter rateLimiter = RateLimiter.of("test-concurrent", config);
        
        // 记录所有调用的时间戳（线程安全）
        List<Long> timestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
        
        // 创建多个线程并发调用
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                rateLimiter.acquirePermission();
                timestamps.add(System.currentTimeMillis());
            });
            threads.add(thread);
        }
        
        // 启动所有线程
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证：所有调用都完成了
        Assertions.assertEquals(numberOfThreads, timestamps.size(),
                "All threads should have completed");
        
        // 排序时间戳
        timestamps.sort(Long::compareTo);
        
        // 验证：连续调用之间的时间间隔应该 >= 950ms
        for (int i = 1; i < timestamps.size(); i++) {
            long interval = timestamps.get(i) - timestamps.get(i - 1);
            
            Assertions.assertTrue(interval >= 950,
                    String.format("Interval between concurrent calls %d and %d should be >= 950ms, but was %dms",
                            i - 1, i, interval));
        }
        
        // 验证：总耗时应该大约等于 (numberOfThreads - 1) * 1000ms
        long totalDuration = timestamps.get(timestamps.size() - 1) - timestamps.get(0);
        long expectedMinDuration = (numberOfThreads - 1) * 950;
        
        Assertions.assertTrue(totalDuration >= expectedMinDuration,
                String.format("Total duration for concurrent calls should be >= %dms, but was %dms",
                        expectedMinDuration, totalDuration));
    }
}
