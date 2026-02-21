package org.xlyo.cocomonyab.listener;

import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.TelegramProperties;
import org.xlyo.cocomonyab.config.TgEnvProperties;
import org.xlyo.cocomonyab.telegram.TgAutoAuthenticationData;
import org.xlyo.cocomonyab.telegram.TgAutoClientInteraction;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Telegram 自动登录监听器
 * 在配置验证完成后自动执行登录流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // 确保在 TgConfigValidationListener (Order=1) 之后执行
public class TgAutoLoginListener implements ApplicationListener<@NonNull ContextRefreshedEvent> {

    private final TgEnvProperties envProperties;
    private final TelegramProperties telegramProperties;
    
    private SimpleTelegramClient client;
    private SimpleTelegramClientFactory clientFactory;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("开始 Telegram 自动登录流程...");
        
        // 打印配置信息（脱敏）
        log.info("手机号: {}", maskPhone(envProperties.getTgPhone()));
        log.info("设备型号: {}", telegramProperties.getDeviceModel());
        log.info("数据库目录: {}", telegramProperties.getDatabaseDirectory());
        log.info("下载目录: {}", telegramProperties.getDownloadDirectory());
        
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
            
            // 数据库路径：实现自动登录的关键（session 持久化）
            var databasePath = Paths.get(telegramProperties.getDatabaseDirectory()).toAbsolutePath();
            var downloadPath = Paths.get(telegramProperties.getDownloadDirectory()).toAbsolutePath();
            
            settings.setDatabaseDirectoryPath(databasePath);
            settings.setDownloadedFilesDirectoryPath(downloadPath);
            
            settings.setUseTestDatacenter(false);
            settings.setFileDatabaseEnabled(true);
            settings.setChatInfoDatabaseEnabled(true);
            settings.setMessageDatabaseEnabled(true);
            
            settings.setSystemLanguageCode(Locale.US.getDisplayLanguage());
            settings.setDeviceModel(telegramProperties.getDeviceModel());
            
            // 5. 准备构建器
            log.info("构建 Telegram 客户端...");
            SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
            
            // 6. 注册认证状态监听器
            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onAuthStateUpdate);
            
            // 7. 设置自定义客户端交互（处理验证码和密码输入）
            TgAutoClientInteraction clientInteraction = new TgAutoClientInteraction(envProperties);
            clientBuilder.setClientInteraction(clientInteraction);
            
            // 8. 创建认证数据（使用手机号登录）
            TgAutoAuthenticationData authData = TgAutoAuthenticationData.phoneNumber(envProperties.getTgPhone());
            
            // 9. 构建并启动客户端
            log.info("正在连接 Telegram...");
            log.info("提示: 验证码将发送到你的其他已登录 Telegram 设备");
            client = clientBuilder.build(authData);
            
            // 10. 等待登录完成（使用配置的超时时间）
            int timeoutMinutes = telegramProperties.getLoginTimeoutMinutes();
            TdApi.User me = client.getMeAsync().get(timeoutMinutes, TimeUnit.MINUTES);
            
            log.info("=".repeat(60));
            log.info("✓ Telegram 登录成功！");
            log.info("用户名称: {} {}", me.firstName, me.lastName != null ? me.lastName : "");
            log.info("用户 ID: {}", me.id);
            if (me.usernames != null && me.usernames.activeUsernames != null && me.usernames.activeUsernames.length > 0) {
                log.info("用户名: @{}", me.usernames.activeUsernames[0]);
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
}
