package org.xlyo.cocomonyab.config.mongo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MongoDB 运行模式枚举
 * 提供配置文件智能提示
 */
@Getter
@RequiredArgsConstructor
public enum MongoMode {
    
    /**
     * 嵌入式模式 - MongoDB 运行在应用进程内
     */
    EMBEDDED("embedded"),
    
    /**
     * 远程模式 - 连接到独立的 MongoDB 服务器
     */
    REMOTE("remote");
    
    private final String value;

    /**
     * 从字符串值转换为枚举
     * 
     * @param value 字符串值（不区分大小写）
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果值无效
     */
    public static MongoMode fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("MongoDB mode 不能为空");
        }
        
        for (MongoMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        
        throw new IllegalArgumentException(
            String.format("无效的 MongoDB mode: %s, 必须是 embedded 或 remote", value)
        );
    }
    
    /**
     * 检查是否为嵌入式模式
     */
    public boolean isEmbedded() {
        return this == EMBEDDED;
    }
    
    /**
     * 检查是否为远程模式
     */
    public boolean isRemote() {
        return this == REMOTE;
    }
}
