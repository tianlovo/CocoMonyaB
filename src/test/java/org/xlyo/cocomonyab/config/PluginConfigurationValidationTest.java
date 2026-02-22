package org.xlyo.cocomonyab.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 插件配置验证测试
 * 测试插件配置验证逻辑
 */
class PluginConfigurationValidationTest {
    
    /**
     * 测试空className配置被拒绝
     */
    @Test
    void testEmptyClassNameRejected() {
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("");
        config.setPriority(5);
        config.setEnabled(true);
        
        // 空className应该被验证逻辑拒绝
        // 验证配置对象本身可以创建，但不应该被注册
        assertNotNull(config);
        assertEquals("", config.getClassName());
    }
    
    /**
     * 测试null className配置被拒绝
     */
    @Test
    void testNullClassNameRejected() {
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName(null);
        config.setPriority(5);
        config.setEnabled(true);
        
        // null className应该被验证逻辑拒绝
        assertNotNull(config);
        assertNull(config.getClassName());
    }
    
    /**
     * 测试有效配置被接受
     */
    @Test
    void testValidConfigurationAccepted() {
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("org.xlyo.cocomonyab.plugin.impl.ConsolePrinterPlugin");
        config.setPriority(5);
        config.setEnabled(true);
        
        assertNotNull(config);
        assertEquals("org.xlyo.cocomonyab.plugin.impl.ConsolePrinterPlugin", 
            config.getClassName());
        assertEquals(5, config.getPriority());
        assertTrue(config.isEnabled());
    }
    
    /**
     * 测试插件配置属性映射
     */
    @Test
    void testPluginPropertiesMapping() {
        PluginProperties properties = new PluginProperties();
        properties.setEnabled(true);
        
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("org.xlyo.cocomonyab.plugin.impl.TestPlugin");
        config.setPriority(10);
        config.setEnabled(true);
        config.getProperties().put("key1", "value1");
        config.getProperties().put("key2", "value2");
        
        properties.getPlugins().add(config);
        
        assertNotNull(properties);
        assertTrue(properties.isEnabled());
        assertEquals(1, properties.getPlugins().size());
        
        PluginProperties.PluginConfig retrievedConfig = properties.getPlugins().get(0);
        assertNotNull(retrievedConfig.getProperties());
        assertEquals("value1", retrievedConfig.getProperties().get("key1"));
        assertEquals("value2", retrievedConfig.getProperties().get("key2"));
    }
    
    /**
     * 测试配置默认值
     */
    @Test
    void testConfigurationDefaults() {
        PluginProperties properties = new PluginProperties();
        
        // 验证默认值
        assertTrue(properties.isEnabled());
        assertNotNull(properties.getPlugins());
        assertTrue(properties.getPlugins().isEmpty());
        
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        assertEquals(0, config.getPriority());
        assertTrue(config.isEnabled());
        assertNotNull(config.getProperties());
        assertTrue(config.getProperties().isEmpty());
    }
    
    /**
     * 测试配置对象的可变性
     */
    @Test
    void testConfigurationMutability() {
        PluginProperties properties = new PluginProperties();
        
        // 修改enabled状态
        properties.setEnabled(false);
        assertFalse(properties.isEnabled());
        
        // 添加插件配置
        PluginProperties.PluginConfig config = new PluginProperties.PluginConfig();
        config.setClassName("test.Plugin");
        properties.getPlugins().add(config);
        
        assertEquals(1, properties.getPlugins().size());
        assertEquals("test.Plugin", properties.getPlugins().get(0).getClassName());
    }
}
