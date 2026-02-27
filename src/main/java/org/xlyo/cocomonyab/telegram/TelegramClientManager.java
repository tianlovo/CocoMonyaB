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
import org.xlyo.cocomonyab.telegram.handler.TgUpdateNewChatHandler;
import org.xlyo.cocomonyab.telegram.handler.TgUpdateChatPositionHandler;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
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
    private final TgUpdateNewChatHandler updateNewChatHandler;
    private final TgUpdateChatPositionHandler updateChatPositionHandler;
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

        // 校验 TG_2FA（可选，但给出警告）
        String tg2fa = envProperties.getTg2fa();
        if (tg2fa == null || tg2fa.isBlank() || "your_2fa_password_if_enabled".equals(tg2fa)) {
            log.warn("⚠️  警告: TG_2FA 未填写或仍为示例值");
            log.warn("若您的 Telegram 账号启用了两步验证（2FA），登录时需要输入密码");
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
     * 通过控制台交互选择登录方式
     */
    private TelegramProperties.LoginType selectLoginType() {
        log.info("=".repeat(60));
        log.info("请选择 Telegram 登录方式:");
        log.info("=".repeat(60));
        log.info("1. 验证码登录 (推荐)");
        log.info("   - 验证码发送到其他已登录的 Telegram 设备");
        log.info("   - 如果配置了手机号会自动使用");
        log.info("   - 快速便捷");
        log.info("");
        log.info("2. 二维码登录");
        log.info("   - 在控制台显示二维码，使用手机扫码登录");
        log.info("   - 无需配置手机号");
        log.info("   - 适合首次登录");
        log.info("=".repeat(60));
        
        try (var scanner = new java.util.Scanner(System.in)) {
            while (true) {
                System.out.print("请输入选项 (1/2) [默认: 1]: ");
                String input = scanner.nextLine().trim();
                
                // 默认选择验证码登录
                if (input.isEmpty()) {
                    input = "1";
                }
                
                switch (input) {
                    case "1":
                        log.info("✓ 已选择: 验证码登录");
                        return TelegramProperties.LoginType.CODE;
                    case "2":
                        log.info("✓ 已选择: 二维码登录");
                        return TelegramProperties.LoginType.QRCODE;
                    default:
                        log.warn("⚠️  无效的选项，请输入 1 或 2");
                }
            }
        }
    }

    /**
     * 执行登录
     */
    private void login() {
        log.info("开始 Telegram 登录流程...");
        log.info("");
        
        // 使用 DataDirectoryManager 获取目录
        String databaseDir = dataDirectoryManager.getTelegramSessionPath().resolve("data").toString();
        String downloadDir = dataDirectoryManager.getTelegramSessionPath().resolve("downloads").toString();
        
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
            
            log.info("数据库目录: {}", databaseDir);
            log.info("下载目录: {}", downloadDir);
            log.info("");
            
            // 5. 尝试自动登录（如果有有效的 session）
            log.info("检查是否存在有效的登录会话...");
            if (tryAutoLogin(settings, apiToken)) {
                log.info("✓ 使用已保存的会话自动登录成功！");
                return;
            }
            
            // 6. 自动登录失败，需要手动登录
            log.info("未找到有效的登录会话，需要手动登录");
            log.info("");
            
            // 7. 通过控制台交互选择登录方式
            TelegramProperties.LoginType loginType = selectLoginType();
            log.info("");
            
            // 8. 验证手机号配置（如果需要）
            if (loginType != TelegramProperties.LoginType.QRCODE) {
                String tgPhone = envProperties.getTgPhone();
                if (tgPhone == null || tgPhone.isBlank() || 
                    "+8613800138000".equals(tgPhone) || "your_phone_number".equals(tgPhone)) {
                    log.warn("⚠️  警告: 未配置有效的手机号");
                    log.warn("当前登录方式需要手机号，将在登录过程中提示输入");
                } else if (!PHONE_PATTERN.matcher(tgPhone).matches()) {
                    log.warn("⚠️  警告: 配置的手机号格式可能不正确: {}", tgPhone);
                    log.warn("正确格式: +[国家代码][手机号]，例如: +8613800138000");
                }
            }
            
            // 9. 打印配置信息（脱敏）
            log.info("API Hash: {}***", envProperties.getApiHash().substring(0, 8));
            if (loginType != TelegramProperties.LoginType.QRCODE) {
                String tgPhone = envProperties.getTgPhone();
                if (tgPhone != null && !tgPhone.isBlank() && 
                    !"+8613800138000".equals(tgPhone) && !"your_phone_number".equals(tgPhone)) {
                    log.info("手机号: {}", maskPhone(tgPhone));
                }
            }
            log.info("设备型号: {}", telegramProperties.getDeviceModel());
            log.info("");
            
            // 10. 准备构建器
            log.info("构建 Telegram 客户端...");
            SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
            
            // 11. 注册监听器
            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onAuthStateUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, updateNewMessageHandler::onNewMessageUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateNewChat.class, updateNewChatHandler::onNewChatUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateChatPosition.class, updateChatPositionHandler::onChatPositionUpdate);

            // 12. 根据登录方式设置客户端交互
            ClientInteraction clientInteraction = switch (loginType) {
                case CODE -> new TgAutoClientInteraction(envProperties);
                case QRCODE -> new TgQrCodeClientInteraction(envProperties);
            };
            clientBuilder.setClientInteraction(clientInteraction);
            
            // 13. 创建认证数据
            TgAutoAuthenticationData authData = switch (loginType) {
                case CODE -> {
                    String phone = envProperties.getTgPhone();
                    // 如果没有配置有效的手机号，使用空字符串，让 ClientInteraction 处理
                    if (phone == null || phone.isBlank() || 
                        "+8613800138000".equals(phone) || "your_phone_number".equals(phone)) {
                        phone = "";
                    }
                    yield TgAutoAuthenticationData.phoneNumber(phone);
                }
                case QRCODE -> TgAutoAuthenticationData.qrCode();
            };
            
            // 14. 构建并启动客户端
            log.info("正在连接 Telegram...");
            client = clientBuilder.build(authData);
            
            // 15. 等待登录完成（使用配置的超时时间）
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
     * 尝试自动登录（使用已保存的 session）
     */
    private boolean tryAutoLogin(TDLibSettings settings, APIToken apiToken) {
        try {
            // 创建一个临时的客户端尝试自动登录
            SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
            
            // 注册监听器
            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onAuthStateUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, updateNewMessageHandler::onNewMessageUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateNewChat.class, updateNewChatHandler::onNewChatUpdate);
            clientBuilder.addUpdateHandler(TdApi.UpdateChatPosition.class, updateChatPositionHandler::onChatPositionUpdate);
            
            // 使用一个不需要交互的 ClientInteraction
            clientBuilder.setClientInteraction(new SilentClientInteraction());
            
            // 使用空的认证数据（依赖已保存的 session）
            TgAutoAuthenticationData authData = TgAutoAuthenticationData.phoneNumber("");
            
            // 构建客户端
            client = clientBuilder.build(authData);
            
            // 尝试获取当前用户信息（短超时）
            currentUser = client.getMeAsync().get(5, TimeUnit.SECONDS);
            
            // 如果成功获取到用户信息，说明自动登录成功
            return currentUser != null;
            
        } catch (Exception e) {
            // 自动登录失败，清理资源
            log.debug("自动登录失败: {}", e.getMessage());
            if (client != null) {
                try {
                    client.sendClose();
                } catch (Exception ex) {
                    // 忽略关闭异常
                }
                client = null;
            }
            currentUser = null;
            return false;
        }
    }
    
    /**
     * 静默的客户端交互（用于自动登录尝试）
     */
    private static class SilentClientInteraction implements ClientInteraction {
        @Override
        public CompletableFuture<String> onParameterRequest(InputParameter parameter, ParameterInfo parameterInfo) {
            // 不提供任何交互，直接失败
            return CompletableFuture.failedFuture(new UnsupportedOperationException("自动登录失败，需要手动登录"));
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
