package org.xlyo.cocomonyab.config.autoconfigure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.xlyo.cocomonyab.config.properties.PluginProperties;
import org.xlyo.cocomonyab.plugin.MessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginManager;

import jakarta.annotation.PostConstruct;

/**
 * 插件自动配置
 * 负责在应用启动时自动注册插件
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PluginProperties.class)
@RequiredArgsConstructor
public class PluginAutoConfiguration {
    
    private final PluginProperties pluginProperties;
    private final PluginManager pluginManager;
    private final ApplicationContext applicationContext;
    
    /**
     * 应用启动后自动注册插件
     */
    @PostConstruct
    public void registerPlugins() {
        if (!pluginProperties.isEnabled()) {
            log.info("插件系统已禁用");
            return;
        }
        
        log.info("正在从配置中注册插件");
        
        // 1. 注册Spring容器中的MessagePlugin beans
        registerSpringManagedPlugins();
        
        // 2. 注册配置文件中定义的插件
        registerConfiguredPlugins();
        
        log.info("插件注册完成，已注册 {} 个插件", 
            pluginManager.getPlugins().size());
    }
    
    /**
     * 注册Spring容器管理的插件
     */
    private void registerSpringManagedPlugins() {
        applicationContext.getBeansOfType(MessagePlugin.class)
            .values()
            .forEach(plugin -> {
                try {
                    pluginManager.registerPlugin(plugin);
                    log.info("已注册 Spring 管理的插件: {}", plugin.getName());
                } catch (Exception e) {
                    log.error("注册 Spring 管理的插件失败: {}", 
                        plugin.getName(), e);
                }
            });
    }
    
    /**
     * 注册配置文件中定义的插件
     */
    private void registerConfiguredPlugins() {
        for (PluginProperties.PluginConfig config : pluginProperties.getPlugins()) {
            if (!validatePluginConfig(config)) {
                continue;
            }
            
            if (!config.isEnabled()) {
                log.info("插件 {} 在配置中已禁用", config.getClassName());
                continue;
            }
            
            try {
                MessagePlugin plugin = loadPlugin(config);
                pluginManager.registerPlugin(plugin);
                log.info("已注册配置的插件: {}，优先级为 {}", 
                    config.getClassName(), config.getPriority());
            } catch (Exception e) {
                log.error("加载插件失败: {}", config.getClassName(), e);
            }
        }
    }
    
    /**
     * 验证插件配置
     */
    private boolean validatePluginConfig(PluginProperties.PluginConfig config) {
        if (config.getClassName() == null || config.getClassName().trim().isEmpty()) {
            log.error("插件配置缺少 className");
            return false;
        }
        
        return true;
    }
    
    /**
     * 加载插件实例
     */
    private MessagePlugin loadPlugin(PluginProperties.PluginConfig config) throws Exception {
        Class<?> pluginClass = Class.forName(config.getClassName());
        
        if (!MessagePlugin.class.isAssignableFrom(pluginClass)) {
            throw new IllegalArgumentException(
                "类 " + config.getClassName() + " 未实现 MessagePlugin 接口");
        }
        
        return (MessagePlugin) pluginClass.getDeclaredConstructor().newInstance();
    }
}
