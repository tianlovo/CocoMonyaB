package org.xlyo.cocomonyab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件配置属性
 * 从application.yaml加载插件配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "message.plugin")
public class PluginProperties {
    
    /**
     * 是否启用插件系统
     */
    private boolean enabled = true;
    
    /**
     * 插件配置列表
     */
    private List<PluginConfig> plugins = new ArrayList<>();
    
    /**
     * 插件配置类
     */
    @Data
    public static class PluginConfig {
        /**
         * 插件类名（全限定名）
         */
        private String className;
        
        /**
         * 插件优先级
         */
        private int priority = 0;
        
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * 插件配置参数
         */
        private Map<String, Object> properties = new HashMap<>();
    }
}
