package org.xlyo.cocomonyab.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.ApiReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

/**
 * 应用就绪监听器
 * <p>
 * 负责监听 API 就绪事件，发布应用就绪事件，并输出启动完成信息。
 * 这是启动流程的最后一个阶段，标志着应用已完全启动。
 * </p>
 * <p>
 * 功能：
 * <ul>
 *   <li>监听 ApiReadyEvent 事件（需求 7.1）</li>
 *   <li>发布 ApplicationReadyEvent 事件（需求 7.1）</li>
 *   <li>输出启动完成日志，包含总启动时间（需求 7.2）</li>
 *   <li>输出组件状态摘要（需求 7.3）</li>
 *   <li>输出启动成功的 ASCII 艺术图案（需求 7.4）</li>
 *   <li>输出系统访问 URL（需求 7.5）</li>
 *   <li>调用 StartupProgressTracker.printStatistics 输出耗时统计</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationReadyListener {
    
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 服务器端口
     * 从 application.yaml 的 server.port 配置读取
     */
    @Value("${server.port:8080}")
    private int serverPort;
    
    /**
     * 监听 API 就绪事件，发布应用就绪事件并输出启动完成信息
     * <p>
     * 当 API 初始化完成后，此方法会被自动调用。
     * 发布应用就绪事件，并输出启动完成的详细信息。
     * </p>
     *
     * @param event API 就绪事件
     */
    @EventListener
    public void onApiReady(ApiReadyEvent event) {
        try {
            // 发布应用就绪事件
            eventPublisher.publishApplicationReady();
            
            // 输出启动完成信息
            printStartupComplete();
            
        } catch (Exception e) {
            log.error("❌ 应用就绪处理失败", e);
        }
    }
    
    /**
     * 输出启动完成信息
     * <p>
     * 包括：
     * <ul>
     *   <li>ASCII 艺术图案（需求 7.4）</li>
     *   <li>总启动时间（需求 7.2）</li>
     *   <li>访问 URL（需求 7.5）</li>
     *   <li>组件状态摘要（需求 7.3）</li>
     *   <li>启动阶段耗时统计</li>
     * </ul>
     * </p>
     */
    private void printStartupComplete() {
        long totalTime = progressTracker.getTotalTime();
        
        // ASCII 艺术图案和启动信息
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║                                                            ║");
        log.info("║              🎉 CocoMonyaB 启动成功！                      ║");
        log.info("║                                                            ║");
        log.info("║  总启动时间: {} ms                                    ║", String.format("%-8d", totalTime));
        log.info("║  访问地址: http://localhost:{}                           ║", serverPort);
        log.info("║                                                            ║");
        log.info("║  组件状态摘要:                                             ║");
        log.info("║    ✅ 配置管理器 - 已就绪                                  ║");
        log.info("║    ✅ 数据库管理器 - 已就绪                                ║");
        log.info("║    ✅ 集合初始化器 - 已就绪                                ║");
        log.info("║    ✅ 插件管理器 - 已就绪                                  ║");
        log.info("║    ✅ 消息源管理器 - 已就绪                                ║");
        log.info("║    ✅ API服务器 - 已就绪                                   ║");
        log.info("║                                                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("");
        
        // 输出各阶段耗时统计
        progressTracker.printStatistics();
    }
}
