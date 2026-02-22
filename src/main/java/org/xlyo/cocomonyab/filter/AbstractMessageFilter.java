package org.xlyo.cocomonyab.filter;

import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息过滤器抽象基类
 * 提供默认实现和通用功能
 */
@Slf4j
public abstract class AbstractMessageFilter implements MessageFilter {
    
    private boolean enabled = true;
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public FilterResult filter(TdApi.Message message, FilterContext context) {
        if (!isEnabled()) {
            log.debug("Filter {} is disabled, accepting message", getName());
            return FilterResult.ACCEPT;
        }
        
        try {
            return doFilter(message, context);
        } catch (Exception e) {
            log.error("Error in filter {}: {}", getName(), e.getMessage(), e);
            // 出错时默认接受消息（fail-open策略）
            return FilterResult.ACCEPT;
        }
    }
    
    /**
     * 实际的过滤逻辑，由子类实现
     */
    protected abstract FilterResult doFilter(TdApi.Message message, FilterContext context);
}
