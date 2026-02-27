package org.xlyo.cocomonyab.telegram;

import it.tdlight.client.ClientInteraction;
import it.tdlight.client.InputParameter;
import it.tdlight.client.ParameterInfo;
import it.tdlight.client.ParameterInfoCode;
import it.tdlight.client.ParameterInfoPasswordHint;
import it.tdlight.client.ParameterInfoTermsOfService;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import org.xlyo.cocomonyab.config.properties.TgEnvProperties;

/**
 * 自定义客户端交互，自动从环境变量读取手机号和两步验证密码
 * 强制使用验证码登录（发送到其他已登录的 Telegram 设备）
 */
@Slf4j
public class TgAutoClientInteraction implements ClientInteraction {
    
    private final TgEnvProperties envProperties;
    private final java.util.Scanner scanner = new java.util.Scanner(System.in);
    
    public TgAutoClientInteraction(TgEnvProperties envProperties) {
        this.envProperties = envProperties;
    }
    
    @Override
    public CompletableFuture<String> onParameterRequest(InputParameter parameter, ParameterInfo parameterInfo) {
        return CompletableFuture.supplyAsync(() -> {
            return switch (parameter) {
                case ASK_FIRST_NAME -> {
                    log.error("✗ 错误: Telegram 需要输入名字（First Name）");
                    log.error("这通常发生在首次注册账号时。");
                    log.error("请先在手机或其他设备上完成 Telegram 账号注册，然后再使用本程序。");
                    System.exit(1);
                    yield "";
                }
                
                case ASK_LAST_NAME -> {
                    log.error("✗ 错误: Telegram 需要输入姓氏（Last Name）");
                    log.error("这通常发生在首次注册账号时。");
                    log.error("请先在手机或其他设备上完成 Telegram 账号注册，然后再使用本程序。");
                    System.exit(1);
                    yield "";
                }
                
                case ASK_CODE -> {
                    ParameterInfoCode codeInfo = ((ParameterInfoCode) parameterInfo);
                    log.info("=".repeat(60));
                    log.info("验证码已发送到你的其他 Telegram 设备");
                    log.info("=".repeat(60));
                    log.info("验证码信息:");
                    log.info("  手机号: {}", codeInfo.getPhoneNumber());
                    log.info("  超时: {} 秒", codeInfo.getTimeout());
                    
                    String codeType = codeInfo.getType().getClass().getSimpleName()
                            .replace("AuthenticationCodeType", "");
                    log.info("  类型: {}", codeType);
                    
                    if (codeInfo.getNextType() != null) {
                        String nextType = codeInfo.getNextType()
                                .getClass()
                                .getSimpleName()
                                .replace("AuthenticationCodeType", "");
                        log.info("  备用类型: {}", nextType);
                    }
                    log.info("=".repeat(60));

                    // 从控制台读取验证码
                    log.info("提示: 验证码将发送到你的其他已登录 Telegram 设备");
                    System.out.print("请输入验证码: ");
                    yield scanner.nextLine().trim();
                }
                
                case ASK_PASSWORD -> {
                    // 从环境变量读取两步验证密码
                    String password = envProperties.getTg2fa();
                    
                    if (password == null || password.isEmpty()) {
                        log.error("✗ 错误: 账号启用了两步验证，但未在 .env 文件中配置 TG_2FA");
                        log.error("请在配置目录的 .env 文件中添加:");
                        log.error("TG_2FA=your_password");
                        System.exit(1);
                        yield "";
                    }
                    
                    ParameterInfoPasswordHint passwordInfo = (ParameterInfoPasswordHint) parameterInfo;
                    String hint = passwordInfo.getHint();
                    
                    log.info("✓ 正在使用环境变量中的两步验证密码...");
                    if (hint != null && !hint.isEmpty()) {
                        log.info("  密码提示: {}", hint);
                    }
                    
                    yield password;
                }
                
                case NOTIFY_LINK -> {
                    // 不支持二维码登录，提示错误并退出
                    log.error("✗ 错误: 检测到二维码登录请求");
                    log.error("本程序仅支持验证码登录（发送到其他已登录的 Telegram 设备）");
                    log.error("请确保:");
                    log.error("  1. 你的账号已在其他设备（手机/平板/电脑）上登录 Telegram");
                    log.error("  2. 在 .env 文件中正确配置了 TG_PHONE");
                    System.exit(1);
                    yield "";
                }
                
                case TERMS_OF_SERVICE -> {
                    TdApi.TermsOfService tos = ((ParameterInfoTermsOfService) parameterInfo).getTermsOfService();
                    log.info("=".repeat(60));
                    log.info("Telegram 服务条款:");
                    log.info(tos.text.text);
                    if (tos.minUserAge > 0) {
                        log.info("最小年龄要求: {}", tos.minUserAge);
                    }
                    log.info("=".repeat(60));
                    
                    if (tos.showPopup) {
                        System.out.print("请按回车接受服务条款并继续: ");
                        try (var scanner = new java.util.Scanner(System.in)) {
                            scanner.nextLine();
                        }
                    }
                    yield "";
                }
                
                default -> {
                    log.error("✗ 错误: Telegram 请求了未预期的输入参数: {}", parameter);
                    log.error("这可能需要额外的配置或操作。");
                    log.error("请联系开发者或查看文档。");
                    System.exit(1);
                    yield "";
                }
            };
        });
    }
}
