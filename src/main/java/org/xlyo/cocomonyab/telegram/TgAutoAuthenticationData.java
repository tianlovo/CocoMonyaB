package org.xlyo.cocomonyab.telegram;

import it.tdlight.client.SimpleAuthenticationSupplier;

import java.util.concurrent.CompletableFuture;

/**
 * 自定义认证数据，仅支持手机号登录
 */
public class TgAutoAuthenticationData implements SimpleAuthenticationSupplier<TgAutoAuthenticationData> {
    
    private final String phoneNumber;
    
    private TgAutoAuthenticationData(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    /**
     * 创建手机号登录方式
     */
    public static TgAutoAuthenticationData phoneNumber(String phoneNumber) {
        return new TgAutoAuthenticationData(phoneNumber);
    }

    @Override
    public boolean isQrCode() {
        return false;
    }

    @Override
    public boolean isBot() {
        return false;
    }

    @Override
    public String getUserPhoneNumber() {
        return phoneNumber;
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
