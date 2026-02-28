package org.xlyo.cocomonyab.event.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 依赖关系验证器
 * <p>
 * 负责验证应用启动过程中各组件之间的依赖关系，确保组件只在其依赖组件就绪后才能访问。
 * 提供依赖关系检查、循环依赖检测和依赖未就绪时的异常抛出功能。
 * </p>
 * <p>
 * 功能：
 * <ul>
 *   <li>跟踪各组件的就绪状态</li>
 *   <li>验证组件依赖关系的完整性</li>
 *   <li>检测循环依赖</li>
 *   <li>在依赖未就绪时抛出 IllegalStateException</li>
 * </ul>
 * </p>
 *
 * @see StartupStatus
 */
@Component
@Slf4j
public class DependencyValidator {
    
    /**
     * 存储各组件的就绪状态
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, Boolean> componentReadyStatus = new ConcurrentHashMap<>();
    
    /**
     * 存储组件之间的依赖关系
     * Key: 组件名称
     * Value: 该组件依赖的组件列表
     */
    private final Map<String, List<String>> componentDependencies = new ConcurrentHashMap<>();
    
    /**
     * 标记组件为就绪状态
     * <p>
     * 当组件初始化完成后，调用此方法标记该组件已就绪。
     * </p>
     *
     * @param componentName 组件名称
     */
    public void markComponentReady(String componentName) {
        componentReadyStatus.put(componentName, true);
        log.debug("组件已就绪: {}", componentName);
    }
    
    /**
     * 检查组件是否就绪
     * <p>
     * 查询指定组件的就绪状态。
     * </p>
     *
     * @param componentName 组件名称
     * @return 如果组件已就绪返回 true，否则返回 false
     */
    public boolean isComponentReady(String componentName) {
        return componentReadyStatus.getOrDefault(componentName, false);
    }
    
    /**
     * 注册组件依赖关系
     * <p>
     * 声明一个组件依赖于其他组件。
     * </p>
     *
     * @param componentName 组件名称
     * @param dependencies  该组件依赖的组件列表
     */
    public void registerDependency(String componentName, String... dependencies) {
        componentDependencies.put(componentName, Arrays.asList(dependencies));
        log.debug("注册组件依赖: {} -> {}", componentName, Arrays.toString(dependencies));
    }
    
    /**
     * 验证组件依赖是否就绪
     * <p>
     * 检查指定组件的所有依赖是否都已就绪。
     * 如果有任何依赖未就绪，则抛出 IllegalStateException。
     * </p>
     *
     * @param componentName 组件名称
     * @throws IllegalStateException 如果组件的依赖未就绪
     */
    public void validateDependencies(String componentName) {
        List<String> dependencies = componentDependencies.get(componentName);
        
        if (dependencies == null || dependencies.isEmpty()) {
            // 没有依赖，直接返回
            return;
        }
        
        List<String> notReadyDependencies = new ArrayList<>();
        
        for (String dependency : dependencies) {
            if (!isComponentReady(dependency)) {
                notReadyDependencies.add(dependency);
            }
        }
        
        if (!notReadyDependencies.isEmpty()) {
            String errorMessage = String.format(
                    "组件 '%s' 的依赖未就绪: %s",
                    componentName,
                    notReadyDependencies
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        log.debug("组件 '{}' 的所有依赖已就绪", componentName);
    }
    
    /**
     * 检测循环依赖
     * <p>
     * 使用深度优先搜索（DFS）检测组件依赖图中是否存在循环依赖。
     * 如果检测到循环依赖，则抛出 IllegalStateException。
     * </p>
     *
     * @throws IllegalStateException 如果检测到循环依赖
     */
    public void detectCircularDependencies() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String component : componentDependencies.keySet()) {
            if (hasCircularDependency(component, visited, recursionStack, new ArrayList<>())) {
                String errorMessage = "检测到循环依赖";
                log.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            }
        }
        
        log.debug("未检测到循环依赖");
    }
    
    /**
     * 递归检查是否存在循环依赖
     * <p>
     * 使用 DFS 算法，通过递归栈检测循环。
     * </p>
     *
     * @param component      当前检查的组件
     * @param visited        已访问的组件集合
     * @param recursionStack 当前递归栈中的组件集合
     * @param path           当前依赖路径
     * @return 如果存在循环依赖返回 true，否则返回 false
     */
    private boolean hasCircularDependency(
            String component,
            Set<String> visited,
            Set<String> recursionStack,
            List<String> path) {
        
        // 如果当前组件在递归栈中，说明存在循环
        if (recursionStack.contains(component)) {
            path.add(component);
            log.error("检测到循环依赖路径: {}", path);
            return true;
        }
        
        // 如果已经访问过且没有循环，直接返回
        if (visited.contains(component)) {
            return false;
        }
        
        // 标记为已访问，并加入递归栈
        visited.add(component);
        recursionStack.add(component);
        path.add(component);
        
        // 递归检查所有依赖
        List<String> dependencies = componentDependencies.get(component);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                if (hasCircularDependency(dependency, visited, recursionStack, new ArrayList<>(path))) {
                    return true;
                }
            }
        }
        
        // 从递归栈中移除
        recursionStack.remove(component);
        
        return false;
    }
    
    /**
     * 验证依赖关系的完整性
     * <p>
     * 检查所有注册的依赖关系是否完整，即所有被依赖的组件是否都已注册。
     * 如果发现未注册的依赖，记录警告日志。
     * </p>
     */
    public void validateDependencyIntegrity() {
        Set<String> allComponents = new HashSet<>(componentDependencies.keySet());
        Set<String> missingComponents = new HashSet<>();
        
        for (List<String> dependencies : componentDependencies.values()) {
            for (String dependency : dependencies) {
                if (!allComponents.contains(dependency) && !componentReadyStatus.containsKey(dependency)) {
                    missingComponents.add(dependency);
                }
            }
        }
        
        if (!missingComponents.isEmpty()) {
            log.warn("发现未注册的依赖组件: {}", missingComponents);
        } else {
            log.debug("依赖关系完整性验证通过");
        }
    }
    
    /**
     * 获取所有组件的就绪状态
     * <p>
     * 用于测试和监控目的。
     * </p>
     *
     * @return 组件就绪状态映射
     */
    public Map<String, Boolean> getComponentReadyStatus() {
        return new HashMap<>(componentReadyStatus);
    }
    
    /**
     * 获取所有组件的依赖关系
     * <p>
     * 用于测试和监控目的。
     * </p>
     *
     * @return 组件依赖关系映射
     */
    public Map<String, List<String>> getComponentDependencies() {
        return new HashMap<>(componentDependencies);
    }
    
    /**
     * 清除所有状态
     * <p>
     * 用于测试目的，清除所有组件的就绪状态和依赖关系。
     * </p>
     */
    public void clear() {
        componentReadyStatus.clear();
        componentDependencies.clear();
        log.debug("已清除所有依赖验证状态");
    }
}
