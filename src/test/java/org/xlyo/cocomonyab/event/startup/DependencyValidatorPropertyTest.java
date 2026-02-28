package org.xlyo.cocomonyab.event.startup;

import net.jqwik.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Dependency Validator Property Test
 * <p>
 * Validates Property 17: Throws exception when dependencies are not ready
 * </p>
 * <p>
 * **Validates: Requirements 10.3**
 * </p>
 */
class DependencyValidatorPropertyTest {
    
    private static final Logger log = LoggerFactory.getLogger(DependencyValidatorPropertyTest.class);
    
    /**
     * Property 17: For any component accessing its dependencies before they are ready,
     * the system should throw IllegalStateException
     * <p>
     * This test verifies that when a component tries to access dependencies that are not ready,
     * the system throws an IllegalStateException with appropriate error message.
     * </p>
     * <p>
     * **Validates: Requirements 10.3**
     * </p>
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17: Throws exception when dependencies are not ready")
    void throwsExceptionWhenDependenciesNotReady(
            @ForAll("componentNames") String componentName,
            @ForAll("dependencyLists") List<String> dependencies) {
        
        Assume.that(!dependencies.isEmpty());
        Assume.that(!dependencies.contains(componentName));
        
        // Prepare: Create validator and register dependencies
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(componentName, dependencies.toArray(new String[0]));
        
        // Mark only some dependencies as ready (not all)
        int readyCount = dependencies.size() / 2; // Mark half as ready
        for (int i = 0; i < readyCount; i++) {
            validator.markComponentReady(dependencies.get(i));
        }
        
        // Execute & Verify: Should throw IllegalStateException
        assertThatThrownBy(() -> validator.validateDependencies(componentName))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(componentName)
                .hasMessageContaining("依赖未就绪");
        
        log.info("Verified: Component '{}' correctly throws exception when dependencies not ready", componentName);
    }
    
    /**
     * Property 17 Extended: When all dependencies are ready, validation should pass
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Validation passes when all dependencies ready")
    void validationPassesWhenAllDependenciesReady(
            @ForAll("componentNames") String componentName,
            @ForAll("dependencyLists") List<String> dependencies) {
        
        Assume.that(!dependencies.isEmpty());
        Assume.that(!dependencies.contains(componentName));
        
        // Prepare: Create validator and register dependencies
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(componentName, dependencies.toArray(new String[0]));
        
        // Mark all dependencies as ready
        for (String dependency : dependencies) {
            validator.markComponentReady(dependency);
        }
        
        // Execute: Validate dependencies (should not throw)
        validator.validateDependencies(componentName);
        
        // Verify: All dependencies are ready
        for (String dependency : dependencies) {
            assertThat(validator.isComponentReady(dependency)).isTrue();
        }
        
        log.info("Verified: Component '{}' validation passes when all dependencies ready", componentName);
    }
    
    /**
     * Property 17 Extended: Component with no dependencies should always pass validation
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: No dependencies always passes")
    void noDependenciesAlwaysPasses(
            @ForAll("componentNames") String componentName) {
        
        // Prepare: Create validator without registering any dependencies
        DependencyValidator validator = new DependencyValidator();
        
        // Execute: Validate dependencies (should not throw)
        validator.validateDependencies(componentName);
        
        log.info("Verified: Component '{}' with no dependencies passes validation", componentName);
    }
    
    /**
     * Property 17 Extended: Circular dependency detection
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Detects circular dependencies")
    void detectsCircularDependencies(
            @ForAll("componentNames") String component1,
            @ForAll("componentNames") String component2) {
        
        Assume.that(!component1.equals(component2));
        
        // Prepare: Create validator with circular dependency
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(component1, component2);
        validator.registerDependency(component2, component1);
        
        // Execute & Verify: Should throw IllegalStateException for circular dependency
        assertThatThrownBy(() -> validator.detectCircularDependencies())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("循环依赖");
        
        log.info("Verified: Circular dependency detected between '{}' and '{}'", component1, component2);
    }
    
    /**
     * Property 17 Extended: Self-circular dependency detection
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Detects self-circular dependency")
    void detectsSelfCircularDependency(
            @ForAll("componentNames") String componentName) {
        
        // Prepare: Create validator with self-circular dependency
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(componentName, componentName);
        
        // Execute & Verify: Should throw IllegalStateException for circular dependency
        assertThatThrownBy(() -> validator.detectCircularDependencies())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("循环依赖");
        
        log.info("Verified: Self-circular dependency detected for '{}'", componentName);
    }
    
    /**
     * Property 17 Extended: No circular dependencies in valid dependency chain
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: No circular dependencies in valid chain")
    void noCircularDependenciesInValidChain(
            @ForAll("componentNames") String component1,
            @ForAll("componentNames") String component2,
            @ForAll("componentNames") String component3) {
        
        Assume.that(!component1.equals(component2));
        Assume.that(!component2.equals(component3));
        Assume.that(!component1.equals(component3));
        
        // Prepare: Create validator with valid dependency chain (no cycles)
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(component1, component2);
        validator.registerDependency(component2, component3);
        // component3 has no dependencies
        
        // Execute: Should not throw (no circular dependencies)
        validator.detectCircularDependencies();
        
        log.info("Verified: No circular dependencies in chain: {} -> {} -> {}", component1, component2, component3);
    }
    
    /**
     * Property 17 Extended: Component ready status tracking
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Component ready status tracking")
    void componentReadyStatusTracking(
            @ForAll("componentNames") String componentName) {
        
        // Prepare: Create validator
        DependencyValidator validator = new DependencyValidator();
        
        // Verify: Initially not ready
        assertThat(validator.isComponentReady(componentName)).isFalse();
        
        // Execute: Mark as ready
        validator.markComponentReady(componentName);
        
        // Verify: Now ready
        assertThat(validator.isComponentReady(componentName)).isTrue();
        
        log.info("Verified: Component '{}' ready status tracked correctly", componentName);
    }
    
    /**
     * Property 17 Extended: Multiple components ready status
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Multiple components ready status")
    void multipleComponentsReadyStatus(
            @ForAll("componentLists") List<String> components) {
        
        Assume.that(!components.isEmpty());
        
        // Prepare: Create validator
        DependencyValidator validator = new DependencyValidator();
        
        // Verify: All initially not ready
        for (String component : components) {
            assertThat(validator.isComponentReady(component)).isFalse();
        }
        
        // Execute: Mark all as ready
        for (String component : components) {
            validator.markComponentReady(component);
        }
        
        // Verify: All now ready
        for (String component : components) {
            assertThat(validator.isComponentReady(component)).isTrue();
        }
        
        log.info("Verified: {} components ready status tracked correctly", components.size());
    }
    
    /**
     * Property 17 Extended: Dependency integrity validation
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Dependency integrity validation")
    void dependencyIntegrityValidation(
            @ForAll("componentNames") String componentName,
            @ForAll("dependencyLists") List<String> dependencies) {
        
        Assume.that(!dependencies.isEmpty());
        Assume.that(!dependencies.contains(componentName));
        
        // Prepare: Create validator and register dependencies
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(componentName, dependencies.toArray(new String[0]));
        
        // Execute: Validate integrity (should not throw, may log warnings)
        validator.validateDependencyIntegrity();
        
        // Verify: Dependencies are registered
        assertThat(validator.getComponentDependencies()).containsKey(componentName);
        assertThat(validator.getComponentDependencies().get(componentName))
                .containsExactlyInAnyOrderElementsOf(dependencies);
        
        log.info("Verified: Dependency integrity validated for '{}'", componentName);
    }
    
    /**
     * Property 17 Extended: Clear state functionality
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Clear state functionality")
    void clearStateFunctionality(
            @ForAll("componentNames") String componentName,
            @ForAll("dependencyLists") List<String> dependencies) {
        
        Assume.that(!dependencies.isEmpty());
        
        // Prepare: Create validator with data
        DependencyValidator validator = new DependencyValidator();
        validator.markComponentReady(componentName);
        validator.registerDependency(componentName, dependencies.toArray(new String[0]));
        
        // Verify: Data exists
        assertThat(validator.isComponentReady(componentName)).isTrue();
        assertThat(validator.getComponentDependencies()).containsKey(componentName);
        
        // Execute: Clear state
        validator.clear();
        
        // Verify: All data cleared
        assertThat(validator.isComponentReady(componentName)).isFalse();
        assertThat(validator.getComponentDependencies()).isEmpty();
        assertThat(validator.getComponentReadyStatus()).isEmpty();
        
        log.info("Verified: State cleared successfully");
    }
    
    /**
     * Property 17 Extended: Exception message contains unready dependencies
     */
    @Property
    @Label("Feature: application-startup-flow-refactor, Property 17 Extended: Exception message contains unready dependencies")
    void exceptionMessageContainsUnreadyDependencies(
            @ForAll("componentNames") String componentName,
            @ForAll("dependencyLists") List<String> dependencies) {
        
        Assume.that(dependencies.size() >= 2);
        Assume.that(!dependencies.contains(componentName));
        
        // Prepare: Create validator and register dependencies
        DependencyValidator validator = new DependencyValidator();
        validator.registerDependency(componentName, dependencies.toArray(new String[0]));
        
        // Mark only first dependency as ready
        validator.markComponentReady(dependencies.get(0));
        
        // Get list of unready dependencies
        List<String> unreadyDeps = new ArrayList<>(dependencies);
        unreadyDeps.remove(0);
        
        // Execute & Verify: Exception message should contain unready dependencies
        assertThatThrownBy(() -> validator.validateDependencies(componentName))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(componentName);
        
        // Verify at least one unready dependency is mentioned
        try {
            validator.validateDependencies(componentName);
        } catch (IllegalStateException e) {
            boolean containsUnreadyDep = unreadyDeps.stream()
                    .anyMatch(dep -> e.getMessage().contains(dep));
            assertThat(containsUnreadyDep).isTrue();
        }
        
        log.info("Verified: Exception message contains unready dependencies for '{}'", componentName);
    }
    
    // ==================== Test Data Generators ====================
    
    /**
     * Generate component names
     */
    @Provide
    Arbitrary<String> componentNames() {
        return Arbitraries.of(
                "配置管理器",
                "数据库管理器",
                "集合初始化器",
                "插件管理器",
                "消息源管理器",
                "API服务器",
                "Telegram客户端",
                "数据目录管理器"
        );
    }
    
    /**
     * Generate dependency lists (1-3 dependencies)
     */
    @Provide
    Arbitrary<List<String>> dependencyLists() {
        return Arbitraries.of(
                "配置管理器",
                "数据库管理器",
                "集合初始化器",
                "插件管理器",
                "消息源管理器",
                "API服务器",
                "Telegram客户端",
                "数据目录管理器"
        ).list().ofMinSize(1).ofMaxSize(3).uniqueElements();
    }
    
    /**
     * Generate component lists (2-5 components)
     */
    @Provide
    Arbitrary<List<String>> componentLists() {
        return Arbitraries.of(
                "配置管理器",
                "数据库管理器",
                "集合初始化器",
                "插件管理器",
                "消息源管理器",
                "API服务器"
        ).list().ofMinSize(2).ofMaxSize(5).uniqueElements();
    }
}
