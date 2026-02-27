package org.xlyo.cocomonyab.filter;

/**
 * 过滤结果枚举
 */
public enum FilterResult {
    /**
     * 接受消息，继续处理
     */
    ACCEPT,
    
    /**
     * 拒绝消息，停止处理（消息将被丢弃，不保存到数据库，不交给插件）
     */
    REJECT
}
