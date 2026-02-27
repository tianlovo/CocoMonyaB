package org.xlyo.cocomonyab.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xlyo.cocomonyab.interceptor.SystemReadyInterceptor;

/**
 * Web MVC 配置
 * <p>
 * 配置拦截器、跨域等Web相关设置
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final SystemReadyInterceptor systemReadyInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册系统就绪状态拦截器");
        
        registry.addInterceptor(systemReadyInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有API请求
                .order(0);  // 设置为最高优先级
    }
}
