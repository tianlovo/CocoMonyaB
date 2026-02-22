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
            log.warn("Plugin {} already registered, skipping", plugin.getName());
            return;
        }
        
        try {
            plugin.initialize();
            plugins.add(plugin);
            pluginMap.put(plugin.getName(), plugin);
            
            // 按优先级排序（从高到低）
            plugins.sort(Comparator.comparingInt(MessagePlugin::getPriority).reversed());
            
            log.info("Registered plugin: {} with priority {}", 
                plugin.getName(), plugin.getPriority());
        } catch (Exception e) {
            log.error("Failed to register plugin: {}", plugin.getName(), e);
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
                log.info("Unregistered plugin: {}", pluginName);
            } catch (Exception e) {
                log.error("Error destroying plugin: {}", pluginName, e);
            }
        }
    }
    
    /**
     * 处理消息
     */
    public void process(BaseMessageEntity entity, TdApi.Message originalMessage) {
        if (plugins.isEmpty()) {
            log.warn("No plugins registered, message will not be processed");
            return;
        }
        
        PluginContext context = new PluginContext(originalMessage);
        
        for (MessagePlugin plugin : plugins) {
            long startTime = System.currentTimeMillis();
            
            try {
                PluginResult result = plugin.handle(entity, context);
                
                long executionTime = System.currentTimeMillis() - startTime;
                recordExecutionTime(plugin.getName(), executionTime);
                
                log.debug("Plugin {} executed in {}ms, result: {}", 
                    plugin.getName(), executionTime, result);
                
                if (result == PluginResult.STOP) {
                    log.debug("Plugin {} stopped the chain", plugin.getName());
                    break;
                }
            } catch (Exception e) {
                log.error("Error executing plugin {}: {}", 
                    plugin.getName(), e.getMessage(), e);
                // 继续执行下一个插件
            }
        }
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
            log.info("Enabled plugin: {}", name);
        }
    }
    
    /**
     * 禁用插件
     */
    public void disablePlugin(String name) {
        MessagePlugin plugin = pluginMap.get(name);
        if (plugin instanceof AbstractMessagePlugin) {
            ((AbstractMessagePlugin) plugin).setEnabled(false);
            log.info("Disabled plugin: {}", name);
        }
    }
    
    /**
     * 应用关闭时销毁所有插件
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down plugin manager, destroying {} plugins", plugins.size());
        for (MessagePlugin plugin : plugins) {
            try {
                plugin.destroy();
            } catch (Exception e) {
                log.error("Error destroying plugin: {}", plugin.getName(), e);
            }
        }
        plugins.clear();
        pluginMap.clear();
    }
}
