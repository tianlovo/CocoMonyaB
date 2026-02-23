package org.xlyo.cocomonyab.config.initializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 环境配置文件初始化器
 * 在应用启动时检查并复制 .env.example 文件到 data/config 目录
 */
@Slf4j
@Component
@Order(1) // 确保在其他初始化器之前执行
public class EnvFileInitializer implements ApplicationRunner {

    private static final String TARGET_DIR = "data/config";
    private static final String ENV_EXAMPLE_FILE = ".env.example";
    private static final String RESOURCE_PATH = "template/config/.env.example";

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializeEnvExampleFile();
        } catch (Exception e) {
            log.error("初始化环境配置文件失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }

    /**
     * 初始化 .env.example 文件
     * 如果 data/config/.env.example 不存在，则从 resources 复制
     */
    private void initializeEnvExampleFile() throws IOException {
        // 确保目标目录存在
        Path targetDir = Paths.get(TARGET_DIR);
        if (!Files.exists(targetDir)) {
            log.info("创建配置目录: {}", targetDir.toAbsolutePath());
            Files.createDirectories(targetDir);
        }

        // 检查目标文件是否存在
        Path targetFile = targetDir.resolve(ENV_EXAMPLE_FILE);
        if (Files.exists(targetFile)) {
            log.debug("环境配置示例文件已存在: {}", targetFile.toAbsolutePath());
            return;
        }

        // 从 resources 复制文件
        log.info("环境配置示例文件不存在，开始复制...");
        copyEnvExampleFromResources(targetFile);
        log.info("环境配置示例文件复制成功: {}", targetFile.toAbsolutePath());
    }

    /**
     * 从 resources 复制 .env.example 文件到目标位置
     *
     * @param targetFile 目标文件路径
     * @throws IOException 如果复制失败
     */
    private void copyEnvExampleFromResources(Path targetFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        
        if (!resource.exists()) {
            log.warn("资源文件不存在: {}", RESOURCE_PATH);
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
