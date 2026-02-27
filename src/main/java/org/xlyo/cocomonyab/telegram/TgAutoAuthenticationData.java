package org.xlyo.cocomonyab.telegram;

import it.tdlight.client.SimpleAuthenticationSupplier;

import java.util.concurrent.CompletableFuture;

/**
 * 自定义认证数据，支持手机号登录和二维码登录
 */
public class TgAutoAuthenticationData implements SimpleAuthenticationSupplier<TgAutoAuthenticationData> {
    
    private final String phoneNumber;
    private final boolean qrCode;
    
    private TgAutoAuthenticationData(String phoneNumber, boolean qrCode) {
        this.phoneNumber = phoneNumber;
        this.qrCode = qrCode;
    }
    
    /**
     * 创建手机号登录方式
     */
    public static TgAutoAuthenticationData phoneNumber(String phoneNumber) {
        return new TgAutoAuthenticationData(phoneNumber, false);
    }
    
    /**
     * 创建二维码登录方式
     */
    public static TgAutoAuthenticationData qrCode() {
        return new TgAutoAuthenticationData(null, true);
    }

    @Override
    public boolean isQrCode() {
        return qrCode;
    }

    @Override
    public boolean isBot() {
        return false;
    }

    @Override
    public String getUserPhoneNumber() {
        if (qrCode) {
            throw new UnsupportedOperationException("二维码登录不需要手机号");
        }
        // 如果手机号为空，返回空字符串，让 ClientInteraction 处理
        return phoneNumber != null ? phoneNumber : "";
    }

    @Override
    public String getBotToken() {
        throw new UnsupportedOperationException("这不是机器人登录");
    }

    @Override
    public CompletableFuture<TgAutoAuthenticationData> get() {
        return CompletableFuture.completedFuture(this);
    }
}
