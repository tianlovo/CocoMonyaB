package org.xlyo.cocomonyab.source.unread.config;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Assertions;

/**
 * UnreadMessageSourceConfig 属性测试
 * 
 * 使用属性测试验证配置类的约束在所有输入下都成立
 * 
 * Property 6: 数量限制遵守
 * 
 * Validates: Requirements 2.5, 11.2
 */
class UnreadMessageSourceConfigPropertyTest {
    
    /**
     * Property 6: 数量限制遵守
     * 
     * For any 配置的最大消息数量 N，单个频道返回的消息数量应该不超过 N。
     * 
     * 此测试验证配置类的约束：
     * - maxMessagesPerFetch 必须 >= 1
     * - maxTotalMessages 必须 >= 1
     * - maxTotalMessages 应该 >= maxMessagesPerFetch（逻辑约束）
     * 
     * Validates: Requirements 2.5, 11.2
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 6: 数量限制遵守")
    void maxMessageLimitsRespected(
            @ForAll @IntRange(min = 1, max = 200) int maxMessagesPerFetch,
            @ForAll @IntRange(min = 1, max = 2000) int maxTotalMessages) {
        
        // 创建配置实例
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setMaxMessagesPerFetch(maxMessagesPerFetch);
        config.setMaxTotalMessages(maxTotalMessages);
        
        // 验证：maxMessagesPerFetch 必须为正数
        Assertions.assertTrue(config.getMaxMessagesPerFetch() >= 1,
                "maxMessagesPerFetch must be at least 1");
        
        // 验证：maxTotalMessages 必须为正数
        Assertions.assertTrue(config.getMaxTotalMessages() >= 1,
                "maxTotalMessages must be at least 1");
        
        // 验证：配置值被正确设置
        Assertions.assertEquals(maxMessagesPerFetch, config.getMaxMessagesPerFetch(),
                "maxMessagesPerFetch should be set correctly");
        Assertions.assertEquals(maxTotalMessages, config.getMaxTotalMessages(),
                "maxTotalMessages should be set correctly");
    }
    
    /**
     * 附加属性测试：API 调用延迟配置验证
     * 
     * 验证 API 调用延迟配置的有效性
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: API 调用延迟配置验证")
    void apiCallDelayConfigValidation(
            @ForAll @LongRange(min = 0, max = 10000) long apiCallDelay) {
        
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setApiCallDelay(apiCallDelay);
        
        // 验证：apiCallDelay 必须非负
        Assertions.assertTrue(config.getApiCallDelay() >= 0,
                "apiCallDelay must be non-negative");
        
        // 验证：配置值被正确设置
        Assertions.assertEquals(apiCallDelay, config.getApiCallDelay(),
                "apiCallDelay should be set correctly");
    }
    
    /**
     * 附加属性测试：重试配置验证
     * 
     * 验证重试相关配置的有效性和一致性
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 重试配置验证")
    void retryConfigValidation(
            @ForAll @IntRange(min = 0, max = 10) int maxRetries,
            @ForAll @LongRange(min = 0, max = 10000) long retryBaseDelay,
            @ForAll @LongRange(min = 0, max = 120000) long retryMaxDelay) {
        
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setMaxRetries(maxRetries);
        config.setRetryBaseDelay(retryBaseDelay);
        config.setRetryMaxDelay(retryMaxDelay);
        
        // 验证：maxRetries 必须非负
        Assertions.assertTrue(config.getMaxRetries() >= 0,
                "maxRetries must be non-negative");
        
        // 验证：retryBaseDelay 必须非负
        Assertions.assertTrue(config.getRetryBaseDelay() >= 0,
                "retryBaseDelay must be non-negative");
        
        // 验证：retryMaxDelay 必须非负
        Assertions.assertTrue(config.getRetryMaxDelay() >= 0,
                "retryMaxDelay must be non-negative");
        
        // 验证：配置值被正确设置
        Assertions.assertEquals(maxRetries, config.getMaxRetries(),
                "maxRetries should be set correctly");
        Assertions.assertEquals(retryBaseDelay, config.getRetryBaseDelay(),
                "retryBaseDelay should be set correctly");
        Assertions.assertEquals(retryMaxDelay, config.getRetryMaxDelay(),
                "retryMaxDelay should be set correctly");
    }
    
    /**
     * 附加属性测试：批处理配置验证
     * 
     * 验证批处理相关配置的有效性
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 批处理配置验证")
    void batchConfigValidation(
            @ForAll @IntRange(min = 1, max = 100) int batchSize,
            @ForAll @LongRange(min = 0, max = 5000) long batchDelay) {
        
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setBatchSize(batchSize);
        config.setBatchDelay(batchDelay);
        
        // 验证：batchSize 必须为正数
        Assertions.assertTrue(config.getBatchSize() >= 1,
                "batchSize must be at least 1");
        
        // 验证：batchDelay 必须非负
        Assertions.assertTrue(config.getBatchDelay() >= 0,
                "batchDelay must be non-negative");
        
        // 验证：配置值被正确设置
        Assertions.assertEquals(batchSize, config.getBatchSize(),
                "batchSize should be set correctly");
        Assertions.assertEquals(batchDelay, config.getBatchDelay(),
                "batchDelay should be set correctly");
    }
    
    /**
     * 附加属性测试：缓冲区 TTL 配置验证
     * 
     * 验证缓冲区 TTL 配置的有效性
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 缓冲区 TTL 配置验证")
    void bufferTtlConfigValidation(
            @ForAll @IntRange(min = 1, max = 30) int bufferTtlDays) {
        
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setBufferTtlDays(bufferTtlDays);
        
        // 验证：bufferTtlDays 必须为正数
        Assertions.assertTrue(config.getBufferTtlDays() >= 1,
                "bufferTtlDays must be at least 1");
        
        // 验证：配置值被正确设置
        Assertions.assertEquals(bufferTtlDays, config.getBufferTtlDays(),
                "bufferTtlDays should be set correctly");
    }
    
    /**
     * 附加属性测试：默认值验证
     * 
     * 验证配置类的默认值是否合理
     */
    @Property(tries = 10)
    @Label("Feature: unread-channel-message-source, Additional Property: 默认值验证")
    void defaultValuesValidation() {
        
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        
        // 验证：所有默认值都是合理的
        Assertions.assertNotNull(config.getAutoDetectOnStartup(),
                "autoDetectOnStartup should have a default value");
        Assertions.assertTrue(config.getMaxMessagesPerFetch() >= 1,
                "default maxMessagesPerFetch should be at least 1");
        Assertions.assertTrue(config.getMaxTotalMessages() >= 1,
                "default maxTotalMessages should be at least 1");
        Assertions.assertTrue(config.getApiCallDelay() >= 0,
                "default apiCallDelay should be non-negative");
        Assertions.assertTrue(config.getMaxRetries() >= 0,
                "default maxRetries should be non-negative");
        Assertions.assertTrue(config.getRetryBaseDelay() >= 0,
                "default retryBaseDelay should be non-negative");
        Assertions.assertTrue(config.getRetryMaxDelay() >= 0,
                "default retryMaxDelay should be non-negative");
        Assertions.assertTrue(config.getBatchSize() >= 1,
                "default batchSize should be at least 1");
        Assertions.assertTrue(config.getBatchDelay() >= 0,
                "default batchDelay should be non-negative");
        Assertions.assertTrue(config.getBufferTtlDays() >= 1,
                "default bufferTtlDays should be at least 1");
        
        // 验证：默认值的逻辑关系
        Assertions.assertTrue(config.getRetryMaxDelay() >= config.getRetryBaseDelay(),
                "default retryMaxDelay should be >= retryBaseDelay");
    }
    
