package org.xlyo.cocomonyab.event.startup;

/**
 * 启动状态枚举
 * <p>
 * 表示应用启动过程中的各个阶段状态。
 * </p>
 */
public enum StartupStatus {
    
    /**
     * 未启动
     */
    NOT_STARTED("未启动"),
    
    /**
     * 配置初始化中
     */
    CONFIGURATION_INIT("配置初始化中"),
    
    /**
     * 数据库初始化中
     */
    DATABASE_INIT("数据库初始化中"),
    
    /**
     * 集合初始化中
     */
    COLLECTIONS_INIT("集合初始化中"),
    
    /**
     * 插件初始化中
     */
    PLUGINS_INIT("插件初始化中"),
    
    /**
     * 消息源初始化中
     */
    MESSAGE_SOURCES_INIT("消息源初始化中"),
    
    /**
     * API初始化中
     */
    API_INIT("API初始化中"),
    
    /**
     * 就绪
     */
    READY("就绪"),
    
    /**
     * 启动失败
     */
    FAILED("启动失败");
    
    private final String description;
    
    /**
     * 构造启动状态
     *
     * @param description 状态描述
     */
    StartupStatus(String description) {
        this.description = description;
    }
    
    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }
}
