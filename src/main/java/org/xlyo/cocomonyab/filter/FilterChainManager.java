package org.xlyo.cocomonyab.filter;

import it.tdlight.jni.TdApi;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 过滤器链管理器
 * 负责过滤器注册、调度和生命周期管理
 */
@Slf4j
@Component
public class FilterChainManager {
    
    private final List<MessageFilter> filters = new ArrayList<>();
    private final Map<String, MessageFilter> filterMap = new ConcurrentHashMap<>();
    private final Map<String, Long> executionTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> rejectionCounts = new ConcurrentHashMap<>();
    
    /**
     * 注册过滤器
     */
    public void registerFilter(MessageFilter filter) {
        if (filterMap.containsKey(filter.getName())) {
            log.warn("过滤器 {} 已注册，跳过", filter.getName());
            return;
        }
        
        try {
            filters.add(filter);
            filterMap.put(filter.getName(), filter);
            
            // 按优先级排序（优先级高的先执行）
            filters.sort(Comparator.comparingInt(MessageFilter::getPriority).reversed());
            
            log.info("已注册过滤器: {} (优先级: {})", 
                filter.getName(), filter.getPriority());
        } catch (Exception e) {
            log.error("注册过滤器失败: {}", filter.getName(), e);
        }
    }
    
    /**
     * 注销过滤器
     */
    public void unregisterFilter(String filterName) {
        MessageFilter filter = filterMap.remove(filterName);
        if (filter != null) {
            filters.remove(filter);
            log.info("已注销过滤器: {}", filterName);
        }
    }
    
    /**
     * 执行过滤器链
     * 
     * @param message TDLib原始消息
     * @return true表示消息被接受，false表示消息被拒绝
     */
    public boolean executeChain(TdApi.Message message) {
        if (filters.isEmpty()) {
            log.debug("未注册过滤器，默认接受消息");
            return true;
        }
        
        log.info("🔍 [过滤器链] 开始执行过滤器链: chatId={}, messageId={}, 过滤器数量={}", 
            message.chatId, message.id, filters.size());
        
        FilterContext context = new FilterContext();
        
        for (MessageFilter filter : filters) {
            long startTime = System.currentTimeMillis();
            
            try {
                log.debug("▶️ [过滤器] 执行过滤器: {}, chatId={}, messageId={}", 
                    filter.getName(), message.chatId, message.id);
                
                FilterResult result = filter.filter(message, context);
                
                long executionTime = System.currentTimeMillis() - startTime;
                recordExecutionTime(filter.getName(), executionTime);
                
                log.debug("✅ [过滤器] 过滤器执行完成: {}, 耗时={}ms, 结果={}", 
                    filter.getName(), executionTime, result);
                
                if (result == FilterResult.REJECT) {
                    recordRejection(filter.getName());
                    String reason = context.getRejectReason() != null 
                        ? context.getRejectReason() 
                        : "未提供原因";
                    log.info("❌ [过滤器拒绝] 消息被过滤器拒绝: 过滤器={}, chatId={}, messageId={}, 原因={}", 
                        filter.getName(), message.chatId, message.id, reason);
                    return false;
                }
            } catch (Exception e) {
                log.error("❌ [过滤器错误] 执行过滤器时出错: {}, chatId={}, messageId={}, error={}", 
                    filter.getName(), message.chatId, message.id, e.getMessage(), e);
                // 继续执行下一个过滤器
            }
        }
        
        // 所有过滤器都接受，消息通过
        log.info("✅ [过滤器链] 消息通过所有过滤器: chatId={}, messageId={}", 
            message.chatId, message.id);
        return true;
    }
    
    /**
     * 记录执行时间
     */
    private void recordExecutionTime(String filterName, long time) {
        executionTimes.merge(filterName, time, Long::sum);
    }
    
    /**
     * 记录拒绝次数
     */
    private void recordRejection(String filterName) {
        rejectionCounts.merge(filterName, 1L, Long::sum);
    }
    
    /**
     * 获取过滤器执行统计
     */
    public Map<String, Long> getExecutionStats() {
        return new HashMap<>(executionTimes);
    }
    
    /**
     * 获取过滤器拒绝统计
     */
    public Map<String, Long> getRejectionStats() {
        return new HashMap<>(rejectionCounts);
    }
    
    /**
     * 获取所有过滤器
     */
    public List<MessageFilter> getFilters() {
        return new ArrayList<>(filters);
    }
    
    /**
     * 获取过滤器
     */
    public MessageFilter getFilter(String name) {
        return filterMap.get(name);
    }
    
    /**
     * 启用过滤器
     */
    public void enableFilter(String name) {
        MessageFilter filter = filterMap.get(name);
        if (filter instanceof AbstractMessageFilter) {
            ((AbstractMessageFilter) filter).setEnabled(true);
            log.info("已启用过滤器: {}", name);
        }
    }
    
    /**
     * 禁用过滤器
     */
    public void disableFilter(String name) {
        MessageFilter filter = filterMap.get(name);
        if (filter instanceof AbstractMessageFilter) {
            ((AbstractMessageFilter) filter).setEnabled(false);
            log.info("已禁用过滤器: {}", name);
        }
    }
    
    /**
     * 应用关闭时清理
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭过滤器链管理器，已注册 {} 个过滤器", filters.size());
        filters.clear();
        filterMap.clear();
    }
}
