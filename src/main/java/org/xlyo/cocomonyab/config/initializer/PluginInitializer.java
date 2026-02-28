package org.xlyo.cocomonyab.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.CollectionsReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;
import org.xlyo.cocomonyab.plugin.MessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginManager;

import java.util.List;

/**
 * 插件初始化器
 * <p>
 * 负责应用启动时的消息处理插件初始化阶段，包括：
 * <ul>
 *   <li>监听集合就绪事件</li>
 *   <li>扫描所有 MessagePlugin Bean</li>
 *   <li>按优先级降序排序插件（优先级数字越大越早执行）</li>
 *   <li>初始化所有插件</li>
 *   <li>发布插件就绪事件</li>
 * </ul>
 * </p>
 * <p>
 * 这是启动流程的第四个阶段，依赖于集合初始化阶段的完成
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PluginInitializer {
    
    private final PluginManager pluginManager;
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 监听集合就绪事件，开始插件初始化
     * <p>
     * 当集合初始化完成后，此方法会被自动调用。
     * 执行插件扫描、排序和初始化，并在成功后发布插件就绪事件。
     * </p>
     *
     * @param event 集合就绪事件
     */
    @EventListener
    public void onCollectionsReady(CollectionsReadyEvent event) {
        progressTracker.startPhase("插件初始化");
        
        try {
            log.info("开始插件初始化...");
            
            // 1. 扫描插件
            List<MessagePlugin> plugins = pluginManager.scanPlugins();
            log.info("扫描到 {} 个插件", plugins.size());
            
            // 2. 插件已在 scanPlugins 中按优先级降序排序
            if (!plugins.isEmpty()) {
                log.info("插件优先级排序（降序）:");
                for (MessagePlugin plugin : plugins) {
                    log.info("  - {} (优先级: {})", plugin.getName(), plugin.getPriority());
                }
            }
            
            // 3. 初始化插件
            pluginManager.initializePlugins(plugins);
            
            // 4. 发布插件就绪事件
            eventPublisher.publishPluginsReady();
            
            progressTracker.completePhase("插件初始化");
            log.info("✅ 插件初始化完成，共 {} 个插件", plugins.size());
            
        } catch (Exception e) {
            progressTracker.failPhase("插件初始化", e.getMessage());
            log.error("❌ 插件初始化失败", e);
            throw new StartupException("插件初始化失败", e);
        }
    }
}
