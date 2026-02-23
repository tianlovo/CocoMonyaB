package org.xlyo.cocomonyab.config.data;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.properties.DataDirectoryProperties;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Data目录管理器
 * <p>
 * 负责管理应用运行时的data目录，采用"释放型数据目录"模式：
 * - 开发环境：在项目根目录创建data目录
 * - 生产环境：在jar包同级目录创建data目录
 * <p>
 * 注意：data 根目录和 config 目录路径固定，不可配置
 * - data 根目录：固定为应用同级的 "data" 目录
 * - config 目录：固定为 "data/config"
 * <p>
 * 配置通过 DataDirectoryProperties 管理
 * <p>
 * 目录结构：
 * <pre>
 * data/
 * ├── config/          # 配置文件目录（固定）
 * ├── db/              # 数据库目录
 * │   └── mongo/       # MongoDB数据存储
 * ├── session/         # 会话数据目录
 * │   └── td/          # Telegram会话数据
 * ├── bin/             # 二进制文件目录
 * │   └── mongo/       # MongoDB二进制文件
 * ├── tmp/             # 临时文件目录
 * └── logs/            # 日志文件目录
 * </pre>
 */
@Slf4j
@Component
public class DataDirectoryManager {

    private static final String DATA_DIR_NAME = "data";
    private static final String CONFIG_DIR_NAME = "config";

    private final DataDirectoryProperties properties;

    /**
     * data目录根路径（固定）
     */
    @Getter
    private Path dataRootPath;

    /**
     * 配置文件目录路径（固定）
     */
    @Getter
    private Path configPath;

    /**
     * 数据库目录路径
     */
    @Getter
    private Path databasePath;

    /**
     * MongoDB数据存储目录路径
     */
    @Getter
    private Path mongoDbPath;

    /**
     * 会话数据目录路径
     */
    @Getter
    private Path sessionPath;

    /**
     * Telegram会话数据目录路径
     */
    @Getter
    private Path telegramSessionPath;

    /**
     * 二进制文件目录路径
     */
    @Getter
    private Path binPath;

    /**
     * MongoDB二进制文件目录路径
     */
    @Getter
    private Path mongoBinPath;

    /**
     * 临时文件目录路径
     */
    @Getter
    private Path tmpPath;

    /**
     * 日志文件目录路径
     */
    @Getter
    private Path logsPath;

    public DataDirectoryManager(DataDirectoryProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        try {
            // 确定data目录的根路径（固定）
            dataRootPath = determineDataRootPath();
            log.info("Data目录根路径: {}", dataRootPath.toAbsolutePath());

            // 初始化各子目录路径
            initializeSubDirectories();

            // 创建所有必要的目录
            createDirectories();

            log.info("Data目录管理器初始化完成");
        } catch (Exception e) {
            log.error("Data目录管理器初始化失败", e);
            throw new IllegalStateException("无法初始化Data目录管理器", e);
        }
    }

    /**
     * 确定data目录的根路径（固定为应用同级的 "data" 目录）
     * <p>
     * 策略：
     * 1. 如果是jar包运行，在jar包同级目录创建data目录
     * 2. 如果是IDE运行，在项目根目录创建data目录
     */
    private Path determineDataRootPath() throws URISyntaxException {
        // 获取应用的运行位置
        File jarFile = new File(
            DataDirectoryManager.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
        );

        Path basePath;
        if (jarFile.isFile()) {
            // jar包运行：使用jar包所在目录
            basePath = jarFile.getParentFile().toPath();
            log.info("检测到jar包运行模式，jar路径: {}", jarFile.getAbsolutePath());
        } else {
            // IDE运行：使用项目根目录（当前工作目录）
            basePath = Paths.get(System.getProperty("user.dir"));
            log.info("检测到IDE运行模式，工作目录: {}", basePath.toAbsolutePath());
        }

        return basePath.resolve(DATA_DIR_NAME);
    }

    /**
     * 初始化各子目录路径
     */
    private void initializeSubDirectories() {
        // config 目录固定为 "config"
        configPath = dataRootPath.resolve(CONFIG_DIR_NAME);
        
        // 其他目录可配置
        databasePath = dataRootPath.resolve(properties.getDatabaseDirectory());
        mongoDbPath = dataRootPath.resolve(properties.getMongoDbDirectory());
        sessionPath = dataRootPath.resolve(properties.getSessionDirectory());
        telegramSessionPath = dataRootPath.resolve(properties.getTelegramSessionDirectory());
        binPath = dataRootPath.resolve(properties.getBinDirectory());
        mongoBinPath = dataRootPath.resolve(properties.getMongoBinDirectory());
        tmpPath = dataRootPath.resolve(properties.getTmpDirectory());
        logsPath = dataRootPath.resolve(properties.getLogsDirectory());
    }

    /**
     * 创建所有必要的目录
     */
    private void createDirectories() throws IOException {
        createDirectoryIfNotExists(dataRootPath, "Data根目录");
        createDirectoryIfNotExists(configPath, "配置文件目录");
        createDirectoryIfNotExists(databasePath, "数据库目录");
        createDirectoryIfNotExists(mongoDbPath, "MongoDB数据目录");
        createDirectoryIfNotExists(sessionPath, "会话数据目录");
        createDirectoryIfNotExists(telegramSessionPath, "Telegram会话目录");
        
        // 创建 Telegram 子目录
        createDirectoryIfNotExists(telegramSessionPath.resolve("data"), "Telegram数据库目录");
        createDirectoryIfNotExists(telegramSessionPath.resolve("downloads"), "Telegram下载目录");
        
        createDirectoryIfNotExists(binPath, "二进制文件目录");
        createDirectoryIfNotExists(mongoBinPath, "MongoDB二进制目录");
        createDirectoryIfNotExists(tmpPath, "临时文件目录");
        createDirectoryIfNotExists(logsPath, "日志文件目录");
    }

    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(Path path, String description) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("创建{}: {}", description, path.toAbsolutePath());
        } else {
            log.debug("{}已存在: {}", description, path.toAbsolutePath());
        }
    }

    /**
     * 获取相对于data根目录的路径字符串
     *
     * @param subPath 子路径
     * @return 完整路径字符串
     */
    public String getPath(String subPath) {
        return dataRootPath.resolve(subPath).toString();
    }

    /**
     * 获取相对于data根目录的Path对象
     *
     * @param subPath 子路径
     * @return Path对象
     */
    public Path resolvePath(String subPath) {
        return dataRootPath.resolve(subPath);
    }

    /**
     * 获取MongoDB测试数据库目录路径（用于测试环境）
     *
     * @param testName 测试名称
     * @return MongoDB测试数据库目录路径
     */
    public String getMongoTestDbPath(String testName) {
        return databasePath.resolve("mongo-" + testName).toString();
    }
}
