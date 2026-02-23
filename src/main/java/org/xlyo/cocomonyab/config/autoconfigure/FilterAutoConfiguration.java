package org.xlyo.cocomonyab.config;

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
        log.info("Starting filter auto-registration...");
        
        // 从Spring容器中获取所有MessageFilter类型的Bean
        Map<String, MessageFilter> filterBeans = applicationContext.getBeansOfType(MessageFilter.class);
        
        if (filterBeans.isEmpty()) {
            log.warn("No MessageFilter beans found in Spring context");
            return;
        }
        
        // 注册所有过滤器
        for (Map.Entry<String, MessageFilter> entry : filterBeans.entrySet()) {
            String beanName = entry.getKey();
            MessageFilter filter = entry.getValue();
            
            try {
                filterChainManager.registerFilter(filter);
                log.info("Auto-registered filter: {} (bean: {})", filter.getName(), beanName);
            } catch (Exception e) {
                log.error("Failed to auto-register filter: {} (bean: {})", filter.getName(), beanName, e);
            }
        }
        
        log.info("Filter auto-registration completed. Total filters: {}", filterBeans.size());
    }
}
