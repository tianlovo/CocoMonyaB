package org.xlyo.cocomonyab.event.startup;

/**
 * 配置就绪事件
 * <p>
 * 当配置初始化阶段完成后发布此事件，表示所有配置已加载和验证完成，数据目录已创建。
 * </p>
 */
public class ConfigurationReadyEvent extends StartupEvent {
    
    /**
     * 构造配置就绪事件
     *
     * @param source 事件源对象
     */
    public ConfigurationReadyEvent(Object source) {
        super(source);
    }
}
