package org.xlyo.cocomonyab.plugin;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * PluginManager 属性测试
 * 使用 jqwik 进行基于属性的测试
 */
class PluginManagerPropertyTest {
    
    /**
     * Property 5: Plugin Priority Ordering
     * 验证插件按优先级从高到低排序
     * 
     * **Validates: Requirements 4.2, 4.3, 5.1**
     */
    @Property(tries = 100)
    @Label("Property 5: Plugin Priority Ordering")
    void pluginsShouldBeSortedByPriorityDescending(
            @ForAll @Size(min = 2, max = 10) List<@IntRange(min = -100, max = 100) Integer> priorities) {
        
        PluginManager manager = new PluginManager();
        
        // 创建并注册具有不同优先级的插件
        for (int i = 0; i < priorities.size(); i++) {
            int priority = priorities.get(i);
            TestPlugin plugin = new TestPlugin("Plugin" + i, priority);
            manager.registerPlugin(plugin);
        }
        
        // 获取注册的插件列表
        List<MessagePlugin> registeredPlugins = manager.getPlugins();
        
        // 验证插件按优先级从高到低排序
        for (int i = 0; i < registeredPlugins.size() - 1; i++) {
            int currentPriority = registeredPlugins.get(i).getPriority();
            int nextPriority = registeredPlugins.get(i + 1).getPriority();
            
            if (currentPriority < nextPriority) {
                throw new AssertionError(
                    String.format("Plugins not sorted correctly: plugin at index %d has priority %d, " +
                        "but plugin at index %d has priority %d", 
                        i, currentPriority, i + 1, nextPriority));
            }
        }
    }
    
    /**
     * Property 6: Plugin Sequential Execution
     * 验证插件按顺序执行（不并发）
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    @Label("Property 6: Plugin Sequential Execution")
    void pluginsShouldExecuteSequentially(
            @ForAll @Size(min = 2, max = 5) List<@IntRange(min = 0, max = 100) Integer> priorities) {
        
        PluginManager manager = new PluginManager();
        List<SequentialTestPlugin> plugins = new ArrayList<>();
        
        // 创建并注册插件
        for (int i = 0; i < priorities.size(); i++) {
            SequentialTestPlugin plugin = new SequentialTestPlugin("Plugin" + i, priorities.get(i));
            plugins.add(plugin);
            manager.registerPlugin(plugin);
        }
        
        // 处理消息
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        manager.process(entity, message);
        
        // 验证执行顺序：每个插件的结束时间应该在下一个插件的开始时间之前
        List<SequentialTestPlugin> executedPlugins = plugins.stream()
            .filter(p -> p.startTime > 0)
            .sorted(Comparator.comparingLong(p -> p.startTime))
            .collect(Collectors.toList());
        
        for (int i = 0; i < executedPlugins.size() - 1; i++) {
            SequentialTestPlugin current = executedPlugins.get(i);
            SequentialTestPlugin next = executedPlugins.get(i + 1);
            
            if (current.endTime > next.startTime) {
                throw new AssertionError(
                    String.format("Plugins executed concurrently: %s ended at %d, but %s started at %d",
                        current.getName(), current.endTime, next.getName(), next.startTime));
            }
        }
    }
    
    /**
     * Property 7: Plugin Chain Control
     * 验证当插件返回STOP时，后续插件不执行
     * 
     * **Validates: Requirements 5.3, 5.5**
     */
    @Property(tries = 100)
    @Label("Property 7: Plugin Chain Control")
    void pluginChainShouldStopWhenPluginReturnsStop(
            @ForAll @IntRange(min = 0, max = 4) int stopIndex,
            @ForAll @Size(5) List<@IntRange(min = 0, max = 100) Integer> priorities) {
        
        PluginManager manager = new PluginManager();
        List<CountingPlugin> plugins = new ArrayList<>();
        
        // 创建插件，其中一个返回STOP
        for (int i = 0; i < priorities.size(); i++) {
            PluginResult result = (i == stopIndex) ? PluginResult.STOP : PluginResult.CONTINUE;
            CountingPlugin plugin = new CountingPlugin("Plugin" + i, priorities.get(i), result);
            plugins.add(plugin);
            manager.registerPlugin(plugin);
        }
        
        // 处理消息
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        manager.process(entity, message);
        
        // 获取按优先级排序的插件列表
        List<MessagePlugin> sortedPlugins = manager.getPlugins();
        
        // 找到返回STOP的插件在排序后列表中的位置
        int stopPluginIndex = -1;
        for (int i = 0; i < sortedPlugins.size(); i++) {
            if (sortedPlugins.get(i).getName().equals("Plugin" + stopIndex)) {
                stopPluginIndex = i;
                break;
            }
        }
        
        // 验证：STOP插件之前的所有插件都应该执行
        for (int i = 0; i < stopPluginIndex; i++) {
            CountingPlugin plugin = (CountingPlugin) sortedPlugins.get(i);
            if (plugin.executionCount.get() != 1) {
                throw new AssertionError(
                    String.format("Plugin %s before STOP plugin should have executed once, but executed %d times",
                        plugin.getName(), plugin.executionCount.get()));
            }
        }
        
        // 验证：STOP插件应该执行
        CountingPlugin stopPlugin = (CountingPlugin) sortedPlugins.get(stopPluginIndex);
        if (stopPlugin.executionCount.get() != 1) {
            throw new AssertionError(
                String.format("STOP plugin %s should have executed once, but executed %d times",
                    stopPlugin.getName(), stopPlugin.executionCount.get()));
        }
        
        // 验证：STOP插件之后的所有插件都不应该执行
        for (int i = stopPluginIndex + 1; i < sortedPlugins.size(); i++) {
            CountingPlugin plugin = (CountingPlugin) sortedPlugins.get(i);
            if (plugin.executionCount.get() != 0) {
                throw new AssertionError(
                    String.format("Plugin %s after STOP plugin should not have executed, but executed %d times",
                        plugin.getName(), plugin.executionCount.get()));
            }
        }
    }
    
