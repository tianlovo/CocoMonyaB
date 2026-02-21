package org.xlyo.cocomonyab.listener;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@Order(1) // 优先执行配置验证
public class TgConfigValidationListener implements ApplicationListener<@NonNull ContextRefreshedEvent> {

    // API_ID 格式：纯数字
    private static final Pattern API_ID_PATTERN = Pattern.compile("^\\d+$");

    // API_HASH 格式：32位十六进制字符
    private static final Pattern API_HASH_PATTERN = Pattern.compile("^[a-f0-9]{32}$");

    // 手机号格式：以 + 开头，后跟数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+\\d{10,15}$");

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        log.info("开始校验 Telegram 配置...");
        boolean hasError = false;

        // 校验 API_ID
        String apiId = env.getProperty("API_ID");
        if (apiId == null || apiId.isBlank()) {
            log.error("❌ 配置验证失败: API_ID 未设置");
            log.error("请在 data/config/.env 文件中添加: API_ID=your_api_id");
            log.error("示例: API_ID=12345678");
            hasError = true;
        } else if ("your_api_id".equals(apiId)) {
            log.error("❌ 配置验证失败: API_ID 仍为示例值");
            log.error("请将 data/config/.env 文件中的 API_ID 修改为真实值");
            log.error("获取地址: https://my.telegram.org/apps");
            hasError = true;
        } else if (!API_ID_PATTERN.matcher(apiId).matches()) {
            log.error("❌ 配置验证失败: API_ID 格式错误");
            log.error("API_ID 应为纯数字，当前值: {}", apiId);
            log.error("示例: API_ID=12345678");
            hasError = true;
        }

        // 校验 API_HASH
        String apiHash = env.getProperty("API_HASH");
        if (apiHash == null || apiHash.isBlank()) {
            log.error("❌ 配置验证失败: API_HASH 未设置");
            log.error("请在 data/config/.env 文件中添加: API_HASH=your_api_hash");
            log.error("示例: API_HASH=0123456789abcdef0123456789abcdef");
            hasError = true;
        } else if ("your_api_hash".equals(apiHash)) {
            log.error("❌ 配置验证失败: API_HASH 仍为示例值");
            log.error("请将 data/config/.env 文件中的 API_HASH 修改为真实值");
            log.error("获取地址: https://my.telegram.org/apps");
            hasError = true;
        } else if (!API_HASH_PATTERN.matcher(apiHash).matches()) {
            log.error("❌ 配置验证失败: API_HASH 格式错误");
            log.error("API_HASH 应为32位十六进制字符，当前长度: {}", apiHash.length());
            log.error("示例: API_HASH=0123456789abcdef0123456789abcdef");
            hasError = true;
        }

        // 校验 TG_PHONE
        String tgPhone = env.getProperty("TG_PHONE");
        if (tgPhone == null || tgPhone.isBlank()) {
            log.error("❌ 配置验证失败: TG_PHONE 未设置");
            log.error("⚠️  本项目只能使用手机号登录，不支持二维码或其他方式登录");
            log.error("请在 data/config/.env 文件中添加: TG_PHONE=+8613800138000");
            hasError = true;
        } else if ("+8613800138000".equals(tgPhone) || "your_phone_number".equals(tgPhone)) {
            log.error("❌ 配置验证失败: TG_PHONE 仍为示例值");
            log.error("请将 data/config/.env 文件中的 TG_PHONE 修改为真实手机号");
            log.error("格式: +[国家代码][手机号]，例如: +8613800138000");
            hasError = true;
        } else if (!PHONE_PATTERN.matcher(tgPhone).matches()) {
            log.error("❌ 配置验证失败: TG_PHONE 格式错误");
            log.error("手机号应以 + 开头，后跟国家代码和手机号，当前值: {}", tgPhone);
            log.error("示例: TG_PHONE=+8613800138000");
            hasError = true;
        }

        // 校验 TG_2FA（可选，但给出警告）
        String tg2fa = env.getProperty("TG_2FA");
        if (tg2fa == null || tg2fa.isBlank() || "your_2fa_password_if_enabled".equals(tg2fa)) {
            log.warn("⚠️  警告: TG_2FA 未填写或仍为示例值");
            log.warn("若您的 Telegram 账号启用了两步验证（2FA），必须填写此密码，否则无法登录");
            log.warn("如未启用 2FA，可忽略此警告");
        }

        if (hasError) {
            log.error("\n❌ Telegram 配置校验失败，应用即将退出");
            log.error("请检查 data/config/.env 文件中的配置");
            log.error("如果文件不存在，请复制 data/config/.env.example 并重命名为 .env");
            System.exit(1);
        }

        log.info("✅ Telegram 配置校验通过");
    }
}
