package org.xlyo.cocomonyab.config.autoconfigure;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.MessageFilter;

import java.util.Map;

/**
 * 过滤器自动配置
 * 自动注册所有MessageFilter类型的Spring Bean
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FilterAutoConfiguration {
    
    private final ApplicationContext applicationContext;
    private final FilterChainManager filterChainManager;
    
    /**
     * 应用启动后自动注册所有过滤器
     */
    @PostConstruct
    public void registerFilters() {
        log.info("开始自动注册过滤器...");
        
        // 从Spring容器中获取所有MessageFilter类型的Bean
        Map<String, MessageFilter> filterBeans = applicationContext.getBeansOfType(MessageFilter.class);
        
        if (filterBeans.isEmpty()) {
            log.warn("在 Spring 上下文中未找到 MessageFilter bean");
            return;
        }
        
        // 注册所有过滤器
        for (Map.Entry<String, MessageFilter> entry : filterBeans.entrySet()) {
            String beanName = entry.getKey();
            MessageFilter filter = entry.getValue();
            
            try {
                filterChainManager.registerFilter(filter);
                log.info("已自动注册过滤器: {} (bean: {})", filter.getName(), beanName);
            } catch (Exception e) {
                log.error("自动注册过滤器失败: {} (bean: {})", filter.getName(), beanName, e);
            }
        }
        
        log.info("过滤器自动注册完成。总过滤器数: {}", filterBeans.size());
    }
}