    /**
     * Property 8: Plugin Error Resilience
     * 验证插件抛出异常时，后续插件仍然执行
     * 
     * **Validates: Requirements 5.4, 14.2**
     */
    @Property(tries = 100)
    @Label("Property 8: Plugin Error Resilience")
    void pluginChainShouldContinueAfterPluginThrowsException(
            @ForAll @IntRange(min = 0, max = 4) int errorIndex,
            @ForAll @Size(5) List<@IntRange(min = 0, max = 100) Integer> priorities) {
        
        PluginManager manager = new PluginManager();
        List<CountingPlugin> plugins = new ArrayList<>();
        
        // 创建插件，其中一个会抛出异常
        for (int i = 0; i < priorities.size(); i++) {
            boolean shouldThrow = (i == errorIndex);
            CountingPlugin plugin = new CountingPlugin("Plugin" + i, priorities.get(i), 
                PluginResult.CONTINUE, shouldThrow);
            plugins.add(plugin);
            manager.registerPlugin(plugin);
        }
        
        // 处理消息
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        manager.process(entity, message);
        
        // 获取按优先级排序的插件列表
        List<MessagePlugin> sortedPlugins = manager.getPlugins();
        
        // 验证：所有插件都应该被调用（包括抛出异常的插件）
        for (MessagePlugin plugin : sortedPlugins) {
            CountingPlugin countingPlugin = (CountingPlugin) plugin;
            if (countingPlugin.executionCount.get() != 1) {
                throw new AssertionError(
                    String.format("Plugin %s should have been called once despite errors, but was called %d times",
                        plugin.getName(), countingPlugin.executionCount.get()));
            }
        }
    }
    
