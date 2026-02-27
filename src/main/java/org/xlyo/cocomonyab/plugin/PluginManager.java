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
     * 注册插件
     */
    public void registerPlugin(MessagePlugin plugin) {
        if (pluginMap.containsKey(plugin.getName())) {
            log.warn("插件 {} 已注册，跳过", plugin.getName());
            return;
        }
        
        try {
            plugin.initialize();
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
}
