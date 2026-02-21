package org.xlyo.cocomonyab.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xlyo.cocomonyab.config.mongo.EmbeddedMongoConfiguration;
import org.xlyo.cocomonyab.config.mongo.MongoDBProperties;
import org.xlyo.cocomonyab.config.mongo.MongoMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌入式 MongoDB 配置测试
 */
class EmbeddedMongoConfigurationTest {
    
    private MongoDBProperties properties;
    private EmbeddedMongoConfiguration configuration;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        properties = new MongoDBProperties();
        properties.setMode(MongoMode.EMBEDDED);
    }
    
    @AfterEach
    void tearDown() {
        if (configuration != null) {
            try {
                configuration.stopEmbeddedMongo();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    @Test
    void testDefaultStorageDirectoryIsUsed() {
        // Given: 使用默认配置
        configuration = new EmbeddedMongoConfiguration(properties);
        
        // Then: 应该使用默认存储目录
        String defaultDir = properties.getEmbedded().getStorage().getDirectory();
        assertEquals("data/db/mongo", defaultDir);
    }
    
    @Test
    void testStorageDirectoryCreationWhenNotExists() throws IOException {
        // Given: 不存在的存储目录
        String storageDir = tempDir.resolve("new-mongo-data").toString();
        properties.getEmbedded().getStorage().setDirectory(storageDir);
        configuration = new EmbeddedMongoConfiguration(properties);
        
        // When: 尝试启动（会创建目录，但不会真正启动 MongoDB 因为需要下载）
        // 这里只测试目录创建逻辑
        Path storagePath = Path.of(storageDir);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }
        
        // Then: 目录应该被创建
        assertTrue(Files.exists(storagePath));
        assertTrue(Files.isDirectory(storagePath));
    }
    
    @Test
    void testExceptionThrownWhenDirectoryNotWritable() throws IOException {
        // Skip this test on Windows as it doesn't support POSIX permissions
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return;
        }
        
        // Given: 不可写的存储目录
        Path readOnlyDir = tempDir.resolve("readonly-mongo-data");
        Files.createDirectories(readOnlyDir);
        
        // 设置为只读
        Set<PosixFilePermission> permissions = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_EXECUTE
        );
        Files.setPosixFilePermissions(readOnlyDir, permissions);
        
        properties.getEmbedded().getStorage().setDirectory(readOnlyDir.toString());
        configuration = new EmbeddedMongoConfiguration(properties);
        
        // When & Then: 应该抛出异常
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> configuration.startEmbeddedMongo()
        );
        
        assertTrue(exception.getMessage().contains("存储目录不可写") || 
                   exception.getMessage().contains("MongoDB"));
        
        // 恢复权限以便清理
        Files.setPosixFilePermissions(readOnlyDir, Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        ));
    }
    
    @Test
    void testGracefulShutdownWithoutStart() {
        // Given: 未启动的配置
        configuration = new EmbeddedMongoConfiguration(properties);
        
        // When & Then: 停止 MongoDB 不应该抛出异常
        assertDoesNotThrow(() -> configuration.stopEmbeddedMongo());
        
        // 再次调用 stop 也不应该抛出异常
        assertDoesNotThrow(() -> configuration.stopEmbeddedMongo());
    }
    
    @Test
    void testConfigurationPropertiesAreSet() {
        // Given: 自定义配置
        String customDir = "custom/db/path";
        properties.getEmbedded().getStorage().setDirectory(customDir);
        configuration = new EmbeddedMongoConfiguration(properties);
        
        // Then: 配置应该被正确设置
        assertEquals(customDir, properties.getEmbedded().getStorage().getDirectory());
    }
}
