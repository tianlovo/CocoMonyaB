package org.xlyo.cocomonyab.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.config.properties.DatabaseStartupProperties;
import org.xlyo.cocomonyab.event.startup.ConfigurationReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

/**
 * 数据库管理器
 * <p>
 * 负责应用启动时的数据库初始化阶段，包括：
 * <ul>
 *   <li>监听配置就绪事件</li>
 *   <li>验证数据库连接（使用 ping 命令）</li>
 *   <li>实现连接失败重试机制（最多3次，间隔2秒）</li>
 *   <li>发布数据库就绪事件</li>
 * </ul>
 * </p>
 * <p>
 * 这是启动流程的第二个阶段，依赖于配置初始化阶段的完成
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseManager {
    
    private final MongoTemplate mongoTemplate;
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    private final DatabaseStartupProperties databaseProperties;
    
    /**
     * 监听配置就绪事件，开始数据库初始化
     * <p>
     * 当配置初始化完成后，此方法会被自动调用。
     * 执行数据库连接验证，并在成功后发布数据库就绪事件。
     * </p>
     *
     * @param event 配置就绪事件
     */
    @EventListener
    public void onConfigurationReady(ConfigurationReadyEvent event) {
        progressTracker.startPhase("数据库初始化");
        
        try {
            log.info("开始数据库初始化...");
            
            // 1. 验证数据库连接（带重试机制）
            validateConnectionWithRetry();
            
            // 2. 发布数据库就绪事件
            eventPublisher.publishDatabaseReady();
            
            progressTracker.completePhase("数据库初始化");
            log.info("✅ 数据库初始化完成");
            
        } catch (Exception e) {
            progressTracker.failPhase("数据库初始化", e.getMessage());
            log.error("❌ 数据库初始化失败", e);
            throw new StartupException("数据库初始化失败", e);
        }
    }
    
    /**
     * 验证数据库连接（带重试机制）
     * <p>
     * 使用 MongoDB 的 ping 命令验证连接。
     * 如果连接失败，会根据配置的重试次数和延迟进行重试。
     * 所有重试失败后，抛出异常终止启动。
     * </p>
     */
    private void validateConnectionWithRetry() {
        int maxRetries = databaseProperties.getMaxRetries();
        long retryDelay = databaseProperties.getRetryDelayMs();
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt <= maxRetries) {
            try {
                if (attempt > 0) {
                    log.info("尝试重新连接数据库 (第 {}/{} 次重试)...", attempt, maxRetries);
                } else {
                    log.info("验证数据库连接...");
                }
                
                // 执行 ping 命令验证连接
                validateConnection();
                
                log.info("✅ 数据库连接验证成功");
                return;
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt <= maxRetries) {
                    log.warn("❌ 数据库连接失败: {} - 将在 {} 毫秒后重试", 
                            e.getMessage(), retryDelay);
                    
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new StartupException("数据库连接重试被中断", ie);
                    }
                } else {
                    log.error("❌ 数据库连接失败，已达到最大重试次数 ({})", maxRetries);
                }
            }
        }
        
        // 所有重试都失败，抛出异常
        throw new StartupException(
                String.format("数据库连接失败，已重试 %d 次", maxRetries), 
                lastException
        );
    }
    
    /**
     * 验证数据库连接
     * <p>
     * 使用 MongoDB 的 ping 命令验证连接是否可用。
     * </p>
     */
    private void validateConnection() {
        Document result = mongoTemplate.executeCommand("{ ping: 1 }");
        
        // 检查 ping 命令的返回结果
        if (result.isEmpty() || !result.containsKey("ok") || result.getDouble("ok") != 1.0) {
            throw new IllegalStateException("数据库 ping 命令返回异常结果");
        }
    }
}
