package org.xlyo.cocomonyab.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.xlyo.cocomonyab.plugin.MessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.plugin.impl.ConsolePrinterPlugin;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 插件自动配置测试
 * 测试插件自动注册和配置验证
 */
class PluginAutoConfigurationTest {
    
    private PluginAutoConfiguration pluginAutoConfiguration;
    private PluginProperties pluginProperties;
    private PluginManager pluginManager;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        pluginProperties = new PluginProperties();
        pluginProperties.setEnabled(true);
        
        pluginManager = new PluginManager();
        
        pluginAutoConfiguration = new PluginAutoConfiguration(
            pluginProperties,
            pluginManager,
            applicationContext
        );
    }
    
    /**
     * 测试插件自动配置对象创建
     */
    @Test
    void testPluginAutoConfigurationCreated() {
        assertNotNull(pluginAutoConfiguration);
    }
    
    /**
     * 测试Spring管理的插件被自动注册
     */
    @Test
    void testSpringManagedPluginsAutoRegistered() {
        // 模拟Spring容器中的插件
        Map<String, MessagePlugin> pluginBeans = new HashMap<>();
        pluginBeans.put("consolePrinterPlugin", new ConsolePrinterPlugin());
        
        when(applicationContext.getBeansOfType(MessagePlugin.class))
            .thenReturn(pluginBeans);
        
        // 执行注册
        pluginAutoConfiguration.registerPlugins();
        
        // 验证插件被注册
        assertEquals(1, pluginManager.getPlugins().size());
        assertEquals("ConsolePrinterPlugin", 
            pluginManager.getPlugins().get(0).getName());
    }
    
    /**
     * 测试插件系统禁用时不注册插件
     */
    @Test
    void testPluginSystemDisabled() {
        pluginProperties.setEnabled(false);
        
        Map<String, MessagePlugin> pluginBeans = new HashMap<>();
        pluginBeans.put("consolePrinterPlugin", new ConsolePrinterPlugin());
        
        when(applicationContext.getBeansOfType(MessagePlugin.class))
            .thenReturn(pluginBeans);
        
        // 执行注册
        pluginAutoConfiguration.registerPlugins();
        
        // 验证没有插件被注册
        assertEquals(0, pluginManager.getPlugins().size());
    }
    
    /**
     * 测试配置文件中的插件被注册
     */
    @Test
    void testConfiguredPluginsRegistered() {
        // 添加配置
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("org.xlyo.cocomonyab.plugin.impl.ConsolePrinterPlugin");
        config.setPriority(5);
        config.setEnabled(true);
        pluginProperties.getPlugins().add(config);
        
        when(applicationContext.getBeansOfType(MessagePlugin.class))
            .thenReturn(new HashMap<>());
        
        // 执行注册
        pluginAutoConfiguration.registerPlugins();
        
        // 验证插件被注册
        assertEquals(1, pluginManager.getPlugins().size());
    }
    
    /**
     * 测试禁用的插件不被注册
     */
    @Test
    void testDisabledPluginNotRegistered() {
        // 添加禁用的配置
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("org.xlyo.cocomonyab.plugin.impl.ConsolePrinterPlugin");
        config.setPriority(5);
        config.setEnabled(false);
        pluginProperties.getPlugins().add(config);
        
        when(applicationContext.getBeansOfType(MessagePlugin.class))
            .thenReturn(new HashMap<>());
        
        // 执行注册
        pluginAutoConfiguration.registerPlugins();
        
        // 验证插件未被注册
        assertEquals(0, pluginManager.getPlugins().size());
    }
    
    /**
     * 测试无效类名的插件配置被跳过
     */
    @Test
    void testInvalidClassNameSkipped() {
        // 添加无效的配置
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("com.invalid.NonExistentPlugin");
        config.setPriority(5);
        config.setEnabled(true);
        pluginProperties.getPlugins().add(config);
        
        when(applicationContext.getBeansOfType(MessagePlugin.class))
            .thenReturn(new HashMap<>());
        
        // 执行注册（不应抛出异常）
        assertDoesNotThrow(() -> pluginAutoConfiguration.registerPlugins());
        
        // 验证没有插件被注册
        assertEquals(0, pluginManager.getPlugins().size());
    }
}
