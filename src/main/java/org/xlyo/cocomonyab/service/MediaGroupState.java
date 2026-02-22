package org.xlyo.cocomonyab.service;

/**
 * 媒体组处理状态枚举
 * <p>
 * 定义媒体组在处理生命周期中的三种状态，用于管理并发安全的状态转换。
 * </p>
 * 
 * <h3>状态转换规则：</h3>
 * <ul>
 *   <li>首次接收消息 → COLLECTING</li>
 *   <li>超时且状态为 COLLECTING → PROCESSING</li>
 *   <li>处理成功 → COMPLETED</li>
 *   <li>处理失败 → 状态重置（允许重试）</li>
 *   <li>PROCESSING 或 COMPLETED 状态拒绝新消息</li>
 * </ul>
 * 
 * @see MediaGroupProcessor
 * @since 1.0
 */
public enum MediaGroupState {
    
    /**
     * 收集中：正在接收媒体组的消息
     * <p>
     * 在此状态下，系统接受该媒体组的新消息并将其添加到缓冲区。
     * 这是媒体组的初始状态。
     * </p>
     */
    COLLECTING,
    
    /**
     * 处理中：正在处理媒体组
     * <p>
     * 在此状态下，系统拒绝该媒体组的新消息。
     * 媒体组正在被处理（保存到数据库、解析、插件处理等）。
     * </p>
     */
    PROCESSING,
    
    /**
     * 已完成：媒体组处理完成
     * <p>
     * 在此状态下，系统拒绝该媒体组的新消息。
     * 媒体组已成功处理完成，不再接受任何修改。
     * </p>
     */
    COMPLETED
}
