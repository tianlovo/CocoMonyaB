package org.xlyo.cocomonyab.filter;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 过滤上下文
 * 提供过滤器执行所需的上下文信息
 */
@Data
public class FilterContext {
    
    /**
     * 上下文属性（用于过滤器之间传递信息）
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    
    /**
     * 拒绝原因（当消息被拒绝时设置）
     */
    private String rejectReason;
    
    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

}
