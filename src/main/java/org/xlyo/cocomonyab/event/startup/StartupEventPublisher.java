package org.xlyo.cocomonyab.event.startup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 启动事件发布器
 * <p>
 * 负责发布应用启动流程中各个阶段的事件。
 * 使用 Spring 的事件机制来协调各启动阶段之间的依赖关系。
 * </p>
 * <p>
 * 启动流程顺序：
 * 配置初始化 → 数据库初始化 → 集合初始化 → 插件初始化 → 消息源初始化 → API初始化 → 应用就绪
 * </p>
 *
 * @see ConfigurationReadyEvent
 * @see DatabaseReadyEvent
 * @see CollectionsReadyEvent
 * @see PluginsReadyEvent
 * @see MessageSourcesReadyEvent
 * @see ApiReadyEvent
 * @see ApplicationReadyEvent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 发布配置就绪事件
     * <p>
     * 当配置初始化阶段完成后调用此方法，表示所有配置已加载和验证完成，数据目录已创建。
     * </p>
     */
    public void publishConfigurationReady() {
        log.debug("发布配置就绪事件");
        eventPublisher.publishEvent(new ConfigurationReadyEvent(this));
    }
    
    /**
     * 发布数据库就绪事件
     * <p>
     * 当数据库初始化阶段完成后调用此方法，表示数据库连接已建立并验证成功。
     * </p>
     */
    public void publishDatabaseReady() {
        log.debug("发布数据库就绪事件");
        eventPublisher.publishEvent(new DatabaseReadyEvent(this));
    }
    
    /**
     * 发布集合就绪事件
     * <p>
     * 当数据库集合初始化阶段完成后调用此方法，表示所有集合索引已创建，初始数据已检查。
     * </p>
     */
    public void publishCollectionsReady() {
        log.debug("发布集合就绪事件");
        eventPublisher.publishEvent(new CollectionsReadyEvent(this));
    }
    
    /**
     * 发布插件就绪事件
     * <p>
     * 当消息处理插件初始化阶段完成后调用此方法，表示所有插件已扫描、排序并初始化完成。
     * </p>
     */
    public void publishPluginsReady() {
        log.debug("发布插件就绪事件");
        eventPublisher.publishEvent(new PluginsReadyEvent(this));
    }
    
    /**
     * 发布消息源就绪事件
     * <p>
     * 当消息源初始化阶段完成后调用此方法，表示所有消息源已注册并启动完成。
     * </p>
     */
    public void publishMessageSourcesReady() {
        log.debug("发布消息源就绪事件");
        eventPublisher.publishEvent(new MessageSourcesReadyEvent(this));
    }
    
    /**
     * 发布 API 就绪事件
     * <p>
     * 当 RESTful API 初始化阶段完成后调用此方法，表示 Web 服务器已启动，API 可以接收请求。
     * </p>
     */
    public void publishApiReady() {
        log.debug("发布 API 就绪事件");
        eventPublisher.publishEvent(new ApiReadyEvent(this));
    }
    
    /**
     * 发布应用就绪事件
     * <p>
     * 当所有启动阶段完成后调用此方法，表示应用已完全启动，可以正常运行。
     * </p>
     */
    public void publishApplicationReady() {
        log.debug("发布应用就绪事件");
        eventPublisher.publishEvent(new ApplicationReadyEvent(this));
    }
}
