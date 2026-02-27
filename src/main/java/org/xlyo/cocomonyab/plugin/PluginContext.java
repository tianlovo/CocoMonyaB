package org.xlyo.cocomonyab.plugin;

import it.tdlight.jni.TdApi;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件上下文
 * 提供插件执行所需的上下文信息
 */
@Getter
public class PluginContext {
    /**
     * 原始TDLib消息
     */
    private final TdApi.Message originalMessage;
    
    /**
     * 上下文属性（用于插件间传递数据）
     */
    private final Map<String, Object> attributes;
    
    public PluginContext(TdApi.Message originalMessage) {
        this.originalMessage = originalMessage;
        this.attributes = new ConcurrentHashMap<>();
    }
    
    /**
     * 设置上下文属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取上下文属性
     * 
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * 检查是否存在指定属性
     * 
     * @param key 属性键
     * @return 如果存在返回true，否则返回false
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * 移除上下文属性
     * 
     * @param key 属性键
     * @return 被移除的属性值，如果不存在则返回null
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }
}
