package org.xlyo.cocomonyab.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import it.tdlight.jni.TdApi;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理器
 * 负责插件注册、调度和生命周期管理
 */
@Slf4j
@Component
public class PluginManager {
    
    private final List<MessagePlugin> plugins = new ArrayList<>();
    private final Map<String, MessagePlugin> pluginMap = new ConcurrentHashMap<>();
    private final Map<String, Long> executionTimes = new ConcurrentHashMap<>();
    
    /**
     * 注册插件（不初始化）
     * <p>
     * 将插件添加到管理器中，但不调用初始化方法。
     * 初始化将在启动流程的插件初始化阶段统一进行。
     * </p>
     */
    public void registerPlugin(MessagePlugin plugin) {
        if (pluginMap.containsKey(plugin.getName())) {
            log.warn("插件 {} 已注册，跳过", plugin.getName());
            return;
        }
        
        try {
            plugins.add(plugin);
            pluginMap.put(plugin.getName(), plugin);
            
            // 按优先级排序（从高到低）
            plugins.sort(Comparator.comparingInt(MessagePlugin::getPriority).reversed());
            
            log.info("已注册插件: {} (优先级: {})", 
                plugin.getName(), plugin.getPriority());
        } catch (Exception e) {
            log.error("注册插件失败: {}", plugin.getName(), e);
        }
    }
    
    /**
     * 注销插件
     */
    public void unregisterPlugin(String pluginName) {
        MessagePlugin plugin = pluginMap.remove(pluginName);
        if (plugin != null) {
            plugins.remove(plugin);
            try {
                plugin.destroy();
                log.info("已注销插件: {}", pluginName);
            } catch (Exception e) {
                log.error("销毁插件时出错: {}", pluginName, e);
            }
        }
    }
    
    /**
     * 处理消息
     */
    public void process(BaseMessageEntity entity, TdApi.Message originalMessage) {
        if (plugins.isEmpty()) {
            log.warn("⚠️ [插件管理器] 未注册插件，消息将不会被处理");
            return;
        }
        
        log.info("🔌 [插件管理器] 开始执行插件链: chatId={}, messageId={}, 插件数量={}", 
            entity.getChatId(), entity.getMessageId(), plugins.size());
        
        PluginContext context = new PluginContext(originalMessage);
        
        for (MessagePlugin plugin : plugins) {
            long startTime = System.currentTimeMillis();
            
            try {
                log.info("▶️ [插件执行] 执行插件: {}, chatId={}, messageId={}", 
                    plugin.getName(), entity.getChatId(), entity.getMessageId());
                
                PluginResult result = plugin.handle(entity, context);
                
                long executionTime = System.currentTimeMillis() - startTime;
                recordExecutionTime(plugin.getName(), executionTime);
                
                log.info("✅ [插件完成] 插件执行完成: {}, chatId={}, messageId={}, 耗时={}ms, 结果={}", 
                    plugin.getName(), entity.getChatId(), entity.getMessageId(), 
                    executionTime, result);
                
                if (result == PluginResult.STOP) {
                    log.info("⏹️ [插件停止] 插件停止了处理链: {}, chatId={}, messageId={}", 
                        plugin.getName(), entity.getChatId(), entity.getMessageId());
                    break;
                }
            } catch (Exception e) {
                log.error("❌ [插件错误] 执行插件时出错: {}, chatId={}, messageId={}, error={}", 
                    plugin.getName(), entity.getChatId(), entity.getMessageId(), 
                    e.getMessage(), e);
                // 继续执行下一个插件
            }
        }
        
        log.info("✅ [插件管理器] 插件链执行完成: chatId={}, messageId={}", 
            entity.getChatId(), entity.getMessageId());
    }
    
    /**
     * 记录执行时间
     */
    private void recordExecutionTime(String pluginName, long time) {
        executionTimes.merge(pluginName, time, Long::sum);
    }
    
    /**
     * 获取插件执行统计
     */
    public Map<String, Long> getExecutionStats() {
        return new HashMap<>(executionTimes);
    }
    
    /**
     * 获取所有插件
     */
    public List<MessagePlugin> getPlugins() {
        return new ArrayList<>(plugins);
    }
    
    /**
     * 获取插件
     */
    public MessagePlugin getPlugin(String name) {
        return pluginMap.get(name);
    }
    
    /**
     * 启用插件
     */
    public void enablePlugin(String name) {
        MessagePlugin plugin = pluginMap.get(name);
        if (plugin instanceof AbstractMessagePlugin) {
            ((AbstractMessagePlugin) plugin).setEnabled(true);
            log.info("已启用插件: {}", name);
        }
    }
    
    /**
     * 禁用插件
     */
    public void disablePlugin(String name) {
        MessagePlugin plugin = pluginMap.get(name);
        if (plugin instanceof AbstractMessagePlugin) {
            ((AbstractMessagePlugin) plugin).setEnabled(false);
            log.info("已禁用插件: {}", name);
        }
    }
    
    /**
     * 应用关闭时销毁所有插件
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭插件管理器，销毁 {} 个插件", plugins.size());
        for (MessagePlugin plugin : plugins) {
            try {
                plugin.destroy();
            } catch (Exception e) {
                log.error("销毁插件时出错: {}", plugin.getName(), e);
            }
        }
        plugins.clear();
        pluginMap.clear();
    }


    /**
     * 扫描所有 MessagePlugin Bean
     * <p>
     * 从 Spring 容器中获取所有实现 MessagePlugin 接口的 Bean，
     * 并按优先级降序排序（优先级数字越大越早执行）
     * </p>
     *
     * @return 排序后的插件列表
     */
    public List<MessagePlugin> scanPlugins() {
        log.info("开始扫描 MessagePlugin Bean...");

        // 获取所有 MessagePlugin Bean
        List<MessagePlugin> scannedPlugins = new ArrayList<>(plugins);

        // 按优先级降序排序
        scannedPlugins.sort(Comparator.comparingInt(MessagePlugin::getPriority).reversed());

        log.info("扫描完成，发现 {} 个插件", scannedPlugins.size());
        return scannedPlugins;
    }

    /**
     * 初始化插件列表
     * <p>
     * 调用每个插件的初始化方法。如果某个插件初始化失败，
     * 会记录错误但继续初始化其他插件（非致命错误）
     * </p>
     *
     * @param pluginsToInitialize 要初始化的插件列表
     */
    public void initializePlugins(List<MessagePlugin> pluginsToInitialize) {
        log.info("开始初始化 {} 个插件...", pluginsToInitialize.size());

        int successCount = 0;
        int failureCount = 0;

        for (MessagePlugin plugin : pluginsToInitialize) {
            try {
                log.info("初始化插件: {} (优先级: {})", plugin.getName(), plugin.getPriority());
                plugin.initialize();
                successCount++;
                log.info("✅ 插件初始化成功: {}", plugin.getName());
            } catch (Exception e) {
                failureCount++;
                log.error("❌ 插件初始化失败: {} - {}", plugin.getName(), e.getMessage(), e);
                // 继续初始化其他插件
            }
        }

        log.info("插件初始化完成: 成功 {} 个，失败 {} 个", successCount, failureCount);
    }

}