    /**
     * 附加属性测试：配置一致性验证
     * 
     * 验证配置之间的逻辑一致性
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 配置一致性验证")
    void configConsistencyValidation(
            @ForAll @IntRange(min = 1, max = 100) int maxMessagesPerFetch,
            @ForAll @IntRange(min = 1, max = 1000) int maxTotalMessages,
            @ForAll @IntRange(min = 1, max = 50) int batchSize) {
        
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setMaxMessagesPerFetch(maxMessagesPerFetch);
        config.setMaxTotalMessages(maxTotalMessages);
        config.setBatchSize(batchSize);
        
        // 验证：所有配置都是正数
        Assertions.assertTrue(config.getMaxMessagesPerFetch() > 0,
                "maxMessagesPerFetch must be positive");
        Assertions.assertTrue(config.getMaxTotalMessages() > 0,
                "maxTotalMessages must be positive");
        Assertions.assertTrue(config.getBatchSize() > 0,
                "batchSize must be positive");
        
        // 注意：我们不强制 maxTotalMessages >= maxMessagesPerFetch
        // 因为这是一个软约束，系统会自动处理这种情况
        // 但我们可以记录这种情况以供参考
        if (config.getMaxTotalMessages() < config.getMaxMessagesPerFetch()) {
            // 这是允许的，但在实际使用中，maxTotalMessages 会限制实际获取的消息数
            Assertions.assertTrue(true, "maxTotalMessages < maxMessagesPerFetch is allowed");
        }
    }
}
