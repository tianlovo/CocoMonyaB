package org.xlyo.cocomonyab.config.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // 由于我们是无状态token认证，并且认证在WebSocket层处理，
                // 因此需要禁用CSRF保护，并允许所有HTTP请求通过，
                // 以便WebSocket握手（/ws/**）可以正常进行。
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // 允许所有WebSocket端点、SockJS资源的请求
                        .requestMatchers("/ws/**").permitAll()
                        // TODO 其他所有HTTP请求（如果有REST API）需要认证，可以根据实际调整
                        .anyRequest().permitAll() // 全部放行
                )
                // 禁用默认的HTTP Basic和表单登录，因为我们不使用它们
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
