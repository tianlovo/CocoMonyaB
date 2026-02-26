package org.xlyo.cocomonyab.domain.enums;

/**
 * 未读消息缓冲区状态枚举
 * <p>
 * 用于标识缓冲区中消息的处理状态
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
public enum BufferStatus {
    
    /**
     * 待处理
     * <p>
     * 消息已保存到缓冲区，等待处理
     */
    PENDING,
    
    /**
     * 已处理
     * <p>
     * 消息已成功处理完成
     */
    PROCESSED,
    
    /**
     * 处理失败
     * <p>
     * 消息处理过程中发生错误
     */
    FAILED
}