    /**
     * Property 12: Plugin Execution Time Recording
     * 验证插件执行时间被正确记录
     * 
     * **Validates: Requirements 14.3**
     */
    @Property(tries = 100)
    @Label("Property 12: Plugin Execution Time Recording")
    void pluginExecutionTimeShouldBeRecorded(
            @ForAll @Size(min = 1, max = 5) List<@IntRange(min = 0, max = 100) Integer> priorities) {
        
        PluginManager manager = new PluginManager();
        List<String> pluginNames = new ArrayList<>();
        
        // 创建并注册插件（使用带延迟的插件确保执行时间可测量）
        for (int i = 0; i < priorities.size(); i++) {
            String name = "Plugin" + i;
            pluginNames.add(name);
            DelayedPlugin plugin = new DelayedPlugin(name, priorities.get(i), 1); // 1ms delay
            manager.registerPlugin(plugin);
        }
        
        // 处理消息
        BaseMessageEntity entity = createTestEntity();
        TdApi.Message message = createTestMessage();
        manager.process(entity, message);
        
        // 获取执行统计
        Map<String, Long> stats = manager.getExecutionStats();
        
        // 验证：所有插件的执行时间都应该被记录
        for (String pluginName : pluginNames) {
            if (!stats.containsKey(pluginName)) {
                throw new AssertionError(
                    String.format("Execution time for plugin %s was not recorded", pluginName));
            }
            
            Long executionTime = stats.get(pluginName);
            if (executionTime == null || executionTime < 0) {
                throw new AssertionError(
                    String.format("Invalid execution time for plugin %s: %d", pluginName, executionTime));
            }
        }
        
        // 多次处理消息，验证执行时间累加
        manager.process(entity, message);
        Map<String, Long> stats2 = manager.getExecutionStats();
        
        for (String pluginName : pluginNames) {
            Long time1 = stats.get(pluginName);
            Long time2 = stats2.get(pluginName);
            
            if (time2 < time1) {
                throw new AssertionError(
                    String.format("Execution time for plugin %s should accumulate: first=%d, second=%d",
                        pluginName, time1, time2));
            }
        }
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
    static class TestPlugin implements MessagePlugin {
        private final String name;
        private final int priority;
        private boolean enabled = true;
        
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
        public boolean isEnabled() {
            return enabled;
        }
        
        @Override
        public void initialize() {
            // Do nothing
        }
        
        @Override
        public PluginResult handle(BaseMessageEntity entity, PluginContext context) {
            return PluginResult.CONTINUE;
        }
        
        @Override
        public void destroy() {
            // Do nothing
        }
    }
    
    /**
     * 用于测试顺序执行的插件
     */
    static class SequentialTestPlugin extends TestPlugin {
        volatile long startTime = 0;
        volatile long endTime = 0;
        
        SequentialTestPlugin(String name, int priority) {
            super(name, priority);
        }
        
        @Override
        public PluginResult handle(BaseMessageEntity entity, PluginContext context) {
            startTime = System.nanoTime();
            try {
                // 模拟一些处理时间
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            endTime = System.nanoTime();
            return PluginResult.CONTINUE;
        }
    }
    
    /**
     * 用于测试执行计数和链控制的插件
     */
    static class CountingPlugin extends TestPlugin {
        final AtomicInteger executionCount = new AtomicInteger(0);
        private final PluginResult resultToReturn;
        private final boolean shouldThrowException;
        
        CountingPlugin(String name, int priority, PluginResult resultToReturn) {
            this(name, priority, resultToReturn, false);
        }
        
        CountingPlugin(String name, int priority, PluginResult resultToReturn, boolean shouldThrowException) {
            super(name, priority);
            this.resultToReturn = resultToReturn;
            this.shouldThrowException = shouldThrowException;
        }
        
        @Override
        public PluginResult handle(BaseMessageEntity entity, PluginContext context) {
            executionCount.incrementAndGet();
            
            if (shouldThrowException) {
                throw new RuntimeException("Test exception from " + getName());
            }
            
            return resultToReturn;
        }
    }
    
    /**
     * 用于测试执行时间记录的插件（带延迟）
     */
    static class DelayedPlugin extends TestPlugin {
        private final long delayMs;
        
        DelayedPlugin(String name, int priority, long delayMs) {
            super(name, priority);
            this.delayMs = delayMs;
        }
        
        @Override
        public PluginResult handle(BaseMessageEntity entity, PluginContext context) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return PluginResult.CONTINUE;
        }
    }
}
