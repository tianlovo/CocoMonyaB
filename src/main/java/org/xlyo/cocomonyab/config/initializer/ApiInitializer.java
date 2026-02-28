package org.xlyo.cocomonyab.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.MessageSourcesReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

/**
 * API 初始化器
 * <p>
 * 负责应用启动时的 RESTful API 初始化阶段，包括：
 * <ul>
 *   <li>监听消息源就绪事件</li>
 *   <li>记录 API 服务器监听地址和端口</li>
 *   <li>发布 API 就绪事件</li>
 * </ul>
 * </p>
 * <p>
 * 这是启动流程的第六个阶段，依赖于消息源初始化阶段的完成。
 * Web 服务器由 Spring Boot 自动启动，此组件主要负责记录信息和发布事件。
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiInitializer {
    
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 服务器端口
     * 从 application.yaml 的 server.port 配置读取
     */
    @Value("${server.port:8080}")
    private int serverPort;
    
    /**
     * 监听消息源就绪事件，开始 API 初始化
     * <p>
     * 当消息源初始化完成后，此方法会被自动调用。
     * Web 服务器由 Spring Boot 自动启动，这里只需要记录日志和发布事件。
     * </p>
     *
     * @param event 消息源就绪事件
     */
    @EventListener
    public void onMessageSourcesReady(MessageSourcesReadyEvent event) {
        progressTracker.startPhase("API初始化");
        
        try {
            log.info("开始 API 初始化...");
            
            // Web 服务器由 Spring Boot 自动启动
            // 这里只需要记录服务器信息
            logServerInfo();
            
            // 发布 API 就绪事件
            eventPublisher.publishApiReady();
            
            progressTracker.completePhase("API初始化");
            log.info("✅ API 初始化完成");
            
        } catch (Exception e) {
            progressTracker.failPhase("API初始化", e.getMessage());
            log.error("❌ API 初始化失败", e);
            throw new StartupException("API 初始化失败", e);
        }
    }
    
    /**
     * 记录 API 服务器信息
     * <p>
     * 输出服务器监听地址和端口信息，方便用户访问。
     * </p>
     */
    private void logServerInfo() {
        log.info("API 服务器监听地址: http://localhost:{}", serverPort);
    }
}
