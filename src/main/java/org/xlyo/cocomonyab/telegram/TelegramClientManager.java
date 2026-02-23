package org.xlyo.cocomonyab.telegram;

import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.data.DataDirectoryManager;
import org.xlyo.cocomonyab.config.properties.TelegramProperties;
import org.xlyo.cocomonyab.config.properties.TgEnvProperties;
import org.xlyo.cocomonyab.telegram.handler.TgUpdateNewMessageHandler;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Telegram 客户端管理器（全局单例）
 * 负责配置验证、客户端初始化、自动登录和资源释放
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramClientManager {

    // API_ID 格式：纯数字
    private static final Pattern API_ID_PATTERN = Pattern.compile("^\\d+$");

    // API_HASH 格式：32位十六进制字符
    private static final Pattern API_HASH_PATTERN = Pattern.compile("^[a-f0-9]{32}$");

    // 手机号格式：以 + 开头，后跟数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+\\d{10,15}$");

    private final TgEnvProperties envProperties;
    private final TelegramProperties telegramProperties;
    private final TgUpdateNewMessageHandler updateNewMessageHandler;
    private final DataDirectoryManager dataDirectoryManager;

    private SimpleTelegramClient client;
    
    private SimpleTelegramClientFactory clientFactory;
    
    private TdApi.User currentUser;

    /**
     * 组件初始化时自动执行配置验证和登录
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化 Telegram 客户端管理器...");
        
        // 1. 验证配置
        validateConfiguration();
        
        // 2. 自动登录
        login();
    }

    /**
     * 验证 Telegram 配置
     */
    private void validateConfiguration() {
        log.info("开始校验 Telegram 配置...");
        boolean hasError = false;

        // 校验 API_ID
        String apiId = envProperties.getApiId();
        if (apiId == null || apiId.isBlank()) {
            log.error("❌ 配置验证失败: API_ID 未设置");
            log.error("请在配置目录的 .env 文件中添加: API_ID=your_api_id");
            log.error("示例: API_ID=12345678");
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

        // 校验 API_HASH
        String apiHash = envProperties.getApiHash();
        if (apiHash == null || apiHash.isBlank()) {
            log.error("❌ 配置验证失败: API_HASH 未设置");
            log.error("请在配置目录的 .env 文件中添加: API_HASH=your_api_hash");
            log.error("示例: API_HASH=0123456789abcdef0123456789abcdef");
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

        // 校验 TG_PHONE
        String tgPhone = envProperties.getTgPhone();
        if (tgPhone == null || tgPhone.isBlank()) {
            log.error("❌ 配置验证失败: TG_PHONE 未设置");
            log.error("⚠️  本项目只能使用手机号登录，不支持二维码或其他方式登录");
            log.error("请在配置目录的 .env 文件中添加: TG_PHONE=+8613800138000");
            hasError = true;
        } else if ("+8613800138000".equals(tgPhone) || "your_phone_number".equals(tgPhone)) {
            log.error("❌ 配置验证失败: TG_PHONE 仍为示例值");
            log.error("请将配置目录的 .env 文件中的 TG_PHONE 修改为真实手机号");
            log.error("格式: +[国家代码][手机号]，例如: +8613800138000");
            hasError = true;
        } else if (!PHONE_PATTERN.matcher(tgPhone).matches()) {
            log.error("❌ 配置验证失败: TG_PHONE 格式错误");
            log.error("手机号应以 + 开头，后跟国家代码和手机号，当前值: {}", tgPhone);
            log.error("示例: TG_PHONE=+8613800138000");
            hasError = true;
        }

        // 校验 TG_2FA（可选，但给出警告）
        String tg2fa = envProperties.getTg2fa();
        if (tg2fa == null || tg2fa.isBlank() || "your_2fa_password_if_enabled".equals(tg2fa)) {
            log.warn("⚠️  警告: TG_2FA 未填写或仍为示例值");
            log.warn("若您的 Telegram 账号启用了两步验证（2FA），必须填写此密码，否则无法登录");
            log.warn("如未启用 2FA，可忽略此警告");
        }

        if (hasError) {
            log.error("\n❌ Telegram 配置校验失败，应用即将退出");
            log.error("请检查配置目录的 .env 文件中的配置");
            log.error("如果文件不存在，请复制 .env.example 并重命名为 .env");
            System.exit(1);
        }

        log.info("✅ Telegram 配置校验通过");
    }

    /**
     * 执行登录
     */
    private void login() {
        log.info("开始 Telegram 自动登录流程...");
        
        // 打印配置信息（脱敏）
        log.info("API Hash: {}***", envProperties.getApiHash().substring(0, 8));
        log.info("手机号: {}", maskPhone(envProperties.getTgPhone()));
        log.info("设备型号: {}", telegramProperties.getDeviceModel());
        
        // 使用 DataDirectoryManager 获取目录
        String databaseDir = dataDirectoryManager.getTelegramSessionPath().resolve("data").toString();
        String downloadDir = dataDirectoryManager.getTelegramSessionPath().resolve("downloads").toString();
        
        log.info("数据库目录: {}", databaseDir);
        log.info("下载目录: {}", downloadDir);
        
        try {
            // 1. 初始化 TDLight 原生库
            log.info("初始化 TDLight 原生库...");
            Init.init();
            
            // 2. 创建工厂（全局单例，用于管理多个客户端）
            log.info("创建 Telegram 客户端工厂...");
            clientFactory = new SimpleTelegramClientFactory();
            
            // 3. 配置 API Token
            int apiId = Integer.parseInt(envProperties.getApiId());
            String apiHash = envProperties.getApiHash();
            APIToken apiToken = new APIToken(apiId, apiHash);
            
            // 4. 配置 TDLib 设置
            log.info("配置 TDLib 设置...");
            TDLibSettings settings = TDLibSettings.create(apiToken);
            
            settings.setUseTestDatacenter(false);
            settings.setFileDatabaseEnabled(true);
            settings.setChatInfoDatabaseEnabled(true);
            settings.setMessageDatabaseEnabled(true);
            
            settings.setSystemLanguageCode(Locale.getDefault().getLanguage());
            settings.setDeviceModel(telegramProperties.getDeviceModel());
            
            // 数据库路径：实现自动登录的关键（session 持久化）
            var databasePath = Paths.get(databaseDir).toAbsolutePath();
            var downloadPath = Paths.get(downloadDir).toAbsolutePath();
            
            settings.setDatabaseDirectoryPath(databasePath);
            settings.setDownloadedFilesDirectoryPath(downloadPath);
            
            // 5. 准备构建器
            log.info("构建 Telegram 客户端...");
            SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
            
            // 6. 注册监听器
            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onAuthStateUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, updateNewMessageHandler::onNewMessageUpdate);

            // 7. 设置自定义客户端交互（处理验证码和密码输入）
            TgAutoClientInteraction clientInteraction = new TgAutoClientInteraction(envProperties);
            clientBuilder.setClientInteraction(clientInteraction);
            
            // 8. 创建认证数据（使用手机号登录）
            TgAutoAuthenticationData authData = TgAutoAuthenticationData.phoneNumber(envProperties.getTgPhone());
            
            // 9. 构建并启动客户端
            log.info("正在连接 Telegram...");
            client = clientBuilder.build(authData);
            
            // 10. 等待登录完成（使用配置的超时时间）
            int timeoutMinutes = telegramProperties.getLoginTimeoutMinutes();
            currentUser = client.getMeAsync().get(timeoutMinutes, TimeUnit.MINUTES);
            
            log.info("=".repeat(60));
            log.info("✓ Telegram 登录成功！");
            log.info("用户名称: {} {}", currentUser.firstName, currentUser.lastName != null ? currentUser.lastName : "");
            log.info("用户 ID: {}", currentUser.id);
            if (currentUser.usernames != null && currentUser.usernames.activeUsernames != null && currentUser.usernames.activeUsernames.length > 0) {
                log.info("用户名: @{}", currentUser.usernames.activeUsernames[0]);
            }
            log.info("=".repeat(60));
            
        } catch (Exception e) {
            log.error("Telegram 登录失败", e);
            throw new RuntimeException("Telegram 登录失败", e);
        }
    }

    /**
     * 认证状态更新处理
     */
    private void onAuthStateUpdate(TdApi.UpdateAuthorizationState update) {
        switch (update.authorizationState) {
            case TdApi.AuthorizationStateReady ready -> 
                log.info("状态: 运行中 (Ready)");
            case TdApi.AuthorizationStateWaitCode waitCode -> 
                log.info("提示: 请查看你的其他 Telegram 设备获取验证码");
            case TdApi.AuthorizationStateWaitPassword waitPassword -> 
                log.info("提示: 需要输入两步验证密码");
            case TdApi.AuthorizationStateWaitPhoneNumber waitPhone -> 
                log.info("状态: 等待手机号");
            case TdApi.AuthorizationStateWaitTdlibParameters waitParams -> 
                log.info("状态: 等待 TDLib 参数");
            default -> 
                log.debug("认证状态: {}", update.authorizationState.getClass().getSimpleName());
        }
    }

    /**
     * 脱敏手机号显示
     * 例如: +8613800138000 -> +861380****000
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        int len = phone.length();
        return phone.substring(0, len - 7) + "****" + phone.substring(len - 3);
    }

    /**
     * 系统关闭时自动登出并释放资源
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭 Telegram 客户端管理器...");
        
        try {
            if (client != null) {
                log.info("正在登出 Telegram...");
                client.sendClose();
                log.info("Telegram 客户端已关闭");
            }
            
            if (clientFactory != null) {
                log.info("正在释放客户端工厂资源...");
                clientFactory.close();
                log.info("客户端工厂资源已释放");
            }
            
            log.info("✓ Telegram 客户端管理器已成功关闭");
        } catch (Exception e) {
            log.error("关闭 Telegram 客户端时发生错误", e);
        }
    }

    /**
     * 检查客户端是否已初始化并登录
     */
    public boolean isReady() {
        return client != null && currentUser != null;
    }

    /**
     * 获取当前登录用户信息
     */
    public TdApi.User getCurrentUser() {
        if (!isReady()) {
            throw new IllegalStateException("Telegram 客户端尚未初始化或登录");
        }
        return currentUser;
    }

    /**
     * 获取 Telegram 客户端
     */
    public SimpleTelegramClient getClient() {
        if (!isReady()) {
            throw new IllegalStateException("Telegram 客户端尚未初始化或登录");
        }
        return client;
    }
}
