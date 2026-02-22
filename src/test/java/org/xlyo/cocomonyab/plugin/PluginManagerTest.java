package org.xlyo.cocomonyab.plugin;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PluginManager 单元测试
 * 测试插件注册、注销、enable/disable功能、配置验证和shutdown清理
 */
class PluginManagerTest {
    
    private PluginManager pluginManager;
    
    @BeforeEach
    void setUp() {
        pluginManager = new PluginManager();
    }
    
    /**
     * 测试插件注册
     * Requirements: 4.4
     */
    @Test
    void testRegisterPlugin() {
        TestPlugin plugin = new TestPlugin("TestPlugin", 10);
        
        pluginManager.registerPlugin(plugin);
        
        List<MessagePlugin> plugins = pluginManager.getPlugins();
        assertEquals(1, plugins.size());
        assertEquals("TestPlugin", plugins.get(0).getName());
        assertEquals(10, plugins.get(0).getPriority());
    }
    
    /**
     * 测试重复注册同名插件
     * Requirements: 4.4
     */
    @Test
    void testRegisterDuplicatePlugin() {
        TestPlugin plugin1 = new TestPlugin("TestPlugin", 10);
        TestPlugin plugin2 = new TestPlugin("TestPlugin", 20);
        
        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(plugin2); // 应该被忽略
        
        List<MessagePlugin> plugins = pluginManager.getPlugins();
        assertEquals(1, plugins.size());
        assertEquals(10, plugins.get(0).getPriority()); // 保持第一个插件的优先级
    }
    
    /**
     * 测试插件注销
     * Requirements: 4.4
     */
    @Test
    void testUnregisterPlugin() {
        TestPlugin plugin = new TestPlugin("TestPlugin", 10);
        
        pluginManager.registerPlugin(plugin);
        assertEquals(1, pluginManager.getPlugins().size());
        
        pluginManager.unregisterPlugin("TestPlugin");
        assertEquals(0, pluginManager.getPlugins().size());
    }
    
    /**
     * 测试注销不存在的插件
     * Requirements: 4.4
     */
    @Test
    void testUnregisterNonExistentPlugin() {
        pluginManager.unregisterPlugin("NonExistent");
        // 应该不抛出异常
        assertEquals(0, pluginManager.getPlugins().size());
    }
    
    /**
     * 测试启用插件
     * Requirements: 4.5
     */
    @Test
    void testEnablePlugin() {
        TestPlugin plugin = new TestPlugin("TestPlugin", 10);
        plugin.setEnabled(false);
        
        pluginManager.registerPlugin(plugin);
        pluginManager.enablePlugin("TestPlugin");
        
        assertTrue(plugin.isEnabled());
    }
    
    /**
     * 测试禁用插件
     * Requirements: 4.5
     */
    @Test
    void testDisablePlugin() {
        TestPlugin plugin = new TestPlugin("TestPlugin", 10);
        
        pluginManager.registerPlugin(plugin);
        pluginManager.disablePlugin("TestPlugin");
        
        assertFalse(plugin.isEnabled());
    }
    
    /**
     * 测试禁用的插件不执行
     * Requirements: 4.5
     */
    @Test
    void testDisabledPluginDoesNotExecute() {
        CountingPlugin plugin = new CountingPlugin("TestPlugin", 10);
        plugin.setEnabled(false);
        
        pluginManager.registerPlugin(plugin);
        
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        pluginManager.process(entity, message);
        
        assertEquals(0, plugin.executionCount);
    }
    
    /**
     * 测试获取插件
     * Requirements: 4.4
     */
    @Test
    void testGetPlugin() {
        TestPlugin plugin = new TestPlugin("TestPlugin", 10);
        
        pluginManager.registerPlugin(plugin);
        
        MessagePlugin retrieved = pluginManager.getPlugin("TestPlugin");
        assertNotNull(retrieved);
        assertEquals("TestPlugin", retrieved.getName());
    }
    
    /**
     * 测试获取不存在的插件
     * Requirements: 4.4
     */
    @Test
    void testGetNonExistentPlugin() {
        MessagePlugin retrieved = pluginManager.getPlugin("NonExistent");
        assertNull(retrieved);
    }
    
