package org.xlyo.cocomonyab.filter.impl;

import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.filter.AbstractMessageFilter;
import org.xlyo.cocomonyab.filter.FilterContext;
import org.xlyo.cocomonyab.filter.FilterResult;

/**
 * 空消息过滤器
 * 过滤掉没有实际内容的消息
 */
@Slf4j
@Component
public class EmptyMessageFilter extends AbstractMessageFilter {
    
    private static final int PRIORITY = 100; // 高优先级，尽早过滤
    
    @Override
    public String getName() {
        return "EmptyMessageFilter";
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    protected FilterResult doFilter(TdApi.Message message, FilterContext context) {
        // 检查消息内容是否为空
        if (message.content == null) {
            context.setRejectReason("Message content is null");
            return FilterResult.REJECT;
        }
        
        // 检查文本消息是否为空
        if (message.content instanceof TdApi.MessageText text) {
            if (text.text == null || text.text.text == null || text.text.text.trim().isEmpty()) {
                context.setRejectReason("Text message is empty");
                return FilterResult.REJECT;
            }
        }
        
        return FilterResult.ACCEPT;
    }
}
