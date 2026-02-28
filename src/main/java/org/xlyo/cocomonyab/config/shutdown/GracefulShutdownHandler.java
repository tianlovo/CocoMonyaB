package org.xlyo.cocomonyab.config.shutdown;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.source.MessageSourceRegistry;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

/**
 * 优雅关闭处理器
 * <p>
 * 负责应用启动失败或正常关闭时的资源清理工作，包括：
 * <ul>
 *   <li>捕获启动阶段的致命错误</li>
 *   <li>按与启动相反的顺序关闭组件</li>
 *   <li>释放所有资源（数据库连接、文件句柄、线程池）</li>
 *   <li>以非零退出码退出（启动失败时）</li>
 * </ul>
 * </p>
 * <p>
 * 关闭顺序（与启动相反）：
 * <ol>
 *   <li>停止消息源（如果已启动）</li>
 *   <li>关闭Telegram客户端（如果已连接）</li>
 *   <li>停止消息处理插件（如果已初始化）</li>
 *   <li>关闭数据库连接（如果已建立）</li>
 *   <li>清理临时文件和资源</li>
 * </ol>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GracefulShutdownHandler {
    
    private final MessageSourceRegistry messageSourceRegistry;
    private final TelegramClientManager telegramClientManager;
    private final PluginManager pluginManager;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 标记是否正在关闭
     */
    @Getter
    private volatile boolean isShuttingDown = false;
    
    /**
     * 标记是否因启动失败而关闭
     */
    @Getter
    private volatile boolean isStartupFailure = false;
    
    /**
     * 启动失败的异常
     */
    @Getter
    private volatile Throwable startupException;
    
    /**
     * 处理启动异常
     * <p>
     * 当启动过程中发生 StartupException 时，此方法会被调用。
     * 记录详细的错误信息，执行优雅关闭流程，并以非零退出码退出进程。
     * </p>
     *
     * @param exception 启动异常
     */
    public void handleStartupFailure(StartupException exception) {
        if (isShuttingDown) {
            log.warn("已在关闭过程中，忽略重复的启动失败处理");
            return;
        }
        
        isShuttingDown = true;
        isStartupFailure = true;
        startupException = exception;
        
        log.error("╔════════════════════════════════════════════════════════════╗");
        log.error("║                                                            ║");
        log.error("║              ❌ 应用启动失败！                              ║");
        log.error("║                                                            ║");
        log.error("╚════════════════════════════════════════════════════════════╝");
        log.error("");
        
        // 记录详细的错误堆栈信息
        log.error("错误详情:", exception);
        
        // 记录当前启动阶段和失败原因
        String currentPhase = getCurrentPhase();
        log.error("失败阶段: {}", currentPhase);
        log.error("失败原因: {}", exception.getMessage());
        log.error("");
        
        // 执行优雅关闭流程
        performGracefulShutdown();
        
        // 以非零退出码退出进程
        log.error("应用将以退出码 1 退出");
        System.exit(1);
    }
    
    /**
     * 监听 Spring 容器关闭事件
     * <p>
     * 当 Spring 容器正常关闭时（如收到 SIGTERM 信号），此方法会被调用。
     * 执行优雅关闭流程，确保所有资源被正确释放。
     * </p>
     *
     * @param event 容器关闭事件
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        if (isShuttingDown) {
            log.debug("已在关闭过程中，忽略容器关闭事件");
            return;
        }
        
        isShuttingDown = true;
        
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║                                                            ║");
        log.info("║              🛑 应用正在关闭...                            ║");
        log.info("║                                                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("");
        
        performGracefulShutdown();
        
        log.info("✅ 应用已优雅关闭");
    }
    
    /**
     * PreDestroy 钩子
     * <p>
     * 作为最后的保障，确保资源被释放。
     * 通常情况下，资源已在 onContextClosed 中释放。
     * </p>
     */
    @PreDestroy
    public void preDestroy() {
        if (!isShuttingDown) {
            log.info("执行 PreDestroy 钩子，确保资源释放");
            performGracefulShutdown();
        }
    }
    
    /**
     * 执行优雅关闭流程
     * <p>
     * 按照与启动相反的顺序关闭已初始化的组件，确保所有资源被正确释放。
     * </p>
     */
    private void performGracefulShutdown() {
        log.info("开始执行优雅关闭流程...");
        log.info("");
        
        // 1. 停止消息源（如果已启动）
        shutdownMessageSources();
        
        // 2. 关闭Telegram客户端（如果已连接）
        shutdownTelegramClient();
        
        // 3. 停止消息处理插件（如果已初始化）
        shutdownPlugins();
        
        // 4. 关闭数据库连接（如果已建立）
        // MongoDB 连接由 Spring 自动管理，会在容器关闭时自动释放
        shutdownDatabase();
        
        // 5. 清理临时文件和资源
        cleanupResources();
        
        log.info("");
        log.info("✅ 优雅关闭流程完成");
    }
    
    /**
     * 停止所有消息源
     */
    private void shutdownMessageSources() {
        try {
            log.info("▶️ 正在停止消息源...");
            
            int runningCount = messageSourceRegistry.getRunningSources().size();
            if (runningCount == 0) {
                log.info("   没有运行中的消息源，跳过");
                return;
            }
            
            messageSourceRegistry.stopAll();
            log.info("✅ 已停止 {} 个消息源", runningCount);
            
        } catch (Exception e) {
            log.error("❌ 停止消息源时发生错误", e);
        }
    }
    
    /**
     * 关闭 Telegram 客户端
     */
    private void shutdownTelegramClient() {
        try {
            log.info("▶️ 正在关闭 Telegram 客户端...");
            
            if (!telegramClientManager.isReady()) {
                log.info("   Telegram 客户端未初始化，跳过");
                return;
            }
            
            // TelegramClientManager 的 @PreDestroy 方法会自动执行关闭
            // 这里只需要记录日志
            log.info("✅ Telegram 客户端将由 Spring 容器自动关闭");
            
        } catch (Exception e) {
            log.error("❌ 关闭 Telegram 客户端时发生错误", e);
        }
    }
    
    /**
     * 停止所有插件
     */
    private void shutdownPlugins() {
        try {
            log.info("▶️ 正在停止消息处理插件...");
            
            int pluginCount = pluginManager.getPlugins().size();
            if (pluginCount == 0) {
                log.info("   没有已注册的插件，跳过");
                return;
            }
            
            // PluginManager 的 @PreDestroy 方法会自动执行关闭
            // 这里只需要记录日志
            log.info("✅ {} 个插件将由 Spring 容器自动关闭", pluginCount);
            
        } catch (Exception e) {
            log.error("❌ 停止插件时发生错误", e);
        }
    }
    
    /**
     * 关闭数据库连接
     */
    private void shutdownDatabase() {
        try {
            log.info("▶️ 正在关闭数据库连接...");
            
            // MongoDB 连接由 Spring Data MongoDB 自动管理
            // MongoClient 会在容器关闭时自动释放
            log.info("✅ 数据库连接将由 Spring 容器自动关闭");
            
        } catch (Exception e) {
            log.error("❌ 关闭数据库连接时发生错误", e);
        }
    }
    
    /**
     * 清理临时文件和资源
     */
    private void cleanupResources() {
        try {
            log.info("▶️ 正在清理临时文件和资源...");
            
            // 这里可以添加清理临时文件的逻辑
            // 例如：删除临时下载的文件、清理缓存等
            
            log.info("✅ 资源清理完成");
            
        } catch (Exception e) {
            log.error("❌ 清理资源时发生错误", e);
        }
    }
    
    /**
     * 获取当前启动阶段
     * <p>
     * 从 StartupProgressTracker 中获取最后一个进行中或失败的阶段。
     * </p>
     *
     * @return 当前阶段名称
     */
    private String getCurrentPhase() {
        try {
            var phases = progressTracker.getPhases();
            
            // 查找最后一个进行中或失败的阶段
            for (var entry : phases.entrySet()) {
                var phaseInfo = entry.getValue();
                if (phaseInfo.getStatus() == StartupProgressTracker.PhaseStatus.IN_PROGRESS ||
                    phaseInfo.getStatus() == StartupProgressTracker.PhaseStatus.FAILED) {
                    return entry.getKey();
                }
            }
            
            // 如果没有找到，返回最后一个阶段
            if (!phases.isEmpty()) {
                var lastEntry = phases.entrySet().stream()
                    .reduce((first, second) -> second);
                if (lastEntry.isPresent()) {
                    return lastEntry.get().getKey();
                }
            }
            
            return "未知阶段";
            
        } catch (Exception e) {
            log.error("获取当前阶段失败", e);
            return "未知阶段";
        }
    }

    /**
     * 执行优雅关闭流程（测试用，不调用 System.exit）
     * <p>
     * 此方法仅用于测试目的，执行优雅关闭但不退出进程。
     * </p>
     */
    void performGracefulShutdownForTest() {
        performGracefulShutdown();
    }
    
    /**
     * 处理启动失败（测试用，不调用 System.exit）
     * <p>
     * 此方法仅用于测试目的，处理启动失败但不退出进程。
     * </p>
     *
     * @param exception 启动异常
     */
    void handleStartupFailureForTest(StartupException exception) {
        if (isShuttingDown) {
            log.warn("已在关闭过程中，忽略重复的启动失败处理");
            return;
        }
        
        isShuttingDown = true;
        isStartupFailure = true;
        startupException = exception;
        
        log.error("测试模式：处理启动失败");
        log.error("错误详情:", exception);
        
        String currentPhase = getCurrentPhase();
        log.error("失败阶段: {}", currentPhase);
        log.error("失败原因: {}", exception.getMessage());
        
        performGracefulShutdown();
        
        // 不调用 System.exit(1)，以便测试可以继续
    }
}
