package org.xlyo.cocomonyab.plugin.tagforward.model;

/**
 * 转发状态枚举
 * 
 * 表示消息在转发队列中的处理状态
 */
public enum ForwardStatus {
    /**
     * 待处理 - 消息已加入队列，等待转发
     */
    PENDING,
    
    /**
     * 转发成功 - 消息已成功转发到目标频道
     */
    SUCCESS,
    
    /**
     * 转发失败 - 消息转发失败且已达到最大重试次数
     */
    FAILED
}
