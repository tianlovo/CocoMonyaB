package org.xlyo.cocomonyab.config.initializer;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 早期环境文件初始化器
 * <p>
 * 在 Spring Boot 应用启动前执行，确保 .env 文件存在。
 * <p>
 * 执行逻辑：
 * 1. 检查 data/config/.env 是否存在
 * 2. 如果不存在，检查应用同级目录的 .env
 * 3. 如果同级目录有 .env，移动到 data/config/
 * 4. 如果都没有，从 resources 复制 .env.example 到应用同级目录
 * 5. 提示用户填写配置并退出应用
 * <p>
 * 此类在 main 方法中调用，不依赖 Spring 容器。
 * 注意：不使用日志框架，因为此时日志系统还未初始化。
 */
public class EarlyEnvFileInitializer {

    private static final String ENV_FILE = ".env";
    private static final String ENV_EXAMPLE_FILE = ".env.example";
    private static final String RESOURCE_PATH = "template/config/.env.example";
    private static final String CONFIG_DIR = "data/config";

    /**
     * 初始化环境文件
     * 
     * @return true 如果可以继续启动应用，false 如果需要退出应用
     */
    public static boolean initialize() {
        try {
            Path workingDir = Paths.get(System.getProperty("user.dir"));
            Path configDir = workingDir.resolve(CONFIG_DIR);
            Path configEnvFile = configDir.resolve(ENV_FILE);
            Path configEnvExampleFile = configDir.resolve(ENV_EXAMPLE_FILE);
            Path rootEnvFile = workingDir.resolve(ENV_FILE);
            Path rootEnvExampleFile = workingDir.resolve(ENV_EXAMPLE_FILE);

            // 1. 检查 data/config/.env 是否存在
            if (Files.exists(configEnvFile)) {
                System.out.println("✓ 环境配置文件已存在: " + configEnvFile.toAbsolutePath());
                
                // 确保 .env.example 也存在
                ensureEnvExampleExists(configEnvExampleFile);
                
                return true; // 可以继续启动
            }

            // 2. 检查应用同级目录的 .env
            if (Files.exists(rootEnvFile)) {
                System.out.println("检测到应用同级目录的 .env 文件，正在迁移...");
                
                // 创建配置目录
                Files.createDirectories(configDir);
                
                // 移动 .env 到 data/config/
                Files.move(rootEnvFile, configEnvFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✓ .env 文件已迁移到: " + configEnvFile.toAbsolutePath());
                
                // 如果同级目录有 .env.example，也移动过去
                if (Files.exists(rootEnvExampleFile)) {
                    Files.move(rootEnvExampleFile, configEnvExampleFile, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("✓ .env.example 文件已迁移到: " + configEnvExampleFile.toAbsolutePath());
                } else {
                    // 从 resources 复制 .env.example
                    copyEnvExampleFromResources(configEnvExampleFile);
                }
                
                return true; // 可以继续启动
            }

            // 3. 都没有，需要初始化
            System.err.println("=".repeat(80));
            System.err.println("未找到环境配置文件 (.env)");
            System.err.println("=".repeat(80));
            
            // 从 resources 复制 .env.example 到应用同级目录
            copyEnvExampleFromResources(rootEnvExampleFile);
            
            System.err.println();
            System.err.println("已在应用目录创建配置模板文件: " + rootEnvExampleFile.toAbsolutePath());
            System.err.println();
            System.err.println("请按以下步骤操作：");
            System.err.println("  1. 将 .env.example 重命名为 .env");
            System.err.println("  2. 编辑 .env 文件，填写以下必需配置：");
            System.err.println("     - API_ID: Telegram API ID（从 https://my.telegram.org/apps 获取）");
            System.err.println("     - API_HASH: Telegram API Hash");
            System.err.println("     - TG_PHONE: 登录手机号（格式：+8613800138000）");
            System.err.println("     - TG_2FA: 两步验证密码（如果启用了 2FA）");
            System.err.println("  3. 保存文件后重新启动应用");
            System.err.println();
            System.err.println("下次启动时，.env 文件将自动移动到 data/config/ 目录");
            System.err.println("=".repeat(80));
            
            return false; // 需要退出应用
            
        } catch (Exception e) {
            System.err.println("初始化环境文件失败: " + e.getMessage());
            e.printStackTrace();
            return false; // 出错时也退出应用
        }
    }

    /**
     * 确保 .env.example 文件存在
     */
    private static void ensureEnvExampleExists(Path targetFile) {
        try {
            if (!Files.exists(targetFile)) {
                copyEnvExampleFromResources(targetFile);
                System.out.println("✓ 已创建配置模板文件: " + targetFile.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("创建配置模板文件失败: " + e.getMessage());
        }
    }

    /**
     * 从 resources 复制 .env.example 文件到目标位置
     *
     * @param targetFile 目标文件路径
     * @throws IOException 如果复制失败
     */
    private static void copyEnvExampleFromResources(Path targetFile) throws IOException {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        
        if (!resource.exists()) {
            throw new IOException("资源文件不存在: " + RESOURCE_PATH);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
