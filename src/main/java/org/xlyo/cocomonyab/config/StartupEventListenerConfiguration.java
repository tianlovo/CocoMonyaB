package org.xlyo.cocomonyab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * 启动事件监听器配置
 * <p>
 * 配置 Spring 事件机制，确保启动阶段事件按顺序同步执行。
 * </p>
 * <p>
 * 关键配置：
 * <ul>
 *   <li>使用同步任务执行器（SyncTaskExecutor）确保事件按顺序处理</li>
 *   <li>防止事件监听器并发执行导致的启动顺序混乱</li>
 *   <li>确保每个阶段完成后才触发下一阶段</li>
 * </ul>
 * </p>
 * <p>
 * 启动阶段顺序：
 * <ol>
 *   <li>配置初始化 → ConfigurationReadyEvent</li>
 *   <li>数据库初始化 → DatabaseReadyEvent</li>
 *   <li>集合初始化 → CollectionsReadyEvent</li>
 *   <li>插件初始化 → PluginsReadyEvent</li>
 *   <li>消息源初始化 → MessageSourcesReadyEvent</li>
 *   <li>API 初始化 → ApiReadyEvent</li>
 *   <li>应用就绪 → ApplicationReadyEvent</li>
 * </ol>
 * </p>
 *
 * @see org.xlyo.cocomonyab.event.startup.StartupEventPublisher
 * @see org.xlyo.cocomonyab.config.initializer.ConfigurationManager
 * @see org.xlyo.cocomonyab.config.initializer.DatabaseManager
 * @see org.xlyo.cocomonyab.config.initializer.CollectionInitializer
 * @see org.xlyo.cocomonyab.config.initializer.PluginInitializer
 * @see org.xlyo.cocomonyab.config.initializer.MessageSourceInitializer
 * @see org.xlyo.cocomonyab.config.initializer.ApiInitializer
 * @see org.xlyo.cocomonyab.config.initializer.ApplicationReadyListener
 */
@Configuration
public class StartupEventListenerConfiguration {
    
    /**
     * 配置应用事件多播器
     * <p>
     * 使用同步任务执行器确保启动事件按顺序同步处理。
     * 这对于启动流程至关重要，因为每个阶段都依赖于前一阶段的完成。
     * </p>
     * <p>
     * 注意：此配置仅影响应用事件的处理方式，不影响其他异步任务。
     * </p>
     *
     * @return 配置好的应用事件多播器
     */
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        
        // 使用同步任务执行器，确保事件按顺序同步处理
        // 这样可以保证启动阶段按正确的依赖顺序执行
        eventMulticaster.setTaskExecutor(new SyncTaskExecutor());
        
        return eventMulticaster;
    }
}
