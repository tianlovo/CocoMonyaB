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
            log.info("Plugin system is disabled");
            return;
        }
        
        log.info("Registering plugins from configuration");
        
        // 1. 注册Spring容器中的MessagePlugin beans
        registerSpringManagedPlugins();
        
        // 2. 注册配置文件中定义的插件
        registerConfiguredPlugins();
        
        log.info("Plugin registration completed, {} plugins registered", 
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
                    log.info("Registered Spring-managed plugin: {}", plugin.getName());
                } catch (Exception e) {
                    log.error("Failed to register Spring-managed plugin: {}", 
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
                log.info("Plugin {} is disabled in configuration", config.getClassName());
                continue;
            }
            
            try {
                MessagePlugin plugin = loadPlugin(config);
                pluginManager.registerPlugin(plugin);
                log.info("Registered configured plugin: {} with priority {}", 
                    config.getClassName(), config.getPriority());
            } catch (Exception e) {
                log.error("Failed to load plugin: {}", config.getClassName(), e);
            }
        }
    }
    
    /**
     * 验证插件配置
     */
    private boolean validatePluginConfig(PluginProperties.PluginConfig config) {
        if (config.getClassName() == null || config.getClassName().trim().isEmpty()) {
            log.error("Plugin configuration missing className");
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
                "Class " + config.getClassName() + " does not implement MessagePlugin interface");
        }
        
        return (MessagePlugin) pluginClass.getDeclaredConstructor().newInstance();
    }
}
