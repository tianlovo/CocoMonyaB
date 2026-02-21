package org.xlyo.cocomonyab.listener;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.TelegramProperties;
import org.xlyo.cocomonyab.config.TgEnvProperties;

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

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("开始 Telegram 自动登录流程...");
        
        // 打印配置信息（脱敏）
        log.info("API ID: {}", envProperties.getApiId());
        log.info("API Hash: {}***", envProperties.getApiHash().substring(0, 8));
        log.info("手机号: {}", maskPhone(envProperties.getTgPhone()));
        log.info("设备型号: {}", telegramProperties.getDeviceModel());
        log.info("数据库目录: {}", telegramProperties.getDatabaseDirectory());
        log.info("下载目录: {}", telegramProperties.getDownloadDirectory());
        
        // TODO: 实现自动登录逻辑
        // 1. 初始化 TDLight 客户端
        // 2. 设置 TDLib 参数
        // 3. 发送手机号进行认证
        // 4. 处理验证码输入
        // 5. 如果启用了 2FA，处理密码输入
        // 6. 登录成功后的处理
        
        log.warn("TODO: Telegram 自动登录功能待实现");
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
