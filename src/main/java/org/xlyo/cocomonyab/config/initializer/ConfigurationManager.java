package org.xlyo.cocomonyab.config.initializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.data.DataDirectoryManager;
import org.xlyo.cocomonyab.config.properties.TgEnvProperties;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import java.util.regex.Pattern;

/**
 * 配置管理器
 * <p>
 * 负责应用启动时的配置初始化阶段，包括：
 * <ul>
 *   <li>验证必需配置项（API_ID、API_HASH、TG_PHONE）</li>
 *   <li>初始化数据目录管理器</li>
 *   <li>发布配置就绪事件</li>
 * </ul>
 * </p>
 * <p>
 * 这是启动流程的第一个阶段，所有后续阶段都依赖于配置初始化的完成
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationManager {
    
    private static final Pattern API_ID_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern API_HASH_PATTERN = Pattern.compile("^[a-f0-9]{32}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+\\d{10,15}$");
    
    private final DataDirectoryManager dataDirectoryManager;
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    private final TgEnvProperties envProperties;
    
    /**
     * 初始化配置
     * <p>
     * 在 Spring 容器完成依赖注入后自动执行
     * 执行配置验证、数据目录初始化，并发布配置就绪事件
     * </p>
     */
    @PostConstruct
    public void initialize() {
        progressTracker.startPhase("配置初始化");
        
        try {
            log.info("开始配置初始化...");
            
            // 1. 验证配置
            validateConfiguration();
            
            // 2. 初始化数据目录（DataDirectoryManager 已经在其 @PostConstruct 中初始化）
            // 这里只需要确认初始化成功
            log.info("数据目录已初始化: {}", dataDirectoryManager.getDataRootPath());
            
            // 3. 发布配置就绪事件
            eventPublisher.publishConfigurationReady();
            
            progressTracker.completePhase("配置初始化");
            log.info("✅ 配置初始化完成");
            
        } catch (Exception e) {
            progressTracker.failPhase("配置初始化", e.getMessage());
            log.error("❌ 配置初始化失败", e);
            throw new StartupException("配置初始化失败", e);
        }
    }
    
    /**
     * 验证必需配置项
     * <p>
     * 验证 API_ID、API_HASH、TG_PHONE 的存在性和有效性
     * 如果任何必需配置项缺失或无效，抛出异常终止启动
     * </p>
     */
    private void validateConfiguration() {
        log.info("开始验证配置...");
        boolean hasError = false;
        
        // 验证 API_ID
        String apiId = envProperties.getApiId();
        if (apiId == null || apiId.isBlank()) {
            log.error("❌ 配置验证失败: API_ID 未设置");
            log.error("请在配置目录的 .env 文件中添加: API_ID=your_api_id");
            log.error("示例: API_ID=12345678");
            log.error("获取地址: https://my.telegram.org/apps");
            hasError = true;
        } else if ("your_api_id".equals(apiId)) {
            log.error("❌ 配置验证失败: API_ID 仍为示例值");
            log.error("请将配置目录的 .env 文件中的 API_ID 修改为真实值");
            log.error("获取地址: https://my.telegram.org/apps");
            hasError = true;
        } else if (!API_ID_PATTERN.matcher(apiId).matches()) {
            log.error("❌ 配置验证失败: API_ID 格式错误");
            log.error("API_ID 应为纯数字，当前值: {}", apiId);
            log.error("示例: API_ID=12345678");
            hasError = true;
        }
        
        // 验证 API_HASH
        String apiHash = envProperties.getApiHash();
        if (apiHash == null || apiHash.isBlank()) {
            log.error("❌ 配置验证失败: API_HASH 未设置");
            log.error("请在配置目录的 .env 文件中添加: API_HASH=your_api_hash");
            log.error("示例: API_HASH=0123456789abcdef0123456789abcdef");
            log.error("获取地址: https://my.telegram.org/apps");
            hasError = true;
        } else if ("your_api_hash".equals(apiHash)) {
            log.error("❌ 配置验证失败: API_HASH 仍为示例值");
            log.error("请将配置目录的 .env 文件中的 API_HASH 修改为真实值");
            log.error("获取地址: https://my.telegram.org/apps");
            hasError = true;
        } else if (!API_HASH_PATTERN.matcher(apiHash).matches()) {
            log.error("❌ 配置验证失败: API_HASH 格式错误");
            log.error("API_HASH 应为32位十六进制字符，当前长度: {}", apiHash.length());
            log.error("示例: API_HASH=0123456789abcdef0123456789abcdef");
            hasError = true;
        }
        
        // 验证 TG_PHONE
        String tgPhone = envProperties.getTgPhone();
        if (tgPhone == null || tgPhone.isBlank()) {
            log.error("❌ 配置验证失败: TG_PHONE 未设置");
            log.error("请在配置目录的 .env 文件中添加: TG_PHONE=your_phone_number");
            log.error("示例: TG_PHONE=+8613800138000");
            hasError = true;
        } else if ("your_phone_number".equals(tgPhone)) {
            log.error("❌ 配置验证失败: TG_PHONE 仍为示例值");
            log.error("请将配置目录的 .env 文件中的 TG_PHONE 修改为真实值");
            log.error("示例: TG_PHONE=+8613800138000");
            hasError = true;
        } else if (!PHONE_PATTERN.matcher(tgPhone).matches()) {
            log.error("❌ 配置验证失败: TG_PHONE 格式错误");
            log.error("TG_PHONE 应为 E.164 格式（以 + 开头，后跟10-15位数字），当前值: {}", tgPhone);
            log.error("示例: TG_PHONE=+8613800138000");
            hasError = true;
        }
        
        if (hasError) {
            throw new IllegalStateException("配置验证失败，请检查上述错误信息");
        }
        
        log.info("✅ 配置验证通过");
    }
}
