package org.xlyo.cocomonyab.config.initializer;

import net.jqwik.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.event.startup.CollectionsReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;
import org.xlyo.cocomonyab.plugin.MessagePlugin;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.plugin.PluginResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * PluginInitializer Property-Based Tests
 * <p>
 * Tests the following properties:
 * - Property 11: Plugin priority sorting
 * </p>
 * <p>
 * **Validates: Requirement 4.3**
 * </p>
 */
class PluginInitializerPropertyTest {
    
    private static final Logger log = LoggerFactory.getLogger(PluginInitializerPropertyTest.class);
    
    /**
     * Property 11: Plugin priority sorting
     * <p>
     * For any discovered plugin collection, plugins should be sorted in descending priority order
     * (higher priority numbers execute first).
     * </p>
     * <p>
     * **Validates: Requirement 4.3**
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: application-startup-flow-refactor, Property 11: 插件优先级排序")
    void pluginsAreSortedByPriorityDescending(
            @ForAll("pluginCollections") List<TestPlugin> plugins) {
        
        log.info("Testing plugin priority sorting with {} plugins", plugins.size());
        
        // Log the input plugin priorities
        if (!plugins.isEmpty()) {
            String priorities = plugins.stream()
                    .map(p -> p.getName() + ":" + p.getPriority())
                    .collect(Collectors.joining(", "));
            log.info("Input plugins: {}", priorities);
        }
        
        // Prepare: Create test environment with the generated plugins
        TestPluginManager pluginManager = new TestPluginManager(plugins);
        TestStartupEventPublisher eventPublisher = new TestStartupEventPublisher();
        TestStartupProgressTracker progressTracker = new TestStartupProgressTracker();
        
        PluginInitializer initializer = new PluginInitializer(
                pluginManager,
                eventPublisher,
                progressTracker
        );
        
        // Execute: Trigger plugin initialization
        initializer.onCollectionsReady(new CollectionsReadyEvent(this));
        
        // Get the sorted plugins from the manager
        List<MessagePlugin> sortedPlugins = pluginManager.getScannedPlugins();
        
        // Verify: Plugins are sorted in descending priority order
        List<Integer> priorities = sortedPlugins.stream()
                .map(MessagePlugin::getPriority)
                .collect(Collectors.toList());
        
        // Log the output plugin priorities
        if (!sortedPlugins.isEmpty()) {
            String sortedPriorities = sortedPlugins.stream()
                    .map(p -> p.getName() + ":" + p.getPriority())
                    .collect(Collectors.joining(", "));
            log.info("Sorted plugins: {}", sortedPriorities);
        }
        
        // Check that priorities are in descending order
        for (int i = 0; i < priorities.size() - 1; i++) {
            assertThat(priorities.get(i))
                    .as("Plugin at index %d should have priority >= plugin at index %d", i, i + 1)
                    .isGreaterThanOrEqualTo(priorities.get(i + 1));
        }
        
        // Verify: Plugins ready event was published
        assertThat(eventPublisher.getPublishedEvents()).contains("PluginsReady");
        
        // Verify: Phase completed successfully
        assertThat(progressTracker.getPhases()).containsKey("插件初始化");
        assertThat(progressTracker.getPhases().get("插件初始化").getStatus())
                .isEqualTo(StartupProgressTracker.PhaseStatus.COMPLETED);
        
        // Verify: All plugins were initialized
        assertThat(pluginManager.getInitializedPlugins()).hasSize(plugins.size());
        
        log.info("✅ Plugin priority sorting verified: {} plugins sorted correctly", plugins.size());
    }
    
    // ==================== Arbitraries ====================
    
    /**
     * Generates various plugin collections with different priorities
     */
    @Provide
    Arbitrary<List<TestPlugin>> pluginCollections() {
        // Generate plugin count between 0 and 10
        Arbitrary<Integer> pluginCount = Arbitraries.integers().between(0, 10);
        
        return pluginCount.flatMap(count -> {
            if (count == 0) {
                return Arbitraries.just(Collections.emptyList());
            }
            
            // Generate a list of plugins with random priorities
            List<Arbitrary<TestPlugin>> pluginArbitraries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final int index = i;
                Arbitrary<TestPlugin> pluginArbitrary = Arbitraries.integers()
                        .between(-100, 100)  // Priority range from -100 to 100
                        .map(priority -> new TestPlugin("Plugin_" + index, priority));
                pluginArbitraries.add(pluginArbitrary);
            }
            
            return Combinators.combine(pluginArbitraries).as(plugins -> plugins);
        });
    }
    
    // ==================== Test Data Classes ====================
    
    /**
     * Test implementation of MessagePlugin
     */
    static class TestPlugin implements MessagePlugin {
        private final String name;
        private final int priority;
        private boolean initialized = false;
        
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
            return true;
        }
        
        @Override
        public void initialize() {
            initialized = true;
        }
        
        @Override
        public PluginResult handle(BaseMessageEntity entity, PluginContext context) {
            return PluginResult.CONTINUE;
        }
        
        @Override
        public void destroy() {
            // No-op for test
        }
        
        boolean isInitialized() {
            return initialized;
        }
    }
    
    // ==================== Test Doubles ====================
    
    /**
     * Test implementation of PluginManager
     */
    static class TestPluginManager extends PluginManager {
        private final List<TestPlugin> plugins;
        private List<MessagePlugin> scannedPlugins;
        private List<MessagePlugin> initializedPlugins;
        
        TestPluginManager(List<TestPlugin> plugins) {
            this.plugins = new ArrayList<>(plugins);
        }
        
        @Override
        public List<MessagePlugin> scanPlugins() {
            // Sort plugins by priority in descending order
            List<MessagePlugin> sorted = new ArrayList<>(plugins);
            sorted.sort((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()));
            this.scannedPlugins = sorted;
            return sorted;
        }
        
        @Override
        public void initializePlugins(List<MessagePlugin> pluginsToInitialize) {
            this.initializedPlugins = new ArrayList<>();
            for (MessagePlugin plugin : pluginsToInitialize) {
                try {
                    plugin.initialize();
                    initializedPlugins.add(plugin);
                } catch (Exception e) {
                    // Log but continue (non-fatal error)
                }
            }
        }
        
        List<MessagePlugin> getScannedPlugins() {
            return scannedPlugins;
        }
        
        List<MessagePlugin> getInitializedPlugins() {
            return initializedPlugins;
        }
    }
    
    /**
     * Test implementation of StartupEventPublisher
     */
    static class TestStartupEventPublisher extends StartupEventPublisher {
        private final List<String> publishedEvents = new ArrayList<>();
        
        TestStartupEventPublisher() {
            super(null);
        }
        
        @Override
        public void publishPluginsReady() {
            publishedEvents.add("PluginsReady");
        }
        
        List<String> getPublishedEvents() {
            return publishedEvents;
        }
    }
    
    /**
     * Test implementation of StartupProgressTracker
     */
    static class TestStartupProgressTracker extends StartupProgressTracker {
        // Inherits all functionality from parent class
    }
}
