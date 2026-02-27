package org.xlyo.cocomonyab.filter;

import it.tdlight.jni.TdApi;

/**
 * 消息过滤器接口
 * 用于在消息保存到数据库和插件处理之前进行过滤
 */
public interface MessageFilter {
    
    /**
     * 获取过滤器名称
     */
    String getName();
    
    /**
     * 获取过滤器优先级（数值越大优先级越高，越早执行）
     */
    int getPriority();
    
    /**
     * 过滤器是否启用
     */
    boolean isEnabled();
    
    /**
     * 过滤消息
     * 
     * @param message TDLib原始消息
     * @param context 过滤上下文
     * @return 过滤结果
     */
    FilterResult filter(TdApi.Message message, FilterContext context);
}
