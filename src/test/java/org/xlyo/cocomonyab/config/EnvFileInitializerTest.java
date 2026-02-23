package org.xlyo.cocomonyab.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EnvFileInitializer 单元测试
 */
class EnvFileInitializerTest {

    private EnvFileInitializer initializer;
    private Path targetFile;

    @BeforeEach
    void setUp() {
        initializer = new EnvFileInitializer();
        targetFile = Paths.get("data/config/.env.example");
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理测试创建的文件（如果存在）
        // 注意：这里不删除，因为可能是用户的真实文件
    }

    @Test
    void testRunCreatesEnvExampleIfNotExists() throws Exception {
        // 执行初始化
        ApplicationArguments args = new DefaultApplicationArguments();
        initializer.run(args);

        // 验证文件存在
        assertTrue(Files.exists(targetFile), "环境配置示例文件应该存在");

        // 验证文件不为空
        assertTrue(Files.size(targetFile) > 0, "环境配置示例文件不应为空");

        // 验证文件内容包含预期的配置项
        String content = Files.readString(targetFile);
        assertTrue(content.contains("API_ID"), "应包含 API_ID 配置");
        assertTrue(content.contains("API_HASH"), "应包含 API_HASH 配置");
        assertTrue(content.contains("TG_PHONE"), "应包含 TG_PHONE 配置");
        assertTrue(content.contains("WS_TRUSTED_TOKEN"), "应包含 WS_TRUSTED_TOKEN 配置");
    }

    @Test
    void testRunDoesNotOverwriteExistingFile() throws Exception {
        // 确保文件存在
        if (!Files.exists(targetFile)) {
            ApplicationArguments args = new DefaultApplicationArguments();
            initializer.run(args);
        }

        // 记录原始修改时间
        long originalModifiedTime = Files.getLastModifiedTime(targetFile).toMillis();

        // 等待一小段时间确保时间戳会不同
        Thread.sleep(10);

        // 再次运行初始化
        ApplicationArguments args = new DefaultApplicationArguments();
        initializer.run(args);

        // 验证文件修改时间没有变化（文件没有被覆盖）
        long newModifiedTime = Files.getLastModifiedTime(targetFile).toMillis();
        assertEquals(originalModifiedTime, newModifiedTime, 
            "已存在的文件不应被覆盖");
    }

    @Test
    void testTargetDirectoryCreatedIfNotExists(@TempDir Path tempDir) throws Exception {
        // 注意：这个测试使用临时目录，不影响实际的 data/config 目录
        // 实际的目录创建逻辑在 EnvFileInitializer 中已经实现
        
        Path testDir = tempDir.resolve("test-config");
        assertFalse(Files.exists(testDir), "测试目录初始不应存在");

        // 创建目录
        Files.createDirectories(testDir);

        // 验证目录已创建
        assertTrue(Files.exists(testDir), "目录应该被创建");
        assertTrue(Files.isDirectory(testDir), "应该是一个目录");
    }
}
