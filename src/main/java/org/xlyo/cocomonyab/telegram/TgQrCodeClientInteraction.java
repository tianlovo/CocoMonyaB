package org.xlyo.cocomonyab.telegram;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import it.tdlight.client.ClientInteraction;
import it.tdlight.client.InputParameter;
import it.tdlight.client.ParameterInfo;
import it.tdlight.client.ParameterInfoNotifyLink;
import it.tdlight.client.ParameterInfoPasswordHint;
import it.tdlight.client.ParameterInfoTermsOfService;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.xlyo.cocomonyab.config.properties.TgEnvProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 二维码登录客户端交互
 * 在控制台显示二维码，用户使用手机扫码登录
 */
@Slf4j
public class TgQrCodeClientInteraction implements ClientInteraction {
    
    private final TgEnvProperties envProperties;
    
    public TgQrCodeClientInteraction(TgEnvProperties envProperties) {
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
                    log.error("✗ 错误: 二维码登录模式下不应该请求验证码");
                    log.error("请检查配置或切换到其他登录方式");
                    System.exit(1);
                    yield "";
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
                    // 从 ParameterInfoNotifyLink 中提取实际的链接
                    ParameterInfoNotifyLink linkInfo = (ParameterInfoNotifyLink) parameterInfo;
                    String link = linkInfo.getLink();
                    
                    log.info("=".repeat(60));
                    log.info("请使用 Telegram 手机客户端扫描以下二维码登录:");
                    log.info("=".repeat(60));
                    log.info("");
                    
                    // 尝试生成并保存二维码图片
                    String qrImagePath = null;
                    try {
                        qrImagePath = saveQRCodeImage(link);
                        log.info("✓ 二维码图片已保存到: {}", qrImagePath);
                        log.info("  可以使用图片查看器打开并扫描");
                        log.info("");
                    } catch (Exception e) {
                        log.debug("保存二维码图片失败: {}", e.getMessage());
                    }
                    
                    // 在控制台显示二维码
                    try {
                        printQRCode(link);
                        log.info("");
                    } catch (Exception e) {
                        log.warn("生成控制台二维码失败: {}", e.getMessage());
                    }
                    
                    // 始终显示登录链接作为备用方案
                    log.info("=".repeat(60));
                    log.info("如果无法扫描二维码，请使用以下方式之一:");
                    if (qrImagePath != null) {
                        log.info("1. 打开图片文件: {}", qrImagePath);
                    }
                    log.info("2. 复制以下链接在 Telegram 中打开:");
                    log.info("   {}", link);
                    log.info("=".repeat(60));
                    log.info("提示: 打开 Telegram 手机客户端 -> 设置 -> 设备 -> 扫描二维码");
                    log.info("=".repeat(60));
                    
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
    
    /**
     * 在控制台打印二维码
     */
    private void printQRCode(String content) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);  // 增加边距
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L);  // 降低纠错级别，减少复杂度
        
        // 调整二维码大小：使用更大的尺寸以提高识别率
        int size = 60;  // 增加尺寸
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        
        // 添加上边距
        System.out.println();
        System.out.println();
        
        // 使用 Unicode 字符绘制二维码，每两行合并为一行
        for (int y = 0; y < bitMatrix.getHeight(); y += 2) {
            // 左边距
            System.out.print("    ");
            
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                boolean top = bitMatrix.get(x, y);
                boolean bottom = (y + 1 < bitMatrix.getHeight()) && bitMatrix.get(x, y + 1);
                
                // 使用不同的字符组合来表示二维码
                if (top && bottom) {
                    System.out.print("██");  // 两个都是黑色
                } else if (top) {
                    System.out.print("▀▀");  // 上半部分黑色
                } else if (bottom) {
                    System.out.print("▄▄");  // 下半部分黑色
                } else {
                    System.out.print("  ");  // 两个都是白色
                }
            }
            System.out.println();
        }
        
        // 添加下边距
        System.out.println();
        System.out.println();
    }
    
    /**
     * 保存二维码为图片文件
     */
    private String saveQRCodeImage(String content) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
        
        // 生成更大的二维码图片（300x300）以便扫描
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300, hints);
        
        // 保存到临时目录
        Path qrCodePath = Paths.get("data", "tmp", "telegram_qrcode.png");
        Files.createDirectories(qrCodePath.getParent());
        
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrCodePath);
        
        return qrCodePath.toAbsolutePath().toString();
    }
}
