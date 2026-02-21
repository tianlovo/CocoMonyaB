package org.xlyo.cocomonyab.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.config.mongo.MongoDBConfiguration;
import org.xlyo.cocomonyab.config.mongo.MongoDBProperties;
import org.xlyo.cocomonyab.config.mongo.MongoMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoDB配置验证测试
 * 测试配置验证逻辑，包括无效模式、缺失URI等场景
 * 
 * Requirements: 1.6, 1.7, 2.4, 2.5
 */
class MongoDBConfigurationValidationTest {
    
    private MongoDBProperties properties;
    private MongoDBConfiguration configuration;
    
    @BeforeEach
    void setUp() {
        properties = new MongoDBProperties();
        configuration = new MongoDBConfiguration();
        // 使用反射设置私有字段
        try {
            java.lang.reflect.Field field = MongoDBConfiguration.class.getDeclaredField("properties");
            field.setAccessible(true);
            field.set(configuration, properties);
        } catch (Exception e) {
            throw new RuntimeException("设置properties字段失败", e);
        }
    }
    
    /**
     * 测试无效的mode配置抛出异常
     * Requirement 1.6: 验证mode配置
     */
    @Test
    void testInvalidModeThrowsException() {
        // Given: 无效的mode字符串
        String invalidMode = "invalid_mode";
        
        // When & Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> MongoMode.fromValue(invalidMode),
                "无效的mode应该抛出IllegalArgumentException"
        );
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("无效的 MongoDB mode"),
                "异常消息应该包含'无效的 MongoDB mode'");
        assertTrue(exception.getMessage().contains("embedded 或 remote"),
                "异常消息应该说明mode的有效值");
    }
    
    /**
     * 测试remote模式缺少URI抛出异常
     * Requirement 1.7: remote模式必须配置URI
     */
    @Test
    void testRemoteModeWithoutUriThrowsException() {
        // Given: remote模式但没有URI
        properties.setMode(MongoMode.REMOTE);
        properties.setUri(null);
        
        // When & Then: 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> configuration.validateConfiguration(),
                "remote模式缺少URI应该抛出IllegalStateException"
        );
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("MongoDB配置无效"),
                "异常消息应该包含'MongoDB配置无效'");
        assertTrue(exception.getMessage().contains("remote模式下必须配置spring.data.mongodb.uri"),
                "异常消息应该说明需要配置URI");
    }
    
    /**
     * 测试remote模式URI为空字符串抛出异常
     * Requirement 1.7: remote模式URI不能为空
     */
    @Test
    void testRemoteModeWithEmptyUriThrowsException() {
        // Given: remote模式但URI为空字符串
        properties.setMode(MongoMode.REMOTE);
        properties.setUri("");
        
        // When & Then: 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> configuration.validateConfiguration(),
                "remote模式URI为空应该抛出IllegalStateException"
        );
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("remote模式下必须配置spring.data.mongodb.uri"),
                "异常消息应该说明需要配置URI");
    }
    
    /**
     * 测试remote模式URI为空白字符串抛出异常
     * Requirement 1.7: remote模式URI不能为空白
     */
    @Test
    void testRemoteModeWithBlankUriThrowsException() {
        // Given: remote模式但URI为空白字符串
        properties.setMode(MongoMode.REMOTE);
        properties.setUri("   ");
        
        // When & Then: 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> configuration.validateConfiguration(),
                "remote模式URI为空白应该抛出IllegalStateException"
        );
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("remote模式下必须配置spring.data.mongodb.uri"),
                "异常消息应该说明需要配置URI");
    }
    
    /**
     * 测试embedded模式配置验证通过
     * Requirement 1.6: embedded模式配置有效
     */
    @Test
    void testEmbeddedModeConfigurationIsValid() {
        // Given: 有效的embedded模式配置
        properties.setMode(MongoMode.EMBEDDED);
        properties.getEmbedded().getStorage().setDirectory("data/db/mongo-test");
        
        // When & Then: 不应该抛出异常
        assertDoesNotThrow(
                () -> configuration.validateConfiguration(),
                "有效的embedded模式配置不应该抛出异常"
        );
    }
    
    /**
     * 测试remote模式配置验证通过
     * Requirement 1.6: remote模式配置有效
     */
    @Test
    void testRemoteModeConfigurationIsValid() {
        // Given: 有效的remote模式配置
        properties.setMode(MongoMode.REMOTE);
        properties.setUri("mongodb://localhost:27017/test");
        
        // When & Then: 不应该抛出异常
        assertDoesNotThrow(
                () -> configuration.validateConfiguration(),
                "有效的remote模式配置不应该抛出异常"
        );
    }
    
    /**
     * 测试mode配置大小写不敏感
     * Requirement 1.6: mode配置应该大小写不敏感
     */
    @Test
    void testModeCaseInsensitive() {
        // Test EMBEDDED (uppercase)
        MongoMode embeddedUpper = MongoMode.fromValue("EMBEDDED");
        assertEquals(MongoMode.EMBEDDED, embeddedUpper, "EMBEDDED (大写) 应该被转换为枚举");
        
        // Test Embedded (mixed case)
        MongoMode embeddedMixed = MongoMode.fromValue("Embedded");
        assertEquals(MongoMode.EMBEDDED, embeddedMixed, "Embedded (混合大小写) 应该被转换为枚举");
        
        // Test REMOTE (uppercase)
        MongoMode remoteUpper = MongoMode.fromValue("REMOTE");
        assertEquals(MongoMode.REMOTE, remoteUpper, "REMOTE (大写) 应该被转换为枚举");
        
        // Test Remote (mixed case)
        MongoMode remoteMixed = MongoMode.fromValue("Remote");
        assertEquals(MongoMode.REMOTE, remoteMixed, "Remote (混合大小写) 应该被转换为枚举");
    }
    
    /**
     * 测试embedded模式使用默认存储目录
     * Requirement 2.2: 默认存储目录
     */
    @Test
    void testEmbeddedModeUsesDefaultStorageDirectory() {
        // Given: embedded模式，未设置存储目录
        properties.setMode(MongoMode.EMBEDDED);
        
        // When: 验证配置
        assertDoesNotThrow(() -> configuration.validateConfiguration());
        
        // Then: 应该使用默认存储目录
        String defaultDir = properties.getEmbedded().getStorage().getDirectory();
        assertEquals("data/db/mongo", defaultDir,
                "应该使用默认存储目录 data/db/mongo");
    }
}