    /**
     * 测试插件执行统计
     * Requirements: 13.1
     */
    @Test
    void testExecutionStats() {
        TestPlugin plugin1 = new TestPlugin("Plugin1", 10);
        TestPlugin plugin2 = new TestPlugin("Plugin2", 5);
        
        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(plugin2);
        
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        pluginManager.process(entity, message);
        
        Map<String, Long> stats = pluginManager.getExecutionStats();
        assertTrue(stats.containsKey("Plugin1"));
        assertTrue(stats.containsKey("Plugin2"));
        assertTrue(stats.get("Plugin1") >= 0);
        assertTrue(stats.get("Plugin2") >= 0);
    }
    
    /**
     * 测试shutdown清理
     * Requirements: 13.2, 13.3
     */
    @Test
    void testShutdown() {
        TestPlugin plugin1 = new TestPlugin("Plugin1", 10);
        TestPlugin plugin2 = new TestPlugin("Plugin2", 5);
        
        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(plugin2);
        
        assertEquals(2, pluginManager.getPlugins().size());
        
        pluginManager.shutdown();
        
        assertEquals(0, pluginManager.getPlugins().size());
        assertNull(pluginManager.getPlugin("Plugin1"));
        assertNull(pluginManager.getPlugin("Plugin2"));
    }
    
    /**
     * 测试插件初始化失败时的处理
     * Requirements: 4.6
     */
    @Test
    void testRegisterPluginWithInitializationFailure() {
        FailingInitPlugin plugin = new FailingInitPlugin("FailingPlugin", 10);
        
        pluginManager.registerPlugin(plugin);
        
        // 插件注册失败，不应该被添加到列表中
        assertEquals(0, pluginManager.getPlugins().size());
    }
    
    /**
     * 测试空插件列表处理消息
     * Requirements: 13.1
     */
    @Test
    void testProcessWithNoPlugins() {
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> pluginManager.process(entity, message));
    }
    
    /**
     * 测试多个插件按优先级排序
     * Requirements: 4.4
     */
    @Test
    void testMultiplePluginsPrioritySorting() {
        TestPlugin plugin1 = new TestPlugin("Plugin1", 5);
        TestPlugin plugin2 = new TestPlugin("Plugin2", 10);
        TestPlugin plugin3 = new TestPlugin("Plugin3", 1);
        
        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(plugin2);
        pluginManager.registerPlugin(plugin3);
        
        List<MessagePlugin> plugins = pluginManager.getPlugins();
        assertEquals(3, plugins.size());
        assertEquals("Plugin2", plugins.get(0).getName()); // 优先级10
        assertEquals("Plugin1", plugins.get(1).getName()); // 优先级5
        assertEquals("Plugin3", plugins.get(2).getName()); // 优先级1
    }
    
    // ========== 辅助方法和测试插件类 ==========
    
    private BaseMessageEntity createTestEntity() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setTextContent("Test message");
        entity.setDate((int) (System.currentTimeMillis() / 1000));
        return entity;
    }
    
    private TdApi.Message createTestMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = 12345L;
        message.chatId = 67890L;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.content = new TdApi.MessageText(new TdApi.FormattedText("Test", new TdApi.TextEntity[0]), null, null);
        return message;
    }
    
    /**
     * 基础测试插件
     */
    static class TestPlugin extends AbstractMessagePlugin {
        private final String name;
        private final int priority;
        
        TestPlugin(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
            return PluginResult.CONTINUE;
        }
    }
    
    /**
     * 用于测试执行计数的插件
     */
    static class CountingPlugin extends TestPlugin {
        int executionCount = 0;
        
        CountingPlugin(String name, int priority) {
            super(name, priority);
        }
        
        @Override
        protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
            executionCount++;
            return PluginResult.CONTINUE;
        }
    }
    
    /**
     * 初始化失败的插件
     */
    static class FailingInitPlugin extends TestPlugin {
        FailingInitPlugin(String name, int priority) {
            super(name, priority);
        }
        
        @Override
        public void initialize() {
            throw new RuntimeException("Initialization failed");
        }
    }
}
